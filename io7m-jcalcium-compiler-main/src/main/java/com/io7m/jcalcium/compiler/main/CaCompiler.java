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
import com.io7m.jcalcium.compiler.api.CaCompileError;
import com.io7m.jcalcium.compiler.api.CaCompileErrorCode;
import com.io7m.jcalcium.compiler.api.CaCompilerType;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.compiled.CaCompiledBone;
import com.io7m.jcalcium.core.compiled.CaCompiledSkeletonType;
import com.io7m.jcalcium.core.definitions.CaDefinitionBone;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeExceptionCycle;
import com.io7m.jorchard.core.JOTreeNode;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.SortedMap;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.io7m.jcalcium.compiler.api.CaCompileErrorCode.ERROR_MULTIPLE_ROOT_BONES;
import static com.io7m.jcalcium.compiler.api.CaCompileErrorCode.ERROR_NONEXISTENT_PARENT;
import static com.io7m.jcalcium.compiler.api.CaCompileErrorCode.ERROR_NO_ROOT_BONE;
import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.valid;

/**
 * Main implementation of the {@link CaCompilerType} interface.
 */

public final class CaCompiler implements CaCompilerType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaCompiler.class);
  }

  private CaCompiler()
  {

  }

  public static CaCompilerType create()
  {
    return new CaCompiler();
  }

  @Override
  public Validation<List<CaCompileError>, CaCompiledSkeletonType> compile(
    final CaDefinitionSkeleton skeleton)
  {
    return new CompileTask(skeleton).run();
  }

  private static final class CompileTask
  {
    private final CaDefinitionSkeleton input;

    CompileTask(final CaDefinitionSkeleton in_definition)
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
          return invalid(errorsFor(ERROR_NONEXISTENT_PARENT, sb.toString()));
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
            errorsFor(CaCompileErrorCode.ERROR_CYCLE, sb.toString()));
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
        return invalid(errorsFor(ERROR_NO_ROOT_BONE, sb.toString()));
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
        return invalid(errorsFor(ERROR_MULTIPLE_ROOT_BONES, sb.toString()));
      }

      return valid(roots.values().apply(Integer.valueOf(0)));
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

    private static Validation<List<CaCompileError>, JOTreeNodeType<CaCompiledBone>>
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

      final java.util.Map<CaBoneName, JOTreeNodeType<CaCompiledBone>> processed =
        new HashMap<>(nodes.size());
      final AtomicInteger id_pool = new AtomicInteger(0);
      JOTreeNodeType<CaCompiledBone> compiled_root = null;

      for (final NodeByDepth node : nodes) {
        final CaDefinitionBone bone = node.node.value();
        final CaCompiledBone compiled = CaCompiledBone.of(
          bone.name(),
          id_pool.getAndIncrement(),
          bone.translation(),
          bone.orientation(),
          bone.scale());

        final JOTreeNodeType<CaCompiledBone> compiled_node =
          JOTreeNode.create(compiled);

        final Optional<CaBoneName> parent_opt = bone.parent();
        if (parent_opt.isPresent()) {
          final CaBoneName parent_name = parent_opt.get();

          Invariants.checkInvariant(
            processed.containsKey(parent_name),
            "Parent node must have been processed");

          final JOTreeNodeType<CaCompiledBone> parent =
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

    private static final class BoneIndex
    {
      private final JOTreeNodeType<CaCompiledBone> bones;
      private final SortedMap<CaBoneName, JOTreeNodeType<CaCompiledBone>> bones_by_name;
      private final SortedMap<Integer, JOTreeNodeType<CaCompiledBone>> bones_by_id;

      BoneIndex(
        final JOTreeNodeType<CaCompiledBone> in_bones,
        final SortedMap<CaBoneName, JOTreeNodeType<CaCompiledBone>> in_bones_by_name,
        final SortedMap<Integer, JOTreeNodeType<CaCompiledBone>> in_bones_by_id)
      {
        this.bones = NullCheck.notNull(in_bones, "Bones");
        this.bones_by_name = NullCheck.notNull(
          in_bones_by_name,
          "Bones by name");
        this.bones_by_id = NullCheck.notNull(in_bones_by_id, "Bones by id");
      }
    }

    /*
     * Make a table of all of the nodes by ID, and another by name.
     */

    private static Validation<List<CaCompileError>, BoneIndex>
    compileBonesCreateIndex(
      final JOTreeNodeType<CaCompiledBone> root)
    {
      final java.util.Map<CaBoneName, JOTreeNodeType<CaCompiledBone>> by_name =
        new HashMap<>(16);
      final java.util.Map<Integer, JOTreeNodeType<CaCompiledBone>> by_id =
        new HashMap<>(16);

      root.forEachDepthFirst(Unit.unit(), (ignored, depth, node) -> {
        final CaCompiledBone bone = node.value();
        final Integer bone_id = Integer.valueOf(bone.id());
        final CaBoneName bone_name = bone.name();

        Invariants.checkInvariant(
          !by_name.containsKey(bone_name),
          "Name must not be duplicated");
        Invariants.checkInvariant(
          !by_id.containsKey(bone_id),
          "ID must not be duplicated");

        by_name.put(bone_name, (JOTreeNodeType<CaCompiledBone>) node);
        by_id.put(bone_id, (JOTreeNodeType<CaCompiledBone>) node);
      });

      return valid(new BoneIndex(
        root,
        javaslang.collection.TreeMap.ofAll(by_name),
        javaslang.collection.TreeMap.ofAll(by_id)));
    }

    Validation<List<CaCompileError>, CaCompiledSkeletonType> run()
    {
      final Map<CaBoneName, CaDefinitionBone> bones = this.input.bones();
      return compileFindRootBone(bones)
        .flatMap(root -> compileBuildDefinitionTree(bones, root))
        .flatMap(CompileTask::compileBonesAssignIdentifiers)
        .flatMap(CompileTask::compileBonesCreateIndex)
        .flatMap(index -> valid(new Skeleton(
          index.bones,
          index.bones_by_name,
          index.bones_by_id)));
    }

    private static final class Skeleton implements CaCompiledSkeletonType
    {
      private final JOTreeNodeType<CaCompiledBone> bones;
      private final SortedMap<CaBoneName, JOTreeNodeType<CaCompiledBone>> bones_by_name;
      private final SortedMap<Integer, JOTreeNodeType<CaCompiledBone>> bones_by_id;

      Skeleton(
        final JOTreeNodeType<CaCompiledBone> in_bones,
        final SortedMap<CaBoneName, JOTreeNodeType<CaCompiledBone>> in_bones_by_name,
        final SortedMap<Integer, JOTreeNodeType<CaCompiledBone>> in_bones_by_id)
      {
        this.bones =
          NullCheck.notNull(in_bones, "Bones");
        this.bones_by_name =
          NullCheck.notNull(in_bones_by_name, "Bones by name");
        this.bones_by_id =
          NullCheck.notNull(in_bones_by_id, "Bones by ID");
      }

      @Override
      public JOTreeNodeReadableType<CaCompiledBone> bones()
      {
        return this.bones;
      }

      @Override
      @SuppressWarnings("unchecked")
      public SortedMap<CaBoneName, JOTreeNodeReadableType<CaCompiledBone>> bonesByName()
      {
        return (SortedMap<CaBoneName, JOTreeNodeReadableType<CaCompiledBone>>) (Object) this.bones_by_name;
      }

      @Override
      @SuppressWarnings("unchecked")
      public SortedMap<Integer, JOTreeNodeReadableType<CaCompiledBone>> bonesByID()
      {
        return (SortedMap<Integer, JOTreeNodeReadableType<CaCompiledBone>>) (Object) this.bones_by_id;
      }
    }
  }
}
