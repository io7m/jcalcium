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

package com.io7m.jcalcium.tests.compiler.api;

import com.io7m.jcalcium.compiler.api.CaCompileError;
import com.io7m.jcalcium.compiler.api.CaCompileErrorCode;
import com.io7m.jcalcium.compiler.api.CaCompilerType;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaCompiledBone;
import com.io7m.jcalcium.core.compiled.CaCompiledSkeletonType;
import com.io7m.jcalcium.core.definitions.CaDefinitionBone;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.tests.BoneNameTree;
import com.io7m.jcalcium.tests.BoneNameTreeGenerator;
import com.io7m.jfunctional.Unit;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.generators.QuaternionI4DGenerator;
import com.io7m.jtensors.generators.VectorI3DGenerator;
import com.io7m.jtensors.generators.parameterized.PVectorI3DGenerator;
import com.io7m.jtensors.parameterized.PVectorI3D;
import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.control.Validation;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class CaCompilerContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaCompilerContract.class);
  }

  private static void dump(
    final Validation<List<CaCompileError>, CaCompiledSkeletonType> r)
  {
    if (r.isValid()) {
      LOG.debug("valid: {}", r.get());
    } else {
      r.getError().forEach(e -> LOG.debug("invalid: {}", e));
    }
  }

  protected abstract CaCompilerType create();

  @Test
  public void testCompileEmpty()
  {
    final CaCompilerType cc = this.create();

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setBones(HashMap.empty());

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaCompiledSkeletonType> r =
      cc.compile(s);

    dump(r);
    Assert.assertFalse(r.isValid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_NO_ROOT_BONE,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileMultipleRoots()
  {
    final CaCompilerType cc = this.create();

    final CaBoneName bone_0_name = CaBoneName.of("root0");
    final CaDefinitionBone bone_0 = CaDefinitionBone.of(
      bone_0_name,
      Optional.empty(),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D());
    final CaBoneName bone_1_name = CaBoneName.of("root1");
    final CaDefinitionBone bone_1 = CaDefinitionBone.of(
      bone_1_name,
      Optional.empty(),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D());

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setBones(HashMap.ofEntries(
      Tuple.of(bone_0_name, bone_0),
      Tuple.of(bone_1_name, bone_1)));

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaCompiledSkeletonType> r =
      cc.compile(s);

    dump(r);
    Assert.assertFalse(r.isValid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_MULTIPLE_ROOT_BONES,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileNonexistentParent()
  {
    final CaCompilerType cc = this.create();

    final CaBoneName bone_0_name = CaBoneName.of("root0");
    final CaDefinitionBone bone_0 = CaDefinitionBone.of(
      bone_0_name,
      Optional.empty(),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D());
    final CaBoneName bone_1_name = CaBoneName.of("root1");
    final CaBoneName bone_2_name = CaBoneName.of("root2");
    final CaDefinitionBone bone_1 = CaDefinitionBone.of(
      bone_1_name,
      Optional.of(bone_2_name),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D());

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setBones(HashMap.ofEntries(
      Tuple.of(bone_0_name, bone_0),
      Tuple.of(bone_1_name, bone_1)));

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaCompiledSkeletonType> r =
      cc.compile(s);

    dump(r);
    Assert.assertFalse(r.isValid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_NONEXISTENT_PARENT,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileCyclic()
  {
    final CaCompilerType cc = this.create();

    final CaBoneName bone_0_name = CaBoneName.of("root0");
    final CaDefinitionBone bone_0 = CaDefinitionBone.of(
      bone_0_name,
      Optional.empty(),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D());

    final CaBoneName bone_1_name = CaBoneName.of("root1");
    final CaBoneName bone_2_name = CaBoneName.of("root2");
    final CaDefinitionBone bone_1 = CaDefinitionBone.of(
      bone_1_name,
      Optional.of(bone_2_name),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D());
    final CaDefinitionBone bone_2 = CaDefinitionBone.of(
      bone_2_name,
      Optional.of(bone_1_name),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D());

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setBones(HashMap.ofEntries(
      Tuple.of(bone_0_name, bone_0),
      Tuple.of(bone_1_name, bone_1),
      Tuple.of(bone_2_name, bone_2)));

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaCompiledSkeletonType> r =
      cc.compile(s);

    dump(r);
    Assert.assertFalse(r.isValid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_CYCLE,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileCorrectBones()
  {
    final CaCompilerType cc = this.create();

    final VectorI3DGenerator vgen =
      new VectorI3DGenerator();
    final PVectorI3DGenerator<CaSpaceBoneParentRelativeType> pvgen =
      new PVectorI3DGenerator<>();
    final QuaternionI4DGenerator qgen =
      new QuaternionI4DGenerator();

    final BoneNameTreeGenerator gen = new BoneNameTreeGenerator();
    final BoneNameTree tree = gen.next();

    final StringBuilder sb = new StringBuilder(100);
    tree.tree.forEachDepthFirst(Unit.unit(), (input, depth, node) -> {
      sb.setLength(0);
      for (int index = 0; index < depth; ++index) {
        sb.append("  ");
      }
      sb.append(node.value().value());
      LOG.debug("tree: {}", sb.toString());
    });

    final Map<CaBoneName, CaDefinitionBone> bones =
      tree.nodes.map((CaBoneName bone_name, JOTreeNodeReadableType<CaBoneName> node) -> {
        final CaDefinitionBone.Builder cb = CaDefinitionBone.builder();
        cb.setName(node.value());
        cb.setScale(vgen.next());
        cb.setOrientation(qgen.next());
        cb.setTranslation(pvgen.next());
        node.parentReadable().map(p -> cb.setParent(p.value()));
        return Tuple.of(bone_name, cb.build());
      });

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setBones(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaCompiledSkeletonType> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isValid());

    final CaCompiledSkeletonType compiled = r.get();

    final Map<CaBoneName, JOTreeNodeReadableType<CaCompiledBone>> by_name =
      compiled.bonesByName();
    final Map<Integer, JOTreeNodeReadableType<CaCompiledBone>> by_id =
      compiled.bonesByID();

    Assert.assertEquals((long) by_id.size(), (long) by_name.size());

    final Collection<Integer> ids_unique = new HashSet<>();

    for (final CaBoneName bone_name : bones.keySet()) {
      Assert.assertTrue(by_name.containsKey(bone_name));

      final JOTreeNodeReadableType<CaCompiledBone> compiled_node =
        by_name.get(bone_name).get();
      final CaCompiledBone compiled_bone =
        compiled_node.value();

      final Integer id = Integer.valueOf(compiled_bone.id());
      Assert.assertFalse(ids_unique.contains(id));
      ids_unique.add(id);

      final JOTreeNodeReadableType<CaBoneName> original_node =
        tree.nodes.get(bone_name).get();
      final CaDefinitionBone original_bone =
        bones.get(bone_name).get();

      Assert.assertEquals(
        Boolean.valueOf(compiled_node.parentReadable().isPresent()),
        Boolean.valueOf(original_bone.parent().isPresent()));

      if (compiled_node.parentReadable().isPresent()) {
        final JOTreeNodeReadableType<CaCompiledBone> c_parent =
          compiled_node.parentReadable().get();
        final CaBoneName b_parent =
          original_bone.parent().get();
        Assert.assertEquals(b_parent, c_parent.value().name());
      }

      Assert.assertEquals(
        (long) original_node.childrenReadable().size(),
        (long) compiled_node.childrenReadable().size());

      Assert.assertEquals(
        original_bone.name(), compiled_bone.name());
      Assert.assertEquals(
        original_bone.translation(), compiled_bone.translation());
      Assert.assertEquals(
        original_bone.scale(), compiled_bone.scale());
      Assert.assertEquals(
        original_bone.orientation(), compiled_bone.orientation());
    }
  }
}
