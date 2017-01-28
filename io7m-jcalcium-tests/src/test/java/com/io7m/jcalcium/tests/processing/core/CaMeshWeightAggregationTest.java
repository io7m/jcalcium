/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
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

package com.io7m.jcalcium.tests.processing.core;

import com.io7m.jaffirm.core.PreconditionViolationException;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.CaSkeletonHash;
import com.io7m.jcalcium.mesh.processing.core.CaMeshWeightAggregation;
import com.io7m.jcalcium.mesh.processing.core.CaMeshWeightsAggregated;
import com.io7m.jorchard.core.JOTreeNode;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.VectorI4D;
import com.io7m.jtensors.VectorI4L;
import com.io7m.jtensors.parameterized.PVectorI3D;
import javaslang.Tuple;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
import javaslang.collection.Vector;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class CaMeshWeightAggregationTest
{
  private static final CaSkeletonHash DEFAULT_HASH =
    CaSkeletonHash.of(
      "SHA2-256",
      "3cb4e2c9e926cce1aa345f1e1724db8683a2aa1056b236ecd7c3aba318a9416d");

  @Rule public ExpectedException expected = ExpectedException.none();

  @Test
  public void testJoint_1()
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);

    final CaSkeleton skeleton = CaSkeleton.of(
      CaSkeletonName.of("skeleton"), DEFAULT_HASH, joints, TreeMap.empty());

    final SortedMap<CaJointName, Vector<Double>> weight_arrays =
      TreeMap.of(CaJointName.of("root"), Vector.of(Double.valueOf(1.0)));

    final CaMeshWeightsAggregated weights =
      CaMeshWeightAggregation.aggregateWeights(skeleton, weight_arrays);

    final Vector<VectorI4L> vi = weights.vertexBoneIndices();
    final Vector<VectorI4D> vw = weights.vertexWeights();

    Assert.assertEquals(8L, (long) weights.indexBitsRequired());
    Assert.assertEquals(1L, (long) vi.size());
    Assert.assertEquals(1L, (long) vw.size());

    Assert.assertEquals(0L, vi.get(0).getXL());
    Assert.assertEquals(0L, vi.get(0).getYL());
    Assert.assertEquals(0L, vi.get(0).getZL());
    Assert.assertEquals(0L, vi.get(0).getWL());
    Assert.assertEquals(1.0, vw.get(0).getXD(), 0.0);
    Assert.assertEquals(0.0, vw.get(0).getYD(), 0.0);
    Assert.assertEquals(0.0, vw.get(0).getZD(), 0.0);
    Assert.assertEquals(0.0, vw.get(0).getWD(), 0.0);
  }

  @Test
  public void testJoint_2()
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_a =
      CaJoint.of(
        CaJointName.of("a"),
        1,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);
    joints.childAdd(JOTreeNode.create(joint_a));

    final CaSkeleton skeleton = CaSkeleton.of(
      CaSkeletonName.of("skeleton"), DEFAULT_HASH, joints, TreeMap.empty());

    final SortedMap<CaJointName, Vector<Double>> weight_arrays =
      TreeMap.ofEntries(
        Tuple.of(joint_root.name(), Vector.of(Double.valueOf(1.0))),
        Tuple.of(joint_a.name(), Vector.of(Double.valueOf(0.0)))
      );

    final CaMeshWeightsAggregated weights =
      CaMeshWeightAggregation.aggregateWeights(skeleton, weight_arrays);

    final Vector<VectorI4L> vi = weights.vertexBoneIndices();
    final Vector<VectorI4D> vw = weights.vertexWeights();

    Assert.assertEquals(8L, (long) weights.indexBitsRequired());
    Assert.assertEquals(1L, (long) vi.size());
    Assert.assertEquals(1L, (long) vw.size());

    Assert.assertEquals(0L, vi.get(0).getXL());
    Assert.assertEquals(1L, vi.get(0).getYL());
    Assert.assertEquals(0L, vi.get(0).getZL());
    Assert.assertEquals(0L, vi.get(0).getWL());
    Assert.assertEquals(1.0, vw.get(0).getXD(), 0.000001);
    Assert.assertEquals(0.0, vw.get(0).getYD(), 0.0);
    Assert.assertEquals(0.0, vw.get(0).getZD(), 0.0);
    Assert.assertEquals(0.0, vw.get(0).getWD(), 0.0);
  }

  @Test
  public void testJoint_3()
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_a =
      CaJoint.of(
        CaJointName.of("a"),
        1,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_b =
      CaJoint.of(
        CaJointName.of("b"),
        2,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);
    joints.childAdd(JOTreeNode.create(joint_a));
    joints.childAdd(JOTreeNode.create(joint_b));

    final CaSkeleton skeleton = CaSkeleton.of(
      CaSkeletonName.of("skeleton"), DEFAULT_HASH, joints, TreeMap.empty());

    final SortedMap<CaJointName, Vector<Double>> weight_arrays =
      TreeMap.ofEntries(
        Tuple.of(joint_root.name(), Vector.of(Double.valueOf(1.0))),
        Tuple.of(joint_a.name(), Vector.of(Double.valueOf(0.0))),
        Tuple.of(joint_b.name(), Vector.of(Double.valueOf(0.0)))
      );

    final CaMeshWeightsAggregated weights =
      CaMeshWeightAggregation.aggregateWeights(skeleton, weight_arrays);

    final Vector<VectorI4L> vi = weights.vertexBoneIndices();
    final Vector<VectorI4D> vw = weights.vertexWeights();

    Assert.assertEquals(8L, (long) weights.indexBitsRequired());
    Assert.assertEquals(1L, (long) vi.size());
    Assert.assertEquals(1L, (long) vw.size());

    Assert.assertEquals(0L, vi.get(0).getXL());
    Assert.assertEquals(1L, vi.get(0).getYL());
    Assert.assertEquals(2L, vi.get(0).getZL());
    Assert.assertEquals(0L, vi.get(0).getWL());
    Assert.assertEquals(1.0, vw.get(0).getXD(), 0.000001);
    Assert.assertEquals(0.0, vw.get(0).getYD(), 0.0);
    Assert.assertEquals(0.0, vw.get(0).getZD(), 0.0);
    Assert.assertEquals(0.0, vw.get(0).getWD(), 0.0);
  }

  @Test
  public void testJoint_4()
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_a =
      CaJoint.of(
        CaJointName.of("a"),
        1,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_b =
      CaJoint.of(
        CaJointName.of("b"),
        2,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_c =
      CaJoint.of(
        CaJointName.of("c"),
        3,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);
    joints.childAdd(JOTreeNode.create(joint_a));
    joints.childAdd(JOTreeNode.create(joint_b));
    joints.childAdd(JOTreeNode.create(joint_c));

    final CaSkeleton skeleton = CaSkeleton.of(
      CaSkeletonName.of("skeleton"), DEFAULT_HASH, joints, TreeMap.empty());

    final SortedMap<CaJointName, Vector<Double>> weight_arrays =
      TreeMap.ofEntries(
        Tuple.of(joint_root.name(), Vector.of(Double.valueOf(1.0))),
        Tuple.of(joint_a.name(), Vector.of(Double.valueOf(0.0))),
        Tuple.of(joint_b.name(), Vector.of(Double.valueOf(0.0))),
        Tuple.of(joint_c.name(), Vector.of(Double.valueOf(0.0)))
      );

    final CaMeshWeightsAggregated weights =
      CaMeshWeightAggregation.aggregateWeights(skeleton, weight_arrays);

    final Vector<VectorI4L> vi = weights.vertexBoneIndices();
    final Vector<VectorI4D> vw = weights.vertexWeights();

    Assert.assertEquals(8L, (long) weights.indexBitsRequired());
    Assert.assertEquals(1L, (long) vi.size());
    Assert.assertEquals(1L, (long) vw.size());

    Assert.assertEquals(0L, vi.get(0).getXL());
    Assert.assertEquals(1L, vi.get(0).getYL());
    Assert.assertEquals(2L, vi.get(0).getZL());
    Assert.assertEquals(3L, vi.get(0).getWL());
    Assert.assertEquals(1.0, vw.get(0).getXD(), 0.000001);
    Assert.assertEquals(0.0, vw.get(0).getYD(), 0.0);
    Assert.assertEquals(0.0, vw.get(0).getZD(), 0.0);
    Assert.assertEquals(0.0, vw.get(0).getWD(), 0.0);
  }

  @Test
  public void testJoint_8()
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_a =
      CaJoint.of(
        CaJointName.of("a"),
        1,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_b =
      CaJoint.of(
        CaJointName.of("b"),
        2,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_c =
      CaJoint.of(
        CaJointName.of("c"),
        3,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_d =
      CaJoint.of(
        CaJointName.of("d"),
        4,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_e =
      CaJoint.of(
        CaJointName.of("e"),
        5,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_f =
      CaJoint.of(
        CaJointName.of("f"),
        6,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_g =
      CaJoint.of(
        CaJointName.of("g"),
        7,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);
    joints.childAdd(JOTreeNode.create(joint_a));
    joints.childAdd(JOTreeNode.create(joint_b));
    joints.childAdd(JOTreeNode.create(joint_c));
    joints.childAdd(JOTreeNode.create(joint_d));
    joints.childAdd(JOTreeNode.create(joint_e));
    joints.childAdd(JOTreeNode.create(joint_f));
    joints.childAdd(JOTreeNode.create(joint_g));

    final CaSkeleton skeleton = CaSkeleton.of(
      CaSkeletonName.of("skeleton"), DEFAULT_HASH, joints, TreeMap.empty());

    final SortedMap<CaJointName, Vector<Double>> weight_arrays =
      TreeMap.ofEntries(
        Tuple.of(joint_root.name(), Vector.of(Double.valueOf(1.0))),
        Tuple.of(joint_a.name(), Vector.of(Double.valueOf(0.9))),
        Tuple.of(joint_b.name(), Vector.of(Double.valueOf(0.8))),
        Tuple.of(joint_c.name(), Vector.of(Double.valueOf(0.7))),
        Tuple.of(joint_d.name(), Vector.of(Double.valueOf(0.6))),
        Tuple.of(joint_e.name(), Vector.of(Double.valueOf(0.5))),
        Tuple.of(joint_f.name(), Vector.of(Double.valueOf(0.4))),
        Tuple.of(joint_g.name(), Vector.of(Double.valueOf(0.3)))
      );

    final CaMeshWeightsAggregated weights =
      CaMeshWeightAggregation.aggregateWeights(skeleton, weight_arrays);

    final Vector<VectorI4L> vi = weights.vertexBoneIndices();
    final Vector<VectorI4D> vw = weights.vertexWeights();

    Assert.assertEquals(8L, (long) weights.indexBitsRequired());
    Assert.assertEquals(1L, (long) vi.size());
    Assert.assertEquals(1L, (long) vw.size());

    Assert.assertEquals(0L, vi.get(0).getXL());
    Assert.assertEquals(1L, vi.get(0).getYL());
    Assert.assertEquals(2L, vi.get(0).getZL());
    Assert.assertEquals(3L, vi.get(0).getWL());

    final VectorI4D vvw = vw.get(0);
    Assert.assertEquals(
      1.0,
      vvw.getXD() + vvw.getYD() + vvw.getZD() + vvw.getWD(),
      0.000001);
  }

  @Test
  public void testNoWeights()
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);

    final CaSkeleton skeleton = CaSkeleton.of(
      CaSkeletonName.of("skeleton"), DEFAULT_HASH, joints, TreeMap.empty());

    final SortedMap<CaJointName, Vector<Double>> weight_arrays =
      TreeMap.empty();

    this.expected.expect(PreconditionViolationException.class);
    this.expected.expectMessage(StringContains.containsString(
      "Must provide at least one weight array"));
    CaMeshWeightAggregation.aggregateWeights(skeleton, weight_arrays);
  }

  @Test
  public void testUnknownJoint()
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);

    final CaSkeleton skeleton = CaSkeleton.of(
      CaSkeletonName.of("skeleton"), DEFAULT_HASH, joints, TreeMap.empty());

    final SortedMap<CaJointName, Vector<Double>> weight_arrays =
      TreeMap.ofEntries(
        Tuple.of(CaJointName.of("nonexistent"), Vector.of(Double.valueOf(1.0)))
      );

    this.expected.expect(PreconditionViolationException.class);
    this.expected.expectMessage(StringContains.containsString(
      "Skeleton must contain the named joint"));
    CaMeshWeightAggregation.aggregateWeights(skeleton, weight_arrays);
  }
}
