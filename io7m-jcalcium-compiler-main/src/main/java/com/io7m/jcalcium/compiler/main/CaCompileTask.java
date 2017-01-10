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
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaJoint;
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
import com.io7m.jcalcium.core.definitions.CaDefinitionJoint;
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

import static com.io7m.jcalcium.compiler.api.CaCompileErrorCode.ERROR_JOINT_NONEXISTENT_PARENT;
import static com.io7m.jcalcium.compiler.api.CaCompileErrorCode.ERROR_JOINT_NO_ROOT;
import static com.io7m.jcalcium.compiler.api.CaCompileErrorCode.ERROR_MULTIPLE_ROOT_JOINTS;
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

  private static Validation<List<CaCompileError>, JOTreeNodeType<CaDefinitionJoint>>
  buildDefinitionTreeAddNode(
    final Map<CaJointName, CaDefinitionJoint> joints,
    final HashMap<CaJointName, JOTreeNodeType<CaDefinitionJoint>> nodes,
    final CaDefinitionJoint joint)
  {
    if (nodes.containsKey(joint.name())) {
      return valid(nodes.get(joint.name()));
    }

    final JOTreeNodeType<CaDefinitionJoint> node = JOTreeNode.create(joint);
    nodes.put(joint.name(), node);

    final Optional<CaJointName> parent_opt = joint.parent();
    if (parent_opt.isPresent()) {
      final CaJointName parent_name = parent_opt.get();
      if (!joints.containsKey(parent_name)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Nonexistent parent joint specified.");
        sb.append(System.lineSeparator());
        sb.append("  Joint:               ");
        sb.append(joint.name().value());
        sb.append(System.lineSeparator());
        sb.append("  Nonexistent parent: ");
        sb.append(parent_name.value());
        sb.append(System.lineSeparator());
        return invalid(errorsFor(
          ERROR_JOINT_NONEXISTENT_PARENT,
          sb.toString()));
      }

      try {
        if (nodes.containsKey(parent_name)) {
          final JOTreeNodeType<CaDefinitionJoint> parent_node = nodes.get(
            parent_name);
          parent_node.childAdd(node);
        } else {
          return buildDefinitionTreeAddNode(
            joints,
            nodes,
            joints.get(parent_name).get()).flatMap(parent -> {
            parent.childAdd(node);
            return valid(node);
          });
        }
      } catch (final JOTreeExceptionCycle e) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Graph cycle detected in skeleton input.");
        sb.append(System.lineSeparator());
        sb.append("  Joint:   ");
        sb.append(joint.name().value());
        sb.append(System.lineSeparator());
        sb.append("  Parent: ");
        sb.append(parent_name.value());
        sb.append(System.lineSeparator());
        return invalid(
          errorsFor(CaCompileErrorCode.ERROR_JOINT_CYCLE, sb.toString()));
      }
    }

    return valid(node);
  }

  /**
   * Attempt to construct a tree from the given set of joint definitions.
   *
   * @param joint_defs The joint definitions
   * @param root       The root joint
   *
   * @return A tree, or a list of reasons why the joints do not form a tree
   */

  private static Validation<
    List<CaCompileError>, JOTreeNodeType<CaDefinitionJoint>>
  compileBuildDefinitionTree(
    final Map<CaJointName, CaDefinitionJoint> joint_defs,
    final CaDefinitionJoint root)
  {
    final JOTreeNodeType<CaDefinitionJoint> tree_root =
      JOTreeNode.create(root);
    final HashMap<CaJointName, JOTreeNodeType<CaDefinitionJoint>> nodes =
      new HashMap<>(joint_defs.size());
    nodes.put(root.name(), tree_root);

    return Validation.sequence(
      joint_defs.values().map(
        joint -> buildDefinitionTreeAddNode(joint_defs, nodes, joint)))
      .flatMap(joints -> {
        Invariants.checkInvariant(
          joints.size() == joint_defs.size(),
          "Compiled skeleton joint count must match");
        return valid(tree_root);
      });
  }

  /**
   * Attempt to find a root joint in the given set of joints.
   *
   * @param joints The joints
   *
   * @return The root joint, or a list of reasons why there are too few or too
   * many root joints
   */

  private static Validation<List<CaCompileError>, CaDefinitionJoint>
  compileFindRootJoint(
    final Map<CaJointName, CaDefinitionJoint> joints)
  {
    final Map<CaJointName, CaDefinitionJoint> roots =
      joints.filter(p -> !p._2.parent().isPresent());

    if (roots.isEmpty()) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("No root joint defined in skeleton.");
      sb.append(System.lineSeparator());
      sb.append("  Possible solution: Add a joint without a parent.");
      sb.append(System.lineSeparator());
      return invalid(errorsFor(ERROR_JOINT_NO_ROOT, sb.toString()));
    }

    if (roots.size() > 1) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Multiple root joints defined in skeleton.");
      sb.append(System.lineSeparator());
      sb.append("  Roots:");
      sb.append(System.lineSeparator());
      for (final CaJointName root : roots.keySet()) {
        sb.append("    ");
        sb.append(root.value());
        sb.append(System.lineSeparator());
      }

      sb.append(
        "  Possible solution: Make sure only one joint exists without a parent.");
      sb.append(System.lineSeparator());
      return invalid(errorsFor(ERROR_MULTIPLE_ROOT_JOINTS, sb.toString()));
    }

    return valid(roots.values().apply(Integer.valueOf(0)));
  }

  private static Validation<List<CaCompileError>, JOTreeNodeType<CaJoint>>
  compileJointsAssignIdentifiers(
    final JOTreeNodeReadableType<CaDefinitionJoint> root)
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
     * Now, compile each joint and build a new joint tree.
     */

    final java.util.Map<CaJointName, JOTreeNodeType<CaJoint>> processed =
      new HashMap<>(nodes.size());
    final AtomicInteger id_pool = new AtomicInteger(0);
    JOTreeNodeType<CaJoint> compiled_root = null;

    for (final NodeByDepth node : nodes) {
      final CaDefinitionJoint joint = node.node.value();
      final CaJoint compiled = CaJoint.of(
        joint.name(),
        id_pool.getAndIncrement(),
        joint.translation(),
        joint.orientation(),
        joint.scale());

      final JOTreeNodeType<CaJoint> compiled_node =
        JOTreeNode.create(compiled);

      final Optional<CaJointName> parent_opt = joint.parent();
      if (parent_opt.isPresent()) {
        final CaJointName parent_name = parent_opt.get();

        Invariants.checkInvariant(
          processed.containsKey(parent_name),
          "Parent node must have been processed");

        final JOTreeNodeType<CaJoint> parent =
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

      processed.put(joint.name(), compiled_node);
    }

    Invariants.checkInvariant(
      processed.size() == nodes.size(),
      "Processed all nodes");
    return valid(NullCheck.notNull(compiled_root, "Root"));
  }

  private static Validation<List<CaCompileError>, JointIndex>
  compileJointsCreateIndex(
    final JOTreeNodeType<CaJoint> root)
  {
    final java.util.Map<CaJointName, JOTreeNodeType<CaJoint>> by_name =
      new HashMap<>(16);
    final java.util.Map<Integer, JOTreeNodeType<CaJoint>> by_id =
      new HashMap<>(16);

    root.forEachDepthFirst(Unit.unit(), (ignored, depth, node) -> {
      final CaJoint joint = node.value();
      final Integer joint_id = Integer.valueOf(joint.id());
      final CaJointName joint_name = joint.name();

      Invariants.checkInvariant(
        !by_name.containsKey(joint_name),
        "Name must not be duplicated");
      Invariants.checkInvariant(
        !by_id.containsKey(joint_id),
        "ID must not be duplicated");

      by_name.put(joint_name, (JOTreeNodeType<CaJoint>) node);
      by_id.put(joint_id, (JOTreeNodeType<CaJoint>) node);
    });

    return valid(new JointIndex(
      root, TreeMap.ofAll(by_name), TreeMap.ofAll(by_id)));
  }

  /*
   * Make a table of all of the nodes by ID, and another by name.
   */

  private static Validation<List<CaCompileError>, SortedMap<CaActionName, CaActionType>>
  compileActions(
    final JointIndex joint_index,
    final Map<CaActionName, CaDefinitionActionType> in_actions)
  {
    return Validation.sequence(
      in_actions.values().map(action -> compileAction(joint_index, action)))
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
    final JointIndex joint_index,
    final CaDefinitionActionType action)
  {
    return action.matchAction(joint_index, CaCompileTask::compileActionCurves);
  }

  private static Validation<List<CaCompileError>, CaActionType>
  compileActionCurves(
    final JointIndex joint_index,
    final CaDefinitionActionCurvesType action)
  {
    final Map<CaJointName, List<CaDefinitionCurveType>> curves =
      action.curves();

    List<CaCompileError> errors = List.empty();
    SortedMap<CaJointName, IndexedSeq<CaCurveType>> results =
      TreeMap.empty();

    for (final CaJointName joint : curves.keySet()) {
      final Validation<List<CaCompileError>, CaJoint> v_joint =
        compileActionJointName(joint_index, action.name(), joint);

      if (v_joint.isInvalid()) {
        errors = errors.appendAll(v_joint.getError());
        continue;
      }

      final List<CaDefinitionCurveType> curves_for_joint =
        curves.get(joint).get();

      IndexedSeq<CaCurveType> curves_ok = Array.empty();

      final CurveTypeCounter counter = new CurveTypeCounter(
        joint,
        action.name());
      for (final CaDefinitionCurveType curve : curves_for_joint) {
        final Validation<List<CaCompileError>, CaCurveType> r = curve.matchCurve(
          counter,
          CurveTypeCounter::onCurveTranslation,
          CurveTypeCounter::onCurveOrientation,
          CurveTypeCounter::onCurveScale).flatMap(
          ignored -> compileActionCurve(
            joint_index,
            action.name(),
            joint,
            curve));

        if (r.isValid()) {
          curves_ok = curves_ok.append(r.get());
        } else {
          errors = errors.appendAll(r.getError());
        }
      }

      results = results.put(joint, curves_ok);
    }

    final Validation<List<CaCompileError>, Integer> v_fps =
      compileActionFPS(action.name(), action.framesPerSecond());

    if (errors.isEmpty()) {
      final SortedMap<CaJointName, IndexedSeq<CaCurveType>> r_results = results;
      return v_fps.flatMap(fps -> {
        final CaActionCurves.Builder b = CaActionCurves.builder();
        b.setName(action.name());
        b.setFramesPerSecond(fps.intValue());
        b.setCurves(r_results);
        return valid(b.build());
      });
    }

    if (v_fps.isInvalid()) {
      return invalid(errors.appendAll(v_fps.getError()));
    }
    return invalid(errors);
  }

  private static Validation<List<CaCompileError>, CaCurveType>
  compileActionCurve(
    final JointIndex joint_index,
    final CaActionName action_name,
    final CaJointName joint_name,
    final CaDefinitionCurveType curve)
  {
    Invariants.checkInvariant(
      Objects.equals(curve.joint(), joint_name),
      () -> String.format(
        "Curve joint %s must match joint %s",
        curve.joint().value(),
        joint_name.value()));

    return compileActionJointName(joint_index, action_name, joint_name)
      .flatMap(ignored -> curve.matchCurve(
        joint_index,
        (in_joint_index, curve_translation) ->
          compileActionCurveTranslation(
            in_joint_index, action_name, joint_name, curve_translation),
        (in_joint_index, curve_orientation) ->
          compileActionCurveOrientation(
            in_joint_index, action_name, joint_name, curve_orientation),
        (in_joint_index, curve_scale) ->
          compileActionCurveScale(
            in_joint_index, action_name, joint_name, curve_scale)));
  }

  private static <E, T> Validation<List<E>, T> flatten(
    final Validation<List<List<E>>, T> v)
  {
    return v.mapError(xs -> xs.fold(List.empty(), List::appendAll));
  }

  private static Validation<List<CaCompileError>, CaCurveKeyframeTranslation>
  compileActionCurveKeyframeTranslation(
    final java.util.Map<Integer, CaCurveKeyframeTranslation> frames,
    final CaActionName action_name,
    final CaJointName joint_name,
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
      sb.append("  Joint:   ");
      sb.append(joint_name.value());
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
    final CaJointName joint_name,
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
      sb.append("  Joint:   ");
      sb.append(joint_name.value());
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
    final CaJointName joint_name,
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
      sb.append("  Joint:   ");
      sb.append(joint_name.value());
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

  private static Validation<List<CaCompileError>, CaJoint> compileActionJointName(
    final JointIndex index,
    final CaActionName action_name,
    final CaJointName joint_name)
  {
    if (index.joints_by_name.containsKey(joint_name)) {
      return valid(index.joints_by_name.get(joint_name).get().value());
    }

    final StringBuilder sb = new StringBuilder(128);
    sb.append("Action specifies a nonexistent joint.");
    sb.append(System.lineSeparator());
    sb.append("  Action: ");
    sb.append(action_name.value());
    sb.append(System.lineSeparator());
    sb.append("  Joint:   ");
    sb.append(joint_name.value());
    sb.append(System.lineSeparator());
    return invalid(errorsFor(
      CaCompileErrorCode.ERROR_ACTION_INVALID_BONE, sb.toString()));
  }

  private static Validation<List<CaCompileError>, CaCurveType>
  compileActionCurveTranslation(
    final JointIndex joint_index,
    final CaActionName action_name,
    final CaJointName joint_name,
    final CaDefinitionCurveTranslationType curve_translation)
  {
    Preconditions.checkPrecondition(
      joint_index.joints_by_name.containsKey(joint_name),
      "Joint must exist");

    final java.util.Map<Integer, CaCurveKeyframeTranslation> frames =
      new java.util.TreeMap<>();

    final Validation<List<CaCompileError>, Seq<CaCurveKeyframeTranslation>> v_frames =
      Validation.sequence(curve_translation.keyframes().map(
        keyframe -> compileActionCurveKeyframeTranslation(
          frames, action_name, joint_name, keyframe)));

    final Validation<List<CaCompileError>, CaJoint> v_name =
      compileActionJointName(joint_index, action_name, joint_name);

    return flatten(
      Validation.combine(v_frames, v_name)
        .ap((c_frames, c_joint) -> {
          final CaCurveTranslation.Builder cb = CaCurveTranslation.builder();
          cb.setKeyframes(TreeMap.ofAll(frames));
          cb.setAction(action_name);
          cb.setJoint(c_joint.name());
          return cb.build();
        }));
  }

  private static Validation<List<CaCompileError>, CaCurveType>
  compileActionCurveScale(
    final JointIndex joint_index,
    final CaActionName action_name,
    final CaJointName joint_name,
    final CaDefinitionCurveScaleType curve_scale)
  {
    Preconditions.checkPrecondition(
      joint_index.joints_by_name.containsKey(joint_name),
      "Joint must exist");

    final java.util.Map<Integer, CaCurveKeyframeScale> frames =
      new java.util.TreeMap<>();

    final Validation<List<CaCompileError>, Seq<CaCurveKeyframeScale>> v_frames =
      Validation.sequence(curve_scale.keyframes().map(
        keyframe -> compileActionCurveKeyframeScale(
          frames, action_name, joint_name, keyframe)));

    final Validation<List<CaCompileError>, CaJoint> v_name =
      compileActionJointName(joint_index, action_name, joint_name);

    return flatten(
      Validation.combine(v_frames, v_name)
        .ap((c_frames, c_joint) -> {
          final CaCurveScale.Builder cb = CaCurveScale.builder();
          cb.setKeyframes(TreeMap.ofAll(frames));
          cb.setAction(action_name);
          cb.setJoint(c_joint.name());
          return cb.build();
        }));
  }

  private static Validation<List<CaCompileError>, CaCurveType>
  compileActionCurveOrientation(
    final JointIndex joint_index,
    final CaActionName action_name,
    final CaJointName joint_name,
    final CaDefinitionCurveOrientationType curve_orientation)
  {
    Preconditions.checkPrecondition(
      joint_index.joints_by_name.containsKey(joint_name),
      "Joint must exist");

    final java.util.Map<Integer, CaCurveKeyframeOrientation> frames =
      new java.util.TreeMap<>();

    final Validation<List<CaCompileError>, Seq<CaCurveKeyframeOrientation>> v_frames =
      Validation.sequence(curve_orientation.keyframes().map(
        keyframe -> compileActionCurveKeyframeOrientation(
          frames, action_name, joint_name, keyframe)));

    final Validation<List<CaCompileError>, CaJoint> v_name =
      compileActionJointName(joint_index, action_name, joint_name);

    return flatten(
      Validation.combine(v_frames, v_name)
        .ap((c_frames, c_joint) -> {
          final CaCurveOrientation.Builder cb = CaCurveOrientation.builder();
          cb.setKeyframes(TreeMap.ofAll(frames));
          cb.setAction(action_name);
          cb.setJoint(c_joint.name());
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
    final CaSkeletonName name,
    final JointIndex index,
    final SortedMap<CaActionName, CaActionType> actions)
  {
    final CaSkeleton.Builder b = CaSkeleton.builder();
    b.setActionsByName(actions);
    b.setJoints(index.joints);
    b.setName(name);
    return b.build();
  }

  Validation<List<CaCompileError>, CaSkeleton> run()
  {
    final Map<CaJointName, CaDefinitionJoint> in_joints = this.input.joints();
    final Map<CaActionName, CaDefinitionActionType> in_actions = this.input.actions();

    return compileFindRootJoint(in_joints)
      .flatMap(root -> compileBuildDefinitionTree(in_joints, root))
      .flatMap(CaCompileTask::compileJointsAssignIdentifiers)
      .flatMap(CaCompileTask::compileJointsCreateIndex)
      .flatMap(index -> compileActions(index, in_actions).flatMap(
        actions -> valid(make(this.input.name(), index, actions))));
  }

  private static final class CurveTypeCounter
  {
    private final BitSet joint_type_received;
    private final CaJointName joint_name;
    private final CaActionName action_name;

    CurveTypeCounter(
      final CaJointName in_joint_name,
      final CaActionName in_action_name)
    {
      this.joint_type_received = new BitSet(3);
      this.joint_name = NullCheck.notNull(in_joint_name, "Joint name");
      this.action_name = NullCheck.notNull(in_action_name, "Action name");
    }

    public Validation<List<CaCompileError>, Unit> onCurveTranslation(
      final CaDefinitionCurveTranslationType translation)
    {
      if (this.joint_type_received.get(0)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Multiple curves of the same type for an action.");
        sb.append(System.lineSeparator());
        sb.append("  Action: ");
        sb.append(this.action_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Joint:   ");
        sb.append(this.joint_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Type:   translation");
        sb.append(System.lineSeparator());
        return invalid(errorsFor(
          CaCompileErrorCode.ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,
          sb.toString()));
      }
      this.joint_type_received.set(0, true);
      return valid(Unit.unit());
    }

    public Validation<List<CaCompileError>, Unit> onCurveOrientation(
      final CaDefinitionCurveOrientationType orientation)
    {
      if (this.joint_type_received.get(1)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Multiple curves of the same type for an action.");
        sb.append(System.lineSeparator());
        sb.append("  Action: ");
        sb.append(this.action_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Joint:   ");
        sb.append(this.joint_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Type:   orientation");
        sb.append(System.lineSeparator());
        return invalid(errorsFor(
          CaCompileErrorCode.ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,
          sb.toString()));
      }
      this.joint_type_received.set(1, true);
      return valid(Unit.unit());
    }

    public Validation<List<CaCompileError>, Unit> onCurveScale(
      final CaDefinitionCurveScaleType orientation)
    {
      if (this.joint_type_received.get(2)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Multiple curves of the same type for an action.");
        sb.append(System.lineSeparator());
        sb.append("  Action: ");
        sb.append(this.action_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Joint:   ");
        sb.append(this.joint_name.value());
        sb.append(System.lineSeparator());
        sb.append("  Type:   scale");
        sb.append(System.lineSeparator());
        return invalid(errorsFor(
          CaCompileErrorCode.ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,
          sb.toString()));
      }
      this.joint_type_received.set(2, true);
      return valid(Unit.unit());
    }
  }

  private static final class NodeByDepth
  {
    private final JOTreeNodeReadableType<CaDefinitionJoint> node;
    private final int depth;

    NodeByDepth(
      final JOTreeNodeReadableType<CaDefinitionJoint> in_node,
      final int in_depth)
    {
      this.node = NullCheck.notNull(in_node, "Node");
      this.depth = in_depth;
    }
  }

  private static final class JointIndex
  {
    private final JOTreeNodeType<CaJoint> joints;
    private final SortedMap<CaJointName, JOTreeNodeType<CaJoint>> joints_by_name;
    private final SortedMap<Integer, JOTreeNodeType<CaJoint>> joints_by_id;

    JointIndex(
      final JOTreeNodeType<CaJoint> in_joints,
      final SortedMap<CaJointName, JOTreeNodeType<CaJoint>> in_joints_by_name,
      final SortedMap<Integer, JOTreeNodeType<CaJoint>> in_joints_by_id)
    {
      this.joints =
        NullCheck.notNull(in_joints, "Joints");
      this.joints_by_name =
        NullCheck.notNull(in_joints_by_name, "Joints by name");
      this.joints_by_id =
        NullCheck.notNull(in_joints_by_id, "Joints by id");
    }
  }

}
