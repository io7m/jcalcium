/*
 * Copyright © 2016 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.jcalcium.compiler.main;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.compiler.api.CaCompileError;
import com.io7m.jcalcium.compiler.api.CaCompileErrorCode;
import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.compiled.CaBone;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurves;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeScale;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveScale;
import com.io7m.jcalcium.core.compiled.actions.CaCurveTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveType;
import com.io7m.jcalcium.core.definitions.CaDefinitionBone;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionCurvesType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeOrientationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeScaleType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeTranslationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveOrientationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveScaleType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveTranslationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeExceptionCycle;
import com.io7m.jorchard.core.JOTreeNode;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import javaslang.collection.Array;
import javaslang.collection.IndexedSeq;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Seq;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
import javaslang.control.Validation;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.io7m.jcalcium.compiler.api.CaCompileErrorCode.ERROR_BONE_MULTIPLE_ROOTS;
import static com.io7m.jcalcium.compiler.api.CaCompileErrorCode.ERROR_BONE_NONEXISTENT_PARENT;
import static com.io7m.jcalcium.compiler.api.CaCompileErrorCode.ERROR_BONE_NO_ROOT;
import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.valid;

/**
 * A single compilation task.
 */

final class CaCompileTask
{
  private final CaDefinitionSkeleton input;

  CaCompileTask(final CaDefinitionSkeleton in_definition)
  {
    this.input = NullCheck.notNull(in_definition, "Definition");
  }

  private static List<CaCompileError> errorsFor(
    final CaCompileErrorCode code,
    final String message)
  {
    return List.of(CaCompileError.of(code, message));
  }

  private static Validation<List<CaCompileError>, JOTreeNodeType<CaDefinitionBone>>
  buildDefinitionTreeAddNode(
    final Map<CaBoneName, CaDefinitionBone> bones,
    final HashMap<CaBoneName, JOTreeNodeType<CaDefinitionBone>> nodes,
    final CaDefinitionBone bone)
  {
    if (nodes.containsKey(bone.name())) {
      return valid(nodes.get(bone.name()));
    }

    final JOTreeNodeType<CaDefinitionBone> node = JOTreeNode.create(bone);
    nodes.put(bone.name(), node);

    final Optional<CaBoneName> parent_opt = bone.parent();
    if (parent_opt.isPresent()) {
      final CaBoneName parent_name = parent_opt.get();
      if (!bones.containsKey(parent_name)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Nonexistent parent bone specified.");
        sb.append(System.lineSeparator());
        sb.append("  Bone:               ");
        sb.append(bone.name().value());
        sb.append(System.lineSeparator());
        sb.append("  Nonexistent parent: ");
        sb.append(parent_name.value());
        sb.append(System.lineSeparator());
        return invalid(errorsFor(
          ERROR_BONE_NONEXISTENT_PARENT,
          sb.toString()));
      }

      try {
        if (nodes.containsKey(parent_name)) {
          final JOTreeNodeType<CaDefinitionBone> parent_node = nodes.get(
            parent_name);
          parent_node.childAdd(node);
        } else {
          return buildDefinitionTreeAddNode(
            bones,
            nodes,
            bones.get(parent_name).get()).flatMap(parent -> {
            parent.childAdd(node);
            return valid(node);
          });
        }
      } catch (final JOTreeExceptionCycle e) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Graph cycle detected in skeleton input.");
        sb.append(System.lineSeparator());
        sb.append("  Bone:   ");
        sb.append(bone.name().value());
        sb.append(System.lineSeparator());
        sb.append("  Parent: ");
        sb.append(parent_name.value());
        sb.append(System.lineSeparator());
        return invalid(
          errorsFor(CaCompileErrorCode.ERROR_BONE_CYCLE, sb.toString()));
      }
    }

    return valid(node);
  }

  /**
   * Attempt to construct a tree from the given set of bone definitions.
   *
   * @param bone_defs The bone definitions
   * @param root      The root bone
   *
   * @return A tree, or a list of reasons why the bones do not form a tree
   */

  private static Validation<
    List<CaCompileError>, JOTreeNodeType<CaDefinitionBone>>
  compileBuildDefinitionTree(
    final Map<CaBoneName, CaDefinitionBone> bone_defs,
    final CaDefinitionBone root)
  {
    final JOTreeNodeType<CaDefinitionBone> tree_root =
      JOTreeNode.create(root);
    final HashMap<CaBoneName, JOTreeNodeType<CaDefinitionBone>> nodes =
      new HashMap<>(bone_defs.size());
    nodes.put(root.name(), tree_root);

    return Validation.sequence(
      bone_defs.values().map(
        bone -> buildDefinitionTreeAddNode(bone_defs, nodes, bone)))
      .flatMap(bones -> {
        Invariants.checkInvariant(
          bones.size() == bone_defs.size(),
          "Compiled skeleton bone count must match");
        return valid(tree_root);
      });
  }

  /**
   * Attempt to find a root bone in the given set of bones.
   *
   * @param bones The bones
   *
   * @return The root bone, or a list of reasons why there are too few or too
   * many root bones
   */

  private static Validation<List<CaCompileError>, CaDefinitionBone>
  compileFindRootBone(
    final Map<CaBoneName, CaDefinitionBone> bones)
  {
    final Map<CaBoneName, CaDefinitionBone> roots =
      bones.filter(p -> !p._2.parent().isPresent());

    if (roots.isEmpty()) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("No root bone defined in skeleton.");
      sb.append(System.lineSeparator());
      sb.append("  Possible solution: Add a bone without a parent.");
      sb.append(System.lineSeparator());
      return invalid(errorsFor(ERROR_BONE_NO_ROOT, sb.toString()));
    }

    if (roots.size() > 1) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Multiple root bones defined in skeleton.");
      sb.append(System.lineSeparator());
      sb.append("  Roots:");
      sb.append(System.lineSeparator());
      for (final CaBoneName root : roots.keySet()) {
        sb.append("    ");
        sb.append(root.value());
        sb.append(System.lineSeparator());
      }

      sb.append(
        "  Possible solution: Make sure only one bone exists without a parent.");
      sb.append(System.lineSeparator());
      return invalid(errorsFor(ERROR_BONE_MULTIPLE_ROOTS, sb.toString()));
    }

    return valid(roots.values().apply(Integer.valueOf(0)));
  }

  private static Validation<List<CaCompileError>, JOTreeNodeType<CaBone>>
  compileBonesAssignIdentifiers(
    final JOTreeNodeReadableType<CaDefinitionBone> root)
  {
    /*
     * First, insert all nodes into a linked list, ordered first by depth
     * and then by name. The aim is to receive all nodes of a given depth
     * in the lexicographical order of their names.
     */

    final LinkedList<NodeByDepth> nodes = new LinkedList<>();
    root.forEachBreadthFirst(
      Unit.unit(),
      (ignored, depth, node) -> nodes.add(new NodeByDepth(node, depth)));

    nodes.sort((o1, o2) -> {
      final int depth_compare = Integer.compare(o1.depth, o2.depth);
      if (depth_compare == 0) {
        return o1.node.value().name().compareTo(o2.node.value().name());
      }
      return depth_compare;
    });

    /*
     * Now, compile each bone and build a new bone tree.
     */

    final java.util.Map<CaBoneName, JOTreeNodeType<CaBone>> processed =
      new HashMap<>(nodes.size());
    final AtomicInteger id_pool = new AtomicInteger(0);
    JOTreeNodeType<CaBone> compiled_root = null;

    for (final NodeByDepth node : nodes) {
      final CaDefinitionBone bone = node.node.value();
      final CaBone compiled = CaBone.of(
        bone.name(),
        id_pool.getAndIncrement(),
        bone.translation(),
        bone.orientation(),
        bone.scale());

      final JOTreeNodeType<CaBone> compiled_node =
        JOTreeNode.create(compiled);

      final Optional<CaBoneName> parent_opt = bone.parent();
      if (parent_opt.isPresent()) {
        final CaBoneName parent_name = parent_opt.get();

        Invariants.checkInvariant(
          processed.containsKey(parent_name),
          "Parent node must have been processed");

        final JOTreeNodeType<CaBone> parent =
          processed.get(parent_name);
        compiled_node.setParent(parent);
      } else {
        Invariants.checkInvariant(
          node.depth == 0,
          "Node without parent must be at depth 0");
        Invariants.checkInvariant(
          compiled_root == null,
          "Only one compiled_root can exist");
        compiled_root = compiled_node;
      }

      processed.put(bone.name(), compiled_node);
    }

    Invariants.checkInvariant(
      processed.size() == nodes.size(),
      "Processed all nodes");
    return valid(NullCheck.notNull(compiled_root, "Root"));
  }

  private static Validation<List<CaCompileError>, BoneIndex>
  compileBonesCreateIndex(
    final JOTreeNodeType<CaBone> root)
  {
    final java.util.Map<CaBoneName, JOTreeNodeType<CaBone>> by_name =
      new HashMap<>(16);
    final java.util.Map<Integer, JOTreeNodeType<CaBone>> by_id =
      new HashMap<>(16);

    root.forEachDepthFirst(Unit.unit(), (ignored, depth, node) -> {
      final CaBone bone = node.value();
      final Integer bone_id = Integer.valueOf(bone.id());
      final CaBoneName bone_name = bone.name();

      Invariants.checkInvariant(
        !by_name.containsKey(bone_name),
        "Name must not be duplicated");
      Invariants.checkInvariant(
        !by_id.containsKey(bone_id),
        "ID must not be duplicated");

      by_name.put(bone_name, (JOTreeNodeType<CaBone>) node);
      by_id.put(bone_id, (JOTreeNodeType<CaBone>) node);
    });

    return valid(new BoneIndex(
      root, TreeMap.ofAll(by_name), TreeMap.ofAll(by_id)));
  }

  /*
   * Make a table of all of the nodes by ID, and another by name.
   */

  private static Validation<List<CaCompileError>, SortedMap<CaActionName, CaActionType>>
  compileActions(
    final BoneIndex bone_index,
    final Map<CaActionName, CaDefinitionActionType> in_actions)
  {
    return Validation.sequence(
      in_actions.values().map(action -> compileAction(bone_index, action)))
      .flatMap(actions -> valid(sortedMapOf(actions)));
  }

  private static SortedMap<CaActionName, CaActionType> sortedMapOf(
    final Iterable<CaActionType> actions)
  {
    final java.util.TreeMap<CaActionName, CaActionType> jm =
      new java.util.TreeMap<>();
    actions.forEach(act -> jm.put(act.name(), act));
    return TreeMap.ofAll(jm);
  }

  private static Validation<List<CaCompileError>, CaActionType>
  compileAction(
    final BoneIndex bone_index,
    final CaDefinitionActionType action)
  {
    return action.matchAction(bone_index, CaCompileTask::compileActionCurves);
  }

  private static Validation<List<CaCompileError>, CaActionType>
  compileActionCurves(
    final BoneIndex bone_index,
    final CaDefinitionActionCurvesType action)
  {
    final Map<CaBoneName, List<CaDefinitionCurveType>> curves =
      action.curves();

    List<CaCompileError> errors = List.empty();
    SortedMap<CaBoneName, IndexedSeq<CaCurveType>> results =
      javaslang.collection.TreeMap.empty();

    for (final CaBoneName bone : curves.keySet()) {
      final Validation<List<CaCompileError>, CaBone> v_bone =
        compileActionBoneName(bone_index, action.name(), bone);

      if (v_bone.isInvalid()) {
        errors = errors.appendAll(v_bone.getError());
        continue;
      }

      final List<CaDefinitionCurveType> curves_for_bone =
        curves.get(bone).get();

      IndexedSeq<CaCurveType> curves_ok = Array.empty();

      final CurveTypeCounter counter = new CurveTypeCounter(
        bone,
        action.name());
      for (final CaDefinitionCurveType curve : curves_for_bone) {
        final Validation<List<CaCompileError>, CaCurveType> r = curve.matchCurve(
          counter,
          CurveTypeCounter::onCurveTranslation,
          CurveTypeCounter::onCurveOrientation,
          CurveTypeCounter::onCurveScale).flatMap(
          ignored -> compileActionCurve(
            bone_index,
            action.name(),
            bone,
            curve));

        if (r.isValid()) {
          curves_ok = curves_ok.append(r.get());
        } else {
          errors = errors.appendAll(r.getError());
        }
      }

      results = results.put(bone, curves_ok);
    }

    final Validation<List<CaCompileError>, Integer> v_fps =
      compileActionFPS(action.name(), action.framesPerSecond());

    if (errors.isEmpty()) {
      final SortedMap<CaBoneName, IndexedSeq<CaCurveType>> r_results = results;
      return v_fps.flatMap(fps -> {
        final CaActionCurves.Builder b = CaActionCurves.builder();
        b.setName(action.name());
        b.setFramesPerSecond(fps.intValue());
        b.setCurves(r_results);
        return valid(b.build());
      });
    }

    if (v_fps.isInvalid()) {
      return Validation.invalid(errors.appendAll(v_fps.getError()));
    }
    return Validation.invalid(errors);
  }

  private static Validation<List<CaCompileError>, CaCurveType>
  compileActionCurve(
    final BoneIndex bone_index,
    final CaActionName action_name,
    final CaBoneName bone_name,
    final CaDefinitionCurveType curve)
  {
    Invariants.checkInvariant(
      Objects.equals(curve.bone(), bone_name),
      () -> String.format(
        "Curve bone %s must match bone %s",
        curve.bone().value(),
        bone_name.value()));

    return compileActionBoneName(bone_index, action_name, bone_name)
      .flatMap(ignored -> curve.matchCurve(
        bone_index,
        (in_bone_index, curve_translation) ->
          compileActionCurveTranslation(
            in_bone_index, action_name, bone_name, curve_translation),
        (in_bone_index, curve_orientation) ->
          compileActionCurveOrientation(
            in_bone_index, action_name, bone_name, curve_orientation),
        (in_bone_index, curve_scale) ->
          compileActionCurveScale(
            in_bone_index, action_name, bone_name, curve_scale)));
  }

  private static <E, T> Validation<List<E>, T> flatten(
    final Validation<List<List<E>>, T> v)
  {
    return v.leftMap(xs -> xs.fold(List.empty(), List::appendAll));
  }

  private static Validation<List<CaCompileError>, CaCurveKeyframeTranslation>
  compileActionCurveKeyframeTranslation(
    final java.util.Map<Integer, CaCurveKeyframeTranslation> frames,
    final CaActionName action_name,
    final CaBoneName bone_name,
    final CaDefinitionCurveKeyframeTranslationType keyframe)
  {
    final Integer keyframe_index = Integer.valueOf(keyframe.index());

    if (frames.containsKey(keyframe_index)) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Keyframes have duplicate indices.");
      sb.append(System.lineSeparator());
      sb.append("  Action: ");
      sb.append(action_name.value());
      sb.append(System.lineSeparator());
      sb.append("  Bone:   ");
      sb.append(bone_name.value());
      sb.append(System.lineSeparator());
      sb.append("  Index:  ");
      sb.append(keyframe.index());
      sb.append(System.lineSeparator());
      sb.append("  Curve:  ");
      sb.append("translation");
      sb.append(System.lineSeparator());
      return invalid(errorsFor(
        CaCompileErrorCode.ERROR_ACTION_DUPLICATE_KEYFRAME, sb.toString()));
    }

    final CaCurveKeyframeTranslation.Builder result_keyframe_builder =
      CaCurveKeyframeTranslation.builder();
    result_keyframe_builder.setEasing(keyframe.easing());
    result_keyframe_builder.setInterpolation(keyframe.interpolation());
    result_keyframe_builder.setTranslation(keyframe.translation());
    result_keyframe_builder.setIndex(keyframe.index());

    final CaCurveKeyframeTranslation result_keyframe =
      result_keyframe_builder.build();
    frames.put(keyframe_index, result_keyframe);
    return valid(result_keyframe);
  }

  private static Validation<List<CaCompileError>, CaCurveKeyframeOrientation>
  compileActionCurveKeyframeOrientation(
    final java.util.Map<Integer, CaCurveKeyframeOrientation> frames,
    final CaActionName action_name,
    final CaBoneName bone_name,
    final CaDefinitionCurveKeyframeOrientationType keyframe)
  {
    final Integer keyframe_index = Integer.valueOf(keyframe.index());

    if (frames.containsKey(keyframe_index)) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Keyframes have duplicate indices.");
      sb.append(System.lineSeparator());
      sb.append("  Action: ");
      sb.append(action_name.value());
      sb.append(System.lineSeparator());
      sb.append("  Bone:   ");
      sb.append(bone_name.value());
      sb.append(System.lineSeparator());
      sb.append("  Index:  ");
      sb.append(keyframe.index());
      sb.append(System.lineSeparator());
      sb.append("  Curve:  ");
      sb.append("orientation");
      sb.append(System.lineSeparator());
      return invalid(errorsFor(
        CaCompileErrorCode.ERROR_ACTION_DUPLICATE_KEYFRAME, sb.toString()));
    }

    final CaCurveKeyframeOrientation.Builder result_keyframe_builder =
      CaCurveKeyframeOrientation.builder();
    result_keyframe_builder.setEasing(keyframe.easing());
    result_keyframe_builder.setInterpolation(keyframe.interpolation());
    result_keyframe_builder.setOrientation(keyframe.orientation());
    result_keyframe_builder.setIndex(keyframe.index());

    final CaCurveKeyframeOrientation result_keyframe =
      result_keyframe_builder.build();
    frames.put(keyframe_index, result_keyframe);
    return valid(result_keyframe);
  }

  private static Validation<List<CaCompileError>, CaCurveKeyframeScale>
  compileActionCurveKeyframeScale(
    final java.util.Map<Integer, CaCurveKeyframeScale> frames,
    final CaActionName action_name,
    final CaBoneName bone_name,
    final CaDefinitionCurveKeyframeScaleType keyframe)
  {
    final Integer keyframe_index = Integer.valueOf(keyframe.index());

    if (frames.containsKey(keyframe_index)) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Keyframes have duplicate indices.");
      sb.append(System.lineSeparator());
      sb.append("  Action: ");
      sb.append(action_name.value());
      sb.append(System.lineSeparator());
      sb.append("  Bone:   ");
      sb.append(bone_name.value());
      sb.append(System.lineSeparator());
      sb.append("  Index:  ");
      sb.append(keyframe.index());
      sb.append(System.lineSeparator());
      sb.append("  Curve:  ");
      sb.append("scale");
      sb.append(System.lineSeparator());
      return invalid(errorsFor(
        CaCompileErrorCode.ERROR_ACTION_DUPLICATE_KEYFRAME, sb.toString()));
    }

    final CaCurveKeyframeScale.Builder result_keyframe_builder =
      CaCurveKeyframeScale.builder();
    result_keyframe_builder.setEasing(keyframe.easing());
    result_keyframe_builder.setInterpolation(keyframe.interpolation());
    result_keyframe_builder.setScale(keyframe.scale());
    result_keyframe_builder.setIndex(keyframe.index());

    final CaCurveKeyframeScale result_keyframe =
      result_keyframe_builder.build();
    frames.put(keyframe_index, result_keyframe);
    return valid(result_keyframe);
  }

  private static Validation<List<CaCompileError>, CaBone> compileActionBoneName(
    final BoneIndex index,
    final CaActionName action_name,
    final CaBoneName bone_name)
  {
    if (index.bones_by_name.containsKey(bone_name)) {
      return valid(index.bones_by_name.get(bone_name).get().value());
    }

    final StringBuilder sb = new StringBuilder(128);
    sb.append("Action specifies a nonexistent bone.");
    sb.append(System.lineSeparator());
    sb.append("  Action: ");
    sb.append(action_name.value());
    sb.append(System.lineSeparator());
    sb.append("  Bone:   ");
    sb.append(bone_name.value());
    sb.append(System.lineSeparator());
    return invalid(errorsFor(
      CaCompileErrorCode.ERROR_ACTION_INVALID_BONE, sb.toString()));
  }

  private static Validation<List<CaCompileError>, CaCurveType>
  compileActionCurveTranslation(
    final BoneIndex bone_index,
    final CaActionName action_name,
    final CaBoneName bone_name,
    final CaDefinitionCurveTranslationType curve_translation)
  {
    Preconditions.checkPrecondition(
      bone_index.bones_by_name.containsKey(bone_name),
      "Bone must exist");

    final java.util.Map<Integer, CaCurveKeyframeTranslation> frames =
      new java.util.TreeMap<>();

    final Validation<List<CaCompileError>, Seq<CaCurveKeyframeTranslation>> v_frames =
      Validation.sequence(curve_translation.keyframes().map(
        keyframe -> compileActionCurveKeyframeTranslation(
          frames, action_name, bone_name, keyframe)));

    final Validation<List<CaCompileError>, CaBone> v_name =
      compileActionBoneName(bone_index, action_name, bone_name);

    return flatten(
      Validation.combine(v_frames, v_name)
        .ap((c_frames, c_bone) -> {
          final CaCurveTranslation.Builder cb = CaCurveTranslation.builder();
          cb.setKeyframes(TreeMap.ofAll(frames));
          cb.setAction(action_name);
          cb.setBone(c_bone.name());
          return cb.build();
        }));
  }

  private static Validation<List<CaCompileError>, CaCurveType>
  compileActionCurveScale(
    final BoneIndex bone_index,
    final CaActionName action_name,
    final CaBoneName bone_name,
    final CaDefinitionCurveScaleType curve_scale)
  {
    Preconditions.checkPrecondition(
      bone_index.bones_by_name.containsKey(bone_name),
      "Bone must exist");

    final java.util.Map<Integer, CaCurveKeyframeScale> frames =
      new java.util.TreeMap<>();

    final Validation<List<CaCompileError>, Seq<CaCurveKeyframeScale>> v_frames =
      Validation.sequence(curve_scale.keyframes().map(
        keyframe -> compileActionCurveKeyframeScale(
          frames, action_name, bone_name, keyframe)));

    final Validation<List<CaCompileError>, CaBone> v_name =
      compileActionBoneName(bone_index, action_name, bone_name);

    return flatten(
      Validation.combine(v_frames, v_name)
        .ap((c_frames, c_bone) -> {
          final CaCurveScale.Builder cb = CaCurveScale.builder();
          cb.setKeyframes(TreeMap.ofAll(frames));
          cb.setAction(action_name);
          cb.setBone(c_bone.name());
          return cb.build();
        }));
  }

  private static Validation<List<CaCompileError>, CaCurveType>
  compileActionCurveOrientation(
    final BoneIndex bone_index,
    final CaActionName action_name,
    final CaBoneName bone_name,
    final CaDefinitionCurveOrientationType curve_orientation)
  {
    Preconditions.checkPrecondition(
      bone_index.bones_by_name.containsKey(bone_name),
      "Bone must exist");

    final java.util.Map<Integer, CaCurveKeyframeOrientation> frames =
      new java.util.TreeMap<>();

    final Validation<List<CaCompileError>, Seq<CaCurveKeyframeOrientation>> v_frames =
      Validation.sequence(curve_orientation.keyframes().map(
        keyframe -> compileActionCurveKeyframeOrientation(
          frames, action_name, bone_name, keyframe)));

    final Validation<List<CaCompileError>, CaBone> v_name =
      compileActionBoneName(bone_index, action_name, bone_name);

    return flatten(
      Validation.combine(v_frames, v_name)
        .ap((c_frames, c_bone) -> {
          final CaCurveOrientation.Builder cb = CaCurveOrientation.builder();
          cb.setKeyframes(TreeMap.ofAll(frames));
          cb.setAction(action_name);
          cb.setBone(c_bone.name());
          return cb.build();
        }));
  }

  private static Validation<List<CaCompileError>, Integer> compileActionFPS(
    final CaActionName action,
    final int fps)
  {
    if (fps <= 0) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Invalid frames-per-second value");
      sb.append(System.lineSeparator());
      sb.append("  Action:   ");
      sb.append(action.value());
      sb.append(System.lineSeparator());
      sb.append("  Expected: A value in the range [1, ");
      sb.append(Integer.MAX_VALUE);
      sb.append("]");
      sb.append(System.lineSeparator());
      sb.append("  Received: ");
      sb.append(fps);
      sb.append(System.lineSeparator());
      return invalid(errorsFor(
        CaCompileErrorCode.ERROR_ACTION_INVALID_FPS, sb.toString()));
    }

    return valid(Integer.valueOf(fps));
  }

  private static CaSkeleton make(
    final BoneIndex index,
    final SortedMap<CaActionName, CaActionType> actions)
  {
    final CaSkeleton.Builder b = CaSkeleton.builder();
    b.setActionsByName(actions);
    b.setBonesByID(index.bones_by_id.mapValues(x -> x));
    b.setBonesByName(index.bones_by_name.mapValues(x -> x));
    b.setBones(index.bones);
    return b.build();
  }

  Validation<List<CaCompileError>, CaSkeleton> run()
  {
    final Map<CaBoneName, CaDefinitionBone> in_bones = this.input.bones();
    final Map<CaActionName, CaDefinitionActionType> in_actions = this.input.actions();

    return compileFindRootBone(in_bones)
      .flatMap(root -> compileBuildDefinitionTree(in_bones, root))
      .flatMap(CaCompileTask::compileBonesAssignIdentifiers)
      .flatMap(CaCompileTask::compileBonesCreateIndex)
      .flatMap(index -> compileActions(index, in_actions).flatMap(
        actions -> valid(make(index, actions))));
  }

  private static final class CurveTypeCounter
  {
    private final BitSet bone_type_received;
    private final CaBoneName bone_name;
    private final CaActionName action_name;

    CurveTypeCounter(
      final CaBoneName in_bone_name,
      final CaActionName in_action_name)
    {
      this.bone_type_received = new BitSet(3);
      this.bone_name = NullCheck.notNull(in_bone_name, "Bone name");
      this.action_name = NullCheck.notNull(in_action_name, "Action name");
    }

    public Validation<List<CaCompileError>, Unit> onCurveTranslation(
      final CaDefinitionCurveTranslationType translation)
    {
      if (this.bone_type_received.get(0)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Multiple curves of the same type for an action.");
        sb.append(System.lineSeparator());
        sb.append("  Action: ");
        sb.append(this.action_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Bone:   ");
        sb.append(this.bone_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Type:   translation");
        sb.append(System.lineSeparator());
        return invalid(errorsFor(
          CaCompileErrorCode.ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,
          sb.toString()));
      }
      this.bone_type_received.set(0, true);
      return valid(Unit.unit());
    }

    public Validation<List<CaCompileError>, Unit> onCurveOrientation(
      final CaDefinitionCurveOrientationType orientation)
    {
      if (this.bone_type_received.get(1)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Multiple curves of the same type for an action.");
        sb.append(System.lineSeparator());
        sb.append("  Action: ");
        sb.append(this.action_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Bone:   ");
        sb.append(this.bone_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Type:   orientation");
        sb.append(System.lineSeparator());
        return invalid(errorsFor(
          CaCompileErrorCode.ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,
          sb.toString()));
      }
      this.bone_type_received.set(1, true);
      return valid(Unit.unit());
    }

    public Validation<List<CaCompileError>, Unit> onCurveScale(
      final CaDefinitionCurveScaleType orientation)
    {
      if (this.bone_type_received.get(2)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Multiple curves of the same type for an action.");
        sb.append(System.lineSeparator());
        sb.append("  Action: ");
        sb.append(this.action_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Bone:   ");
        sb.append(this.bone_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Type:   scale");
        sb.append(System.lineSeparator());
        return invalid(errorsFor(
          CaCompileErrorCode.ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,
          sb.toString()));
      }
      this.bone_type_received.set(2, true);
      return valid(Unit.unit());
    }
  }

  private static final class NodeByDepth
  {
    private final JOTreeNodeReadableType<CaDefinitionBone> node;
    private final int depth;

    NodeByDepth(
      final JOTreeNodeReadableType<CaDefinitionBone> in_node,
      final int in_depth)
    {
      this.node = NullCheck.notNull(in_node, "Node");
      this.depth = in_depth;
    }
  }

  private static final class BoneIndex
  {
    private final JOTreeNodeType<CaBone> bones;
    private final SortedMap<CaBoneName, JOTreeNodeType<CaBone>> bones_by_name;
    private final SortedMap<Integer, JOTreeNodeType<CaBone>> bones_by_id;

    BoneIndex(
      final JOTreeNodeType<CaBone> in_bones,
      final SortedMap<CaBoneName, JOTreeNodeType<CaBone>> in_bones_by_name,
      final SortedMap<Integer, JOTreeNodeType<CaBone>> in_bones_by_id)
    {
      this.bones =
        NullCheck.notNull(in_bones, "Bones");
      this.bones_by_name =
        NullCheck.notNull(in_bones_by_name, "Bones by name");
      this.bones_by_id =
        NullCheck.notNull(in_bones_by_id, "Bones by id");
    }
  }

}
