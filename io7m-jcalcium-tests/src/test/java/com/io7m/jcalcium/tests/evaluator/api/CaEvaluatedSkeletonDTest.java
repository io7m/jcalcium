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


package com.io7m.jcalcium.tests.evaluator.api;

import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.CaSkeletonHash;
import com.io7m.jcalcium.core.compiled.CaSkeletonMetadata;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPose;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPoseDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointMutableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointReadableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedSkeletonD;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedSkeletonMutableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContext;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextType;
import com.io7m.jorchard.core.JOTreeNode;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import javaslang.collection.TreeMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public final class CaEvaluatedSkeletonDTest
{
  @Test
  public void testEvaluatedSkeleton()
  {
    final CaJoint joint_0 =
      CaJoint.of(
        CaJointName.of("joint.000"),
        0,
        new PVectorI3D<>(0.0, 0.0, 0.0),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_1 =
      CaJoint.of(
        CaJointName.of("joint.001"),
        1,
        new PVectorI3D<>(0.0, 0.0, 0.0),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_2 =
      CaJoint.of(
        CaJointName.of("joint.002"),
        2,
        new PVectorI3D<>(0.0, 0.0, 0.0),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> node_0 = JOTreeNode.create(joint_0);
    final JOTreeNodeType<CaJoint> node_1 = JOTreeNode.create(joint_1);
    final JOTreeNodeType<CaJoint> node_2 = JOTreeNode.create(joint_2);
    node_0.childAdd(node_1);
    node_1.childAdd(node_2);

    final CaSkeleton.Builder cb = CaSkeleton.builder();
    cb.setJoints(node_0);
    cb.setActionsByName(TreeMap.empty());
    cb.setMeta(CaSkeletonMetadata.of(
      CaSkeletonName.of("skeleton"),
      CaSkeletonHash.of("SHA2-256", "3cb4e2c9e926cce1aa345f1e1724db8683a2aa1056b236ecd7c3aba318a9416d")));

    final CaSkeleton skeleton = cb.build();

    final CaEvaluationContextType context =
      CaEvaluationContext.create();
    final CaSkeletonRestPoseDType rest_pose =
      CaSkeletonRestPose.createD(new MatrixM4x4D.ContextMM4D(), skeleton);

    final CaEvaluatedSkeletonMutableDType eval_skeleton =
      CaEvaluatedSkeletonD.create(context, rest_pose);

    Assert.assertEquals(
      rest_pose, eval_skeleton.restPose());
    Assert.assertEquals(
      eval_skeleton.jointsMutableByID(), eval_skeleton.jointsByID());
    Assert.assertEquals(
      eval_skeleton.jointsMutable(), eval_skeleton.joints());

    rest_pose.skeleton().jointsByID().forEach((joint_id, joint_node) -> {
      final CaJoint joint = joint_node.value();
      final CaEvaluatedJointMutableDType eval_joint =
        eval_skeleton.jointsMutableByID().get(joint_id.intValue());

      Assert.assertEquals(joint.name(), eval_joint.name());
      Assert.assertEquals((long) joint.id(), (long) eval_joint.id());

      Assert.assertEquals(
        joint_0.translation(),
        new PVectorI3D<>(eval_joint.translation3D()));
      Assert.assertEquals(
        joint_0.scale(),
        new VectorI3D(eval_joint.scale3D()));
      Assert.assertEquals(
        joint_0.orientation(),
        new QuaternionI4D(eval_joint.orientation4D()));

      final Optional<CaJoint> joint_parent_opt =
        joint_node.parentReadable().map(JOTreeNodeReadableType::value);
      final Optional<CaEvaluatedJointReadableDType> eval_joint_parent_opt =
        eval_joint.parent();

      Assert.assertEquals(
        Boolean.valueOf(joint_parent_opt.isPresent()),
        Boolean.valueOf(eval_joint_parent_opt.isPresent()));

      joint_parent_opt.ifPresent(joint_parent -> {
        final CaEvaluatedJointReadableDType eval_joint_parent =
          eval_joint_parent_opt.get();

        Assert.assertEquals(
          (long) joint_parent.id(),
          (long) eval_joint_parent.id());
      });
    });
  }
}
