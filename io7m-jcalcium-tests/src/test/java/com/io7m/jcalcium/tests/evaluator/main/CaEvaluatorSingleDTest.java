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

package com.io7m.jcalcium.tests.evaluator.main;

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPose;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPoseDType;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurves;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointReadableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedSkeletonD;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedSkeletonMutableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContext;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorSingleType;
import com.io7m.jcalcium.evaluator.main.CaEvaluatorSingleD;
import com.io7m.jorchard.core.JOTreeNode;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.VectorI4D;
import com.io7m.jtensors.VectorM4D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import javaslang.collection.IndexedSeq;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
import javaslang.collection.Vector;
import org.junit.Assert;
import org.junit.Test;

public final class CaEvaluatorSingleDTest
{
  private static final double DELTA = 0.0000001;

  @Test
  public void testTransforms()
  {
    SortedMap<Integer, JOTreeNodeReadableType<CaJoint>> joints_by_id = TreeMap.empty();
    SortedMap<CaJointName, JOTreeNodeReadableType<CaJoint>> joints_by_name = TreeMap.empty();

    final CaJoint joint_0 = CaJoint.of(
      CaJointName.of("joint.000"),
      0,
      new PVectorI3D<>(0.0, 0.0, 0.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_1 = CaJoint.of(
      CaJointName.of("joint.001"),
      1,
      new PVectorI3D<>(1.0, 0.0, 0.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_2 = CaJoint.of(
      CaJointName.of("joint.002"),
      2,
      new PVectorI3D<>(1.0, 0.0, 0.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_3 = CaJoint.of(
      CaJointName.of("joint.003"),
      3,
      new PVectorI3D<>(1.0, 0.0, 0.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> node_0 = JOTreeNode.create(joint_0);
    final JOTreeNodeType<CaJoint> node_1 = JOTreeNode.create(joint_1);
    final JOTreeNodeType<CaJoint> node_2 = JOTreeNode.create(joint_2);
    final JOTreeNodeType<CaJoint> node_3 = JOTreeNode.create(joint_3);

    node_0.childAdd(node_1);
    node_1.childAdd(node_2);
    node_2.childAdd(node_3);

    joints_by_id = joints_by_id.put(Integer.valueOf(joint_0.id()), node_0);
    joints_by_id = joints_by_id.put(Integer.valueOf(joint_1.id()), node_1);
    joints_by_id = joints_by_id.put(Integer.valueOf(joint_2.id()), node_2);
    joints_by_id = joints_by_id.put(Integer.valueOf(joint_3.id()), node_3);

    joints_by_name = joints_by_name.put(joint_0.name(), node_0);
    joints_by_name = joints_by_name.put(joint_1.name(), node_1);
    joints_by_name = joints_by_name.put(joint_2.name(), node_2);
    joints_by_name = joints_by_name.put(joint_3.name(), node_3);

    SortedMap<CaActionName, CaActionType> actions_by_name = TreeMap.empty();
    final CaActionCurves act;

    {
      final CaCurveKeyframeTranslation curve_kf =
        CaCurveKeyframeTranslation.of(
          0,
          CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
          CaCurveEasing.CURVE_EASING_IN_OUT,
          new PVectorI3D<>(0.0, 0.0, 0.0));

      final CaCurveTranslation.Builder curve_b = CaCurveTranslation.builder();
      curve_b.setAction(CaActionName.of("act"));
      curve_b.setJoint(CaJointName.of("joint.000"));
      curve_b.setKeyframes(TreeMap.of(
        Integer.valueOf(curve_kf.index()),
        curve_kf));
      final CaCurveTranslation curve = curve_b.build();

      SortedMap<CaJointName, IndexedSeq<CaCurveType>> curves = TreeMap.empty();
      curves = curves.put(CaJointName.of("joint.000"), Vector.of(curve));

      final CaActionCurves.Builder act_b = CaActionCurves.builder();
      act_b.setName(CaActionName.of("act"));
      act_b.setFramesPerSecond(60);
      act_b.setCurves(curves);

      act = act_b.build();
      actions_by_name = actions_by_name.put(act.name(), act);
    }

    final CaSkeleton.Builder cb = CaSkeleton.builder();
    cb.setName(CaSkeletonName.of("skeleton"));
    cb.setJoints(node_0);
    cb.setActionsByName(actions_by_name);

    final MatrixM4x4D.ContextMM4D context = new MatrixM4x4D.ContextMM4D();
    final CaSkeleton skeleton = cb.build();
    final CaSkeletonRestPoseDType rest_pose =
      CaSkeletonRestPose.createD(context, skeleton);

    final CaEvaluationContextType eval_context =
      CaEvaluationContext.create();
    final CaEvaluatedSkeletonMutableDType eval_skeleton =
      CaEvaluatedSkeletonD.create(eval_context, rest_pose);
    final CaEvaluatorSingleType eval =
      CaEvaluatorSingleD.create(eval_context, eval_skeleton, act, 60);

    eval.evaluateForGlobalFrame(0L, 0L, 1.0);

    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_joints =
      eval_skeleton.joints();
    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_0 =
      eval_joints;

    final CaEvaluatedJointReadableDType eval_joint_0 = eval_node_0.value();

    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_1 =
      eval_node_0.childrenReadable().iterator().next();
    final CaEvaluatedJointReadableDType eval_joint_1 = eval_node_1.value();

    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_2 =
      eval_node_1.childrenReadable().iterator().next();
    final CaEvaluatedJointReadableDType eval_joint_2 = eval_node_2.value();

    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_3 =
      eval_node_2.childrenReadable().iterator().next();
    final CaEvaluatedJointReadableDType eval_joint_3 = eval_node_3.value();

    final VectorM4D output = new VectorM4D();

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_0.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(0.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_1.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(1.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_2.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(2.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_3.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(3.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);
  }

  /**
   * Translating the root joint results in a correctly translated transform
   * for all joints.
   */

  @Test
  public void testTranslateThree()
  {
    SortedMap<Integer, JOTreeNodeReadableType<CaJoint>> joints_by_id = TreeMap.empty();
    SortedMap<CaJointName, JOTreeNodeReadableType<CaJoint>> joints_by_name = TreeMap.empty();

    final CaJoint joint_0 = CaJoint.of(
      CaJointName.of("joint.000"),
      0,
      new PVectorI3D<>(0.0, 0.0, 0.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_1 = CaJoint.of(
      CaJointName.of("joint.001"),
      1,
      new PVectorI3D<>(0.0, 0.0, -1.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_2 = CaJoint.of(
      CaJointName.of("joint.002"),
      2,
      new PVectorI3D<>(0.0, 0.0, -1.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> node_0 = JOTreeNode.create(joint_0);
    final JOTreeNodeType<CaJoint> node_1 = JOTreeNode.create(joint_1);
    final JOTreeNodeType<CaJoint> node_2 = JOTreeNode.create(joint_2);
    node_0.childAdd(node_1);
    node_1.childAdd(node_2);

    joints_by_id = joints_by_id.put(Integer.valueOf(joint_0.id()), node_0);
    joints_by_id = joints_by_id.put(Integer.valueOf(joint_1.id()), node_1);
    joints_by_id = joints_by_id.put(Integer.valueOf(joint_2.id()), node_2);

    joints_by_name = joints_by_name.put(joint_0.name(), node_0);
    joints_by_name = joints_by_name.put(joint_1.name(), node_1);
    joints_by_name = joints_by_name.put(joint_2.name(), node_2);

    SortedMap<CaActionName, CaActionType> actions_by_name = TreeMap.empty();
    final CaActionCurves act;

    {
      final CaCurveKeyframeTranslation curve_kf_0 =
        CaCurveKeyframeTranslation.of(
          0,
          CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
          CaCurveEasing.CURVE_EASING_IN_OUT,
          new PVectorI3D<>(0.0, 0.0, 0.0));
      final CaCurveKeyframeTranslation curve_kf_1 =
        CaCurveKeyframeTranslation.of(
          10,
          CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
          CaCurveEasing.CURVE_EASING_IN_OUT,
          new PVectorI3D<>(1.0, 0.0, 0.0));
      final CaCurveKeyframeTranslation curve_kf_2 =
        CaCurveKeyframeTranslation.of(
          11,
          CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
          CaCurveEasing.CURVE_EASING_IN_OUT,
          new PVectorI3D<>(1.0, 0.0, 0.0));

      final CaCurveTranslation.Builder curve_b = CaCurveTranslation.builder();
      curve_b.setAction(CaActionName.of("act"));
      curve_b.setJoint(joint_0.name());
      curve_b.putKeyframes(Integer.valueOf(curve_kf_0.index()), curve_kf_0);
      curve_b.putKeyframes(Integer.valueOf(curve_kf_1.index()), curve_kf_1);
      curve_b.putKeyframes(Integer.valueOf(curve_kf_2.index()), curve_kf_2);
      final CaCurveTranslation curve = curve_b.build();

      SortedMap<CaJointName, IndexedSeq<CaCurveType>> curves = TreeMap.empty();
      curves = curves.put(joint_0.name(), Vector.of(curve));

      final CaActionCurves.Builder act_b = CaActionCurves.builder();
      act_b.setName(CaActionName.of("act"));
      act_b.setFramesPerSecond(60);
      act_b.setCurves(curves);

      act = act_b.build();
      actions_by_name = actions_by_name.put(act.name(), act);
    }

    final CaSkeleton.Builder cb = CaSkeleton.builder();
    cb.setName(CaSkeletonName.of("skeleton"));
    cb.setJoints(node_0);
    cb.setActionsByName(actions_by_name);

    final MatrixM4x4D.ContextMM4D context = new MatrixM4x4D.ContextMM4D();

    final CaSkeleton skeleton = cb.build();
    final CaSkeletonRestPoseDType rest_pose =
      CaSkeletonRestPose.createD(context, skeleton);

    final CaEvaluationContextType eval_context =
      CaEvaluationContext.create();
    final CaEvaluatedSkeletonMutableDType eval_skeleton =
      CaEvaluatedSkeletonD.create(eval_context, rest_pose);
    final CaEvaluatorSingleType eval =
      CaEvaluatorSingleD.create(eval_context, eval_skeleton, act, 60);

    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_joints =
      eval_skeleton.joints();
    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_0 =
      eval_joints;
    final CaEvaluatedJointReadableDType eval_joint_0 =
      eval_node_0.value();
    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_1 =
      eval_node_0.childrenReadable().iterator().next();
    final CaEvaluatedJointReadableDType eval_joint_1 =
      eval_node_1.value();
    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_2 =
      eval_node_1.childrenReadable().iterator().next();
    final CaEvaluatedJointReadableDType eval_joint_2 =
      eval_node_2.value();

    final VectorM4D output = new VectorM4D();
    eval.evaluateForGlobalFrame(0L, 0L, 1.0);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_0.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(0.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_1.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(0.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(-1.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_2.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(0.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(-2.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    eval.evaluateForGlobalFrame(0L, 10L, 1.0);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_0.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(1.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_1.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(1.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(-1.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_2.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(1.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(-2.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);
  }

  /**
   * Rotating the root joint results in a correctly rotated transform
   * for all joints.
   */

  @Test
  public void testRotateThree()
  {
    SortedMap<Integer, JOTreeNodeReadableType<CaJoint>> joints_by_id = TreeMap.empty();
    SortedMap<CaJointName, JOTreeNodeReadableType<CaJoint>> joints_by_name = TreeMap.empty();

    final CaJoint joint_0 = CaJoint.of(
      CaJointName.of("joint.000"),
      0,
      new PVectorI3D<>(0.0, 0.0, 0.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_1 = CaJoint.of(
      CaJointName.of("joint.001"),
      1,
      new PVectorI3D<>(0.0, 0.0, -1.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint joint_2 = CaJoint.of(
      CaJointName.of("joint.002"),
      2,
      new PVectorI3D<>(0.0, 0.0, -1.0),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> node_0 = JOTreeNode.create(joint_0);
    final JOTreeNodeType<CaJoint> node_1 = JOTreeNode.create(joint_1);
    final JOTreeNodeType<CaJoint> node_2 = JOTreeNode.create(joint_2);
    node_0.childAdd(node_1);
    node_1.childAdd(node_2);

    joints_by_id = joints_by_id.put(Integer.valueOf(joint_0.id()), node_0);
    joints_by_id = joints_by_id.put(Integer.valueOf(joint_1.id()), node_1);
    joints_by_id = joints_by_id.put(Integer.valueOf(joint_2.id()), node_2);

    joints_by_name = joints_by_name.put(joint_0.name(), node_0);
    joints_by_name = joints_by_name.put(joint_1.name(), node_1);
    joints_by_name = joints_by_name.put(joint_2.name(), node_2);

    SortedMap<CaActionName, CaActionType> actions_by_name = TreeMap.empty();
    final CaActionCurves act;

    {
      /*
       * A 90-degree rotation around the positive Y axis should result in
       * a 90-degree anti-clockwise rotation when looking down the Y axis
       * towards negative infinity.
       *
       * A vector pointing towards negative infinity on the Z axis should
       * end up pointing towards negative infinity on the X axis.
       */

      final CaCurveKeyframeOrientation curve_kf_0 =
        CaCurveKeyframeOrientation.of(
          0,
          CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
          CaCurveEasing.CURVE_EASING_IN_OUT,
          QuaternionI4D.makeFromAxisAngle(
            new VectorI3D(0.0, 1.0, 0.0),
            Math.toRadians(0.0)));
      final CaCurveKeyframeOrientation curve_kf_1 =
        CaCurveKeyframeOrientation.of(
          10,
          CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
          CaCurveEasing.CURVE_EASING_IN_OUT,
          QuaternionI4D.makeFromAxisAngle(
            new VectorI3D(0.0, 1.0, 0.0),
            Math.toRadians(90.0)));
      final CaCurveKeyframeOrientation curve_kf_2 =
        CaCurveKeyframeOrientation.of(
          11,
          CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
          CaCurveEasing.CURVE_EASING_IN_OUT,
          QuaternionI4D.makeFromAxisAngle(
            new VectorI3D(0.0, 1.0, 0.0),
            Math.toRadians(90.0)));

      final CaCurveOrientation.Builder curve_b = CaCurveOrientation.builder();
      curve_b.setAction(CaActionName.of("act"));
      curve_b.setJoint(joint_0.name());
      curve_b.putKeyframes(Integer.valueOf(curve_kf_0.index()), curve_kf_0);
      curve_b.putKeyframes(Integer.valueOf(curve_kf_1.index()), curve_kf_1);
      curve_b.putKeyframes(Integer.valueOf(curve_kf_2.index()), curve_kf_2);
      final CaCurveOrientation curve = curve_b.build();

      SortedMap<CaJointName, IndexedSeq<CaCurveType>> curves = TreeMap.empty();
      curves = curves.put(joint_0.name(), Vector.of(curve));

      final CaActionCurves.Builder act_b = CaActionCurves.builder();
      act_b.setName(CaActionName.of("act"));
      act_b.setFramesPerSecond(60);
      act_b.setCurves(curves);

      act = act_b.build();
      actions_by_name = actions_by_name.put(act.name(), act);
    }

    final CaSkeleton.Builder cb = CaSkeleton.builder();
    cb.setName(CaSkeletonName.of("skeleton"));
    cb.setJoints(node_0);
    cb.setActionsByName(actions_by_name);

    final MatrixM4x4D.ContextMM4D context = new MatrixM4x4D.ContextMM4D();

    final CaSkeleton skeleton = cb.build();
    final CaSkeletonRestPoseDType rest_pose =
      CaSkeletonRestPose.createD(context, skeleton);

    final CaEvaluationContextType eval_context =
      CaEvaluationContext.create();
    final CaEvaluatedSkeletonMutableDType eval_skeleton =
      CaEvaluatedSkeletonD.create(eval_context, rest_pose);
    final CaEvaluatorSingleType eval =
      CaEvaluatorSingleD.create(eval_context, eval_skeleton, act, 60);

    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_joints =
      eval_skeleton.joints();
    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_0 =
      eval_joints;
    final CaEvaluatedJointReadableDType eval_joint_0 =
      eval_node_0.value();
    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_1 =
      eval_node_0.childrenReadable().iterator().next();
    final CaEvaluatedJointReadableDType eval_joint_1 =
      eval_node_1.value();
    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> eval_node_2 =
      eval_node_1.childrenReadable().iterator().next();
    final CaEvaluatedJointReadableDType eval_joint_2 =
      eval_node_2.value();

    final VectorM4D output = new VectorM4D();

    eval.evaluateForGlobalFrame(0L, 0L, 1.0);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_0.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(0.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_1.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(0.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(-1.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_2.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(0.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(-2.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    eval.evaluateForGlobalFrame(0L, 10L, 1.0);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_0.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(0.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_1.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(-1.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);

    MatrixM4x4D.multiplyVector4D(
      context,
      eval_joint_2.transformJointObject4x4D(),
      new VectorI4D(0.0, 0.0, 0.0, 1.0),
      output);
    Assert.assertEquals(-2.0, output.getXD(), DELTA);
    Assert.assertEquals(0.0, output.getYD(), DELTA);
    Assert.assertEquals(0.0, output.getZD(), DELTA);
    Assert.assertEquals(1.0, output.getWD(), DELTA);
  }
}
