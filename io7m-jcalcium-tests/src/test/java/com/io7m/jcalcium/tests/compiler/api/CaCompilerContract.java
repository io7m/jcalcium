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
import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientationType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeScaleType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslationType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveOrientationType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveScaleType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveTranslationType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveType;
import com.io7m.jcalcium.core.definitions.CaDefinitionJoint;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionCurves;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeOrientation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeOrientationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeScale;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeScaleType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeTranslation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeTranslationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveOrientation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveScale;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveTranslation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveType;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jcalcium.generators.CaDefinitionSkeletonGenerator;
import com.io7m.jcalcium.generators.JointNameTree;
import com.io7m.jcalcium.generators.JointNameTreeGenerator;
import com.io7m.jcalcium.generators.JointTree;
import com.io7m.jcalcium.generators.JointTreeGenerator;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.generators.QuaternionI4DGenerator;
import com.io7m.jtensors.generators.VectorI3DGenerator;
import com.io7m.jtensors.generators.parameterized.PVectorI3DGenerator;
import com.io7m.jtensors.parameterized.PVectorI3D;
import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.IndexedSeq;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.SortedMap;
import javaslang.control.Validation;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static com.io7m.jfunctional.Unit.unit;

public abstract class CaCompilerContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaCompilerContract.class);
  }

  private static void dump(
    final Validation<List<CaCompileError>, CaSkeleton> r)
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
    b.setJoints(HashMap.empty());

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertFalse(r.isValid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_JOINT_NO_ROOT,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileMultipleRoots()
  {
    final CaCompilerType cc = this.create();

    final CaJointName bone_0_name = CaJointName.of("root0");
    final CaDefinitionJoint bone_0 = CaDefinitionJoint.of(
      bone_0_name,
      Optional.empty(),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));
    final CaJointName bone_1_name = CaJointName.of("root1");
    final CaDefinitionJoint bone_1 = CaDefinitionJoint.of(
      bone_1_name,
      Optional.empty(),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setJoints(HashMap.ofEntries(
      Tuple.of(bone_0_name, bone_0),
      Tuple.of(bone_1_name, bone_1)));

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertFalse(r.isValid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_MULTIPLE_ROOT_JOINTS,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileNonexistentParent()
  {
    final CaCompilerType cc = this.create();

    final CaJointName bone_0_name = CaJointName.of("root0");
    final CaDefinitionJoint bone_0 = CaDefinitionJoint.of(
      bone_0_name,
      Optional.empty(),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));
    final CaJointName bone_1_name = CaJointName.of("root1");
    final CaJointName bone_2_name = CaJointName.of("root2");
    final CaDefinitionJoint bone_1 = CaDefinitionJoint.of(
      bone_1_name,
      Optional.of(bone_2_name),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setJoints(HashMap.ofEntries(
      Tuple.of(bone_0_name, bone_0),
      Tuple.of(bone_1_name, bone_1)));

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertFalse(r.isValid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_JOINT_NONEXISTENT_PARENT,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileCyclic()
  {
    final CaCompilerType cc = this.create();

    final CaJointName bone_0_name = CaJointName.of("root0");
    final CaDefinitionJoint bone_0 = CaDefinitionJoint.of(
      bone_0_name,
      Optional.empty(),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaJointName bone_1_name = CaJointName.of("root1");
    final CaJointName bone_2_name = CaJointName.of("root2");
    final CaDefinitionJoint bone_1 = CaDefinitionJoint.of(
      bone_1_name,
      Optional.of(bone_2_name),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));
    final CaDefinitionJoint bone_2 = CaDefinitionJoint.of(
      bone_2_name,
      Optional.of(bone_1_name),
      new PVectorI3D<>(),
      new QuaternionI4D(),
      new VectorI3D(1.0, 1.0, 1.0));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setJoints(HashMap.ofEntries(
      Tuple.of(bone_0_name, bone_0),
      Tuple.of(bone_1_name, bone_1),
      Tuple.of(bone_2_name, bone_2)));

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertFalse(r.isValid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_JOINT_CYCLE,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileActionNonexistentJoint()
  {
    final CaCompilerType cc = this.create();

    Map<CaJointName, List<CaDefinitionCurveType>> act_curves = HashMap.empty();
    act_curves = act_curves.put(CaJointName.of("nonexistent"), List.empty());

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(act_curves);

    Map<CaActionName, CaDefinitionActionType> actions = HashMap.empty();
    actions = actions.put(CaActionName.of("act0"), act_b.build());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(actions);
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_ACTION_INVALID_BONE,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileActionInvalidFPS()
  {
    final CaCompilerType cc = this.create();

    Map<CaJointName, List<CaDefinitionCurveType>> act_curves = HashMap.empty();
    act_curves = act_curves.put(CaJointName.of("bone0"), List.empty());

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(-1);
    act_b.setCurves(act_curves);

    Map<CaActionName, CaDefinitionActionType> actions = HashMap.empty();
    actions = actions.put(CaActionName.of("act0"), act_b.build());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(actions);
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_ACTION_INVALID_FPS,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileActionDuplicateKeyframesTranslation()
  {
    final CaCompilerType cc = this.create();

    final CaDefinitionCurveKeyframeTranslation.Builder kf_b =
      CaDefinitionCurveKeyframeTranslation.builder();
    kf_b.setIndex(0);
    kf_b.setInterpolation(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
    kf_b.setEasing(CaCurveEasing.CURVE_EASING_IN_OUT);
    kf_b.setTranslation(new PVectorI3D<>());

    List<CaDefinitionCurveKeyframeTranslationType> curve_keyframes = List.empty();
    curve_keyframes = curve_keyframes.append(kf_b.build());
    curve_keyframes = curve_keyframes.append(kf_b.build());

    final CaDefinitionCurveTranslation.Builder curve_b =
      CaDefinitionCurveTranslation.builder();
    curve_b.setJoint(CaJointName.of("bone0"));
    curve_b.setKeyframes(curve_keyframes);

    List<CaDefinitionCurveType> curves = List.empty();
    curves = curves.append(curve_b.build());

    Map<CaJointName, List<CaDefinitionCurveType>> act_curves = HashMap.empty();
    act_curves = act_curves.put(CaJointName.of("bone0"), curves);

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(act_curves);

    Map<CaActionName, CaDefinitionActionType> actions = HashMap.empty();
    actions = actions.put(CaActionName.of("act0"), act_b.build());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(actions);
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_ACTION_DUPLICATE_KEYFRAME,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileActionDuplicateKeyframesOrientation()
  {
    final CaCompilerType cc = this.create();

    final CaDefinitionCurveKeyframeOrientation.Builder kf_b =
      CaDefinitionCurveKeyframeOrientation.builder();
    kf_b.setIndex(0);
    kf_b.setInterpolation(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
    kf_b.setEasing(CaCurveEasing.CURVE_EASING_IN_OUT);
    kf_b.setOrientation(new QuaternionI4D());

    List<CaDefinitionCurveKeyframeOrientationType> curve_keyframes = List.empty();
    curve_keyframes = curve_keyframes.append(kf_b.build());
    curve_keyframes = curve_keyframes.append(kf_b.build());

    final CaDefinitionCurveOrientation.Builder curve_b =
      CaDefinitionCurveOrientation.builder();
    curve_b.setJoint(CaJointName.of("bone0"));
    curve_b.setKeyframes(curve_keyframes);

    List<CaDefinitionCurveType> curves = List.empty();
    curves = curves.append(curve_b.build());

    Map<CaJointName, List<CaDefinitionCurveType>> act_curves = HashMap.empty();
    act_curves = act_curves.put(CaJointName.of("bone0"), curves);

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(act_curves);

    Map<CaActionName, CaDefinitionActionType> actions = HashMap.empty();
    actions = actions.put(CaActionName.of("act0"), act_b.build());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(actions);
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_ACTION_DUPLICATE_KEYFRAME,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileActionDuplicateKeyframesScale()
  {
    final CaCompilerType cc = this.create();

    final CaDefinitionCurveKeyframeScale.Builder kf_b =
      CaDefinitionCurveKeyframeScale.builder();
    kf_b.setIndex(0);
    kf_b.setInterpolation(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
    kf_b.setEasing(CaCurveEasing.CURVE_EASING_IN_OUT);
    kf_b.setScale(new VectorI3D(1.0, 1.0, 1.0));

    List<CaDefinitionCurveKeyframeScaleType> curve_keyframes = List.empty();
    curve_keyframes = curve_keyframes.append(kf_b.build());
    curve_keyframes = curve_keyframes.append(kf_b.build());

    final CaDefinitionCurveScale.Builder curve_b =
      CaDefinitionCurveScale.builder();
    curve_b.setJoint(CaJointName.of("bone0"));
    curve_b.setKeyframes(curve_keyframes);

    List<CaDefinitionCurveType> curves = List.empty();
    curves = curves.append(curve_b.build());

    Map<CaJointName, List<CaDefinitionCurveType>> act_curves = HashMap.empty();
    act_curves = act_curves.put(CaJointName.of("bone0"), curves);

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(act_curves);

    Map<CaActionName, CaDefinitionActionType> actions = HashMap.empty();
    actions = actions.put(CaActionName.of("act0"), act_b.build());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(actions);
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_ACTION_DUPLICATE_KEYFRAME,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileActionMultipleCurvesSameTypeTranslation()
  {
    final CaCompilerType cc = this.create();

    List<CaDefinitionCurveType> curves = List.empty();

    {
      final CaDefinitionCurveKeyframeTranslation.Builder kf_b =
        CaDefinitionCurveKeyframeTranslation.builder();
      kf_b.setIndex(0);
      kf_b.setInterpolation(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
      kf_b.setEasing(CaCurveEasing.CURVE_EASING_IN_OUT);
      kf_b.setTranslation(new PVectorI3D<>());

      List<CaDefinitionCurveKeyframeTranslationType> curve_keyframes = List.empty();
      curve_keyframes = curve_keyframes.append(kf_b.build());

      final CaDefinitionCurveTranslation.Builder curve_b =
        CaDefinitionCurveTranslation.builder();
      curve_b.setJoint(CaJointName.of("bone0"));
      curve_b.setKeyframes(curve_keyframes);
      curves = curves.append(curve_b.build());
    }

    {
      final CaDefinitionCurveKeyframeTranslation.Builder kf_b =
        CaDefinitionCurveKeyframeTranslation.builder();
      kf_b.setIndex(0);
      kf_b.setInterpolation(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
      kf_b.setEasing(CaCurveEasing.CURVE_EASING_IN_OUT);
      kf_b.setTranslation(new PVectorI3D<>());

      List<CaDefinitionCurveKeyframeTranslationType> curve_keyframes = List.empty();
      curve_keyframes = curve_keyframes.append(kf_b.build());

      final CaDefinitionCurveTranslation.Builder curve_b =
        CaDefinitionCurveTranslation.builder();
      curve_b.setJoint(CaJointName.of("bone0"));
      curve_b.setKeyframes(curve_keyframes);
      curves = curves.append(curve_b.build());
    }

    Map<CaJointName, List<CaDefinitionCurveType>> act_curves = HashMap.empty();
    act_curves = act_curves.put(CaJointName.of("bone0"), curves);

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(act_curves);

    Map<CaActionName, CaDefinitionActionType> actions = HashMap.empty();
    actions = actions.put(CaActionName.of("act0"), act_b.build());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(actions);
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileActionMultipleCurvesSameTypeOrientation()
  {
    final CaCompilerType cc = this.create();

    List<CaDefinitionCurveType> curves = List.empty();

    {
      final CaDefinitionCurveKeyframeOrientation.Builder kf_b =
        CaDefinitionCurveKeyframeOrientation.builder();
      kf_b.setIndex(0);
      kf_b.setInterpolation(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
      kf_b.setEasing(CaCurveEasing.CURVE_EASING_IN_OUT);
      kf_b.setOrientation(new QuaternionI4D());

      List<CaDefinitionCurveKeyframeOrientationType> curve_keyframes = List.empty();
      curve_keyframes = curve_keyframes.append(kf_b.build());

      final CaDefinitionCurveOrientation.Builder curve_b =
        CaDefinitionCurveOrientation.builder();
      curve_b.setJoint(CaJointName.of("bone0"));
      curve_b.setKeyframes(curve_keyframes);
      curves = curves.append(curve_b.build());
    }

    {
      final CaDefinitionCurveKeyframeOrientation.Builder kf_b =
        CaDefinitionCurveKeyframeOrientation.builder();
      kf_b.setIndex(0);
      kf_b.setInterpolation(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
      kf_b.setEasing(CaCurveEasing.CURVE_EASING_IN_OUT);
      kf_b.setOrientation(new QuaternionI4D());

      List<CaDefinitionCurveKeyframeOrientationType> curve_keyframes = List.empty();
      curve_keyframes = curve_keyframes.append(kf_b.build());

      final CaDefinitionCurveOrientation.Builder curve_b =
        CaDefinitionCurveOrientation.builder();
      curve_b.setJoint(CaJointName.of("bone0"));
      curve_b.setKeyframes(curve_keyframes);
      curves = curves.append(curve_b.build());
    }

    Map<CaJointName, List<CaDefinitionCurveType>> act_curves = HashMap.empty();
    act_curves = act_curves.put(CaJointName.of("bone0"), curves);

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(act_curves);

    Map<CaActionName, CaDefinitionActionType> actions = HashMap.empty();
    actions = actions.put(CaActionName.of("act0"), act_b.build());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(actions);
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileActionMultipleCurvesSameTypeScale()
  {
    final CaCompilerType cc = this.create();

    List<CaDefinitionCurveType> curves = List.empty();

    {
      final CaDefinitionCurveKeyframeScale.Builder kf_b =
        CaDefinitionCurveKeyframeScale.builder();
      kf_b.setIndex(0);
      kf_b.setInterpolation(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
      kf_b.setEasing(CaCurveEasing.CURVE_EASING_IN_OUT);
      kf_b.setScale(new VectorI3D(1.0, 1.0, 1.0));

      List<CaDefinitionCurveKeyframeScaleType> curve_keyframes = List.empty();
      curve_keyframes = curve_keyframes.append(kf_b.build());

      final CaDefinitionCurveScale.Builder curve_b =
        CaDefinitionCurveScale.builder();
      curve_b.setJoint(CaJointName.of("bone0"));
      curve_b.setKeyframes(curve_keyframes);
      curves = curves.append(curve_b.build());
    }

    {
      final CaDefinitionCurveKeyframeScale.Builder kf_b =
        CaDefinitionCurveKeyframeScale.builder();
      kf_b.setIndex(0);
      kf_b.setInterpolation(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
      kf_b.setEasing(CaCurveEasing.CURVE_EASING_IN_OUT);
      kf_b.setScale(new VectorI3D(1.0, 1.0, 1.0));

      List<CaDefinitionCurveKeyframeScaleType> curve_keyframes = List.empty();
      curve_keyframes = curve_keyframes.append(kf_b.build());

      final CaDefinitionCurveScale.Builder curve_b =
        CaDefinitionCurveScale.builder();
      curve_b.setJoint(CaJointName.of("bone0"));
      curve_b.setKeyframes(curve_keyframes);
      curves = curves.append(curve_b.build());
    }

    Map<CaJointName, List<CaDefinitionCurveType>> act_curves = HashMap.empty();
    act_curves = act_curves.put(CaJointName.of("bone0"), curves);

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(act_curves);

    Map<CaActionName, CaDefinitionActionType> actions = HashMap.empty();
    actions = actions.put(CaActionName.of("act0"), act_b.build());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(actions);
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileRootOrientationNotIdentity()
  {
    final CaCompilerType cc = this.create();

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(HashMap.empty());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(2.0, 3.0, 4.0, 5.0),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_JOINT_ROOT_NOT_IDENTITY_TRANSFORM,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileRootScaleNotIdentity()
  {
    final CaCompilerType cc = this.create();

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(HashMap.empty());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(2.0, 2.0, 2.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_JOINT_ROOT_NOT_IDENTITY_TRANSFORM,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileRootTranslateNotIdentity()
  {
    final CaCompilerType cc = this.create();

    final CaDefinitionActionCurves.Builder act_b =
      CaDefinitionActionCurves.builder();
    act_b.setName(CaActionName.of("act0"));
    act_b.setFramesPerSecond(60);
    act_b.setCurves(HashMap.empty());

    Map<CaJointName, CaDefinitionJoint> bones = HashMap.empty();
    bones = bones.put(
      CaJointName.of("bone0"),
      CaDefinitionJoint.of(
        CaJointName.of("bone0"),
        Optional.empty(),
        new PVectorI3D<>(2.0, 2.0, 3.0),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0)));

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isInvalid());
    Assert.assertEquals(
      CaCompileErrorCode.ERROR_JOINT_ROOT_NOT_IDENTITY_TRANSFORM,
      r.getError().get(0).code());
  }

  @Test
  public void testCompileCorrectJoints()
  {
    final CaCompilerType cc = this.create();

    final VectorI3DGenerator vgen =
      new VectorI3DGenerator();
    final PVectorI3DGenerator<CaSpaceJointType> pvgen =
      new PVectorI3DGenerator<>();
    final QuaternionI4DGenerator qgen =
      new QuaternionI4DGenerator();

    final JointNameTreeGenerator gen = new JointNameTreeGenerator();
    final JointNameTree tree = gen.next();

    final StringBuilder sb = new StringBuilder(100);
    tree.tree().forEachDepthFirst(unit(), (input, depth, node) -> {
      sb.setLength(0);
      for (int index = 0; index < depth; ++index) {
        sb.append("  ");
      }
      sb.append(node.value().value());
      LOG.debug("tree: {}", sb.toString());
    });

    final Map<CaJointName, CaDefinitionJoint> bones =
      tree.nodes().map((CaJointName bone_name, JOTreeNodeReadableType<CaJointName> node) -> {
        final CaDefinitionJoint.Builder cb = CaDefinitionJoint.builder();
        cb.setName(node.value());
        cb.setScale(vgen.next());
        cb.setOrientation(qgen.next());
        cb.setTranslation(pvgen.next());

        final Optional<JOTreeNodeReadableType<CaJointName>> parent_opt =
          node.parentReadable();
        if (parent_opt.isPresent()) {
          cb.setParent(parent_opt.get().value());
        } else {
          cb.setScale(new VectorI3D(1.0, 1.0, 1.0));
          cb.setOrientation(new QuaternionI4D());
          cb.setTranslation(new PVectorI3D<>());
        }

        return Tuple.of(bone_name, cb.build());
      });

    final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
    b.setName(CaSkeletonName.of("skeleton"));
    b.setActions(HashMap.empty());
    b.setJoints(bones);

    final CaDefinitionSkeleton s = b.build();
    final Validation<List<CaCompileError>, CaSkeleton> r =
      cc.compile(s);

    dump(r);
    Assert.assertTrue(r.isValid());

    final CaSkeleton compiled = r.get();

    final Map<CaJointName, JOTreeNodeReadableType<CaJoint>> by_name =
      compiled.jointsByName();
    final Map<Integer, JOTreeNodeReadableType<CaJoint>> by_id =
      compiled.jointsByID();

    Assert.assertEquals((long) by_id.size(), (long) by_name.size());

    final Collection<Integer> ids_unique = new HashSet<>();

    for (final CaJointName bone_name : bones.keySet()) {
      Assert.assertTrue(by_name.containsKey(bone_name));

      final JOTreeNodeReadableType<CaJoint> compiled_node =
        by_name.get(bone_name).get();
      final CaJoint compiled_bone =
        compiled_node.value();

      final Integer id = Integer.valueOf(compiled_bone.id());
      Assert.assertFalse(ids_unique.contains(id));
      ids_unique.add(id);

      final JOTreeNodeReadableType<CaJointName> original_node =
        tree.nodes().get(bone_name).get();
      final CaDefinitionJoint original_bone =
        bones.get(bone_name).get();

      Assert.assertEquals(
        Boolean.valueOf(compiled_node.parentReadable().isPresent()),
        Boolean.valueOf(original_bone.parent().isPresent()));

      if (compiled_node.parentReadable().isPresent()) {
        final JOTreeNodeReadableType<CaJoint> c_parent =
          compiled_node.parentReadable().get();
        final CaJointName b_parent =
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

  @Test
  public void testCompileCorrectAll()
  {
    final CaCompilerType cc = this.create();
    final JointTreeGenerator gen = new JointTreeGenerator();
    final JointTree tree = gen.next();

    QuickCheck.forAll(
      25,
      new CaDefinitionSkeletonGenerator(tree),
      new AbstractCharacteristic<CaDefinitionSkeleton>()
      {
        @Override
        protected void doSpecify(final CaDefinitionSkeleton original)
          throws Throwable
        {
          final Validation<List<CaCompileError>, CaSkeleton> r =
            cc.compile(original);
          dump(r);
          Assert.assertTrue(r.isValid());

          final CaSkeleton compiled = r.get();
          Assert.assertEquals(
            (long) original.joints().size(),
            (long) compiled.jointsByName().size());
          Assert.assertEquals(
            (long) original.joints().size(),
            (long) compiled.jointsByID().size());

          for (final CaJointName bone_name : original.joints().keySet()) {
            Assert.assertTrue(compiled.jointsByName().containsKey(bone_name));

            final CaDefinitionJoint bone_orig =
              original.joints().get(bone_name).get();
            final CaJoint bone_comp =
              compiled.jointsByName().get(bone_name).get().value();

            Assert.assertEquals(
              bone_orig.name(), bone_comp.name());
            Assert.assertEquals(
              bone_orig.orientation(), bone_comp.orientation());
            Assert.assertEquals(
              bone_orig.scale(), bone_comp.scale());
            Assert.assertEquals(
              bone_orig.translation(), bone_comp.translation());
          }

          Assert.assertEquals(
            (long) original.actions().size(),
            (long) compiled.actionsByName().size());

          for (final CaActionName action_name : original.actions().keySet()) {
            Assert.assertTrue(
              compiled.actionsByName().containsKey(action_name));

            final CaDefinitionActionType act_orig =
              original.actions().get(action_name).get();
            final CaActionType act_comp =
              compiled.actionsByName().get(action_name).get();

            Assert.assertEquals(act_orig.name(), act_comp.name());
            Assert.assertEquals(
              (long) act_orig.framesPerSecond(),
              (long) act_comp.framesPerSecond());

            act_comp.matchAction(unit(), (ignored, act_comp_c) -> {
              Assert.assertEquals(
                CaDefinitionActionCurves.class, act_orig.getClass());
              final CaDefinitionActionCurves act_orig_c =
                (CaDefinitionActionCurves) act_orig;

              final SortedMap<CaJointName, IndexedSeq<CaCurveType>> curves_by_bone_comp =
                act_comp_c.curves();
              final Map<CaJointName, List<CaDefinitionCurveType>> curves_by_bone_orig =
                act_orig_c.curves();

              Assert.assertEquals(
                (long) curves_by_bone_orig.size(),
                (long) curves_by_bone_comp.size());

              for (final CaJointName curve_bone : curves_by_bone_comp.keySet()) {
                final List<CaDefinitionCurveType> curves_orig =
                  curves_by_bone_orig.get(curve_bone).get();
                final IndexedSeq<CaCurveType> curves_comp =
                  curves_by_bone_comp.get(curve_bone).get();

                for (int index = 0; index < curves_comp.size(); ++index) {
                  final CaCurveType curve_comp =
                    curves_comp.get(index);
                  final CaDefinitionCurveType curve_orig =
                    curves_orig.get(index);

                  Assert.assertEquals(curve_orig.joint(), curve_bone);
                  Assert.assertEquals(curve_orig.joint(), curve_comp.joint());

                  curve_orig.matchCurve(
                    unit(),
                    (ignored1, orig_translation) -> {
                      final CaCurveTranslationType comp_translation =
                        (CaCurveTranslationType) curve_comp;

                      Assert.assertEquals(
                        (long) orig_translation.keyframes().size(),
                        (long) comp_translation.keyframes().size());

                      for (final CaDefinitionCurveKeyframeTranslationType kf :
                        orig_translation.keyframes()) {

                        final Integer kf_index = Integer.valueOf(kf.index());
                        Assert.assertTrue(comp_translation.keyframes().containsKey(
                          kf_index));

                        final CaCurveKeyframeTranslationType comp_kf =
                          comp_translation.keyframes().get(kf_index).get();

                        Assert.assertEquals(
                          kf.translation(), comp_kf.translation());
                        Assert.assertEquals(
                          kf.easing(), comp_kf.easing());
                        Assert.assertEquals(
                          kf.interpolation(), comp_kf.interpolation());
                        Assert.assertEquals(
                          (long) kf.index(), (long) comp_kf.index());
                      }

                      return unit();
                    },

                    (ignored1, orig_orientation) -> {
                      final CaCurveOrientationType comp_orientation =
                        (CaCurveOrientationType) curve_comp;

                      Assert.assertEquals(
                        (long) orig_orientation.keyframes().size(),
                        (long) comp_orientation.keyframes().size());

                      for (final CaDefinitionCurveKeyframeOrientationType kf :
                        orig_orientation.keyframes()) {

                        final Integer kf_index = Integer.valueOf(kf.index());
                        Assert.assertTrue(comp_orientation.keyframes().containsKey(
                          kf_index));

                        final CaCurveKeyframeOrientationType comp_kf =
                          comp_orientation.keyframes().get(kf_index).get();

                        Assert.assertEquals(
                          kf.orientation(), comp_kf.orientation());
                        Assert.assertEquals(
                          kf.easing(), comp_kf.easing());
                        Assert.assertEquals(
                          kf.interpolation(), comp_kf.interpolation());
                        Assert.assertEquals(
                          (long) kf.index(), (long) comp_kf.index());
                      }

                      return unit();
                    },

                    (ignored1, orig_scale) -> {
                      final CaCurveScaleType comp_scale =
                        (CaCurveScaleType) curve_comp;

                      Assert.assertEquals(
                        (long) orig_scale.keyframes().size(),
                        (long) comp_scale.keyframes().size());

                      for (final CaDefinitionCurveKeyframeScaleType kf :
                        orig_scale.keyframes()) {

                        final Integer kf_index = Integer.valueOf(kf.index());
                        Assert.assertTrue(comp_scale.keyframes().containsKey(
                          kf_index));

                        final CaCurveKeyframeScaleType comp_kf =
                          comp_scale.keyframes().get(kf_index).get();

                        Assert.assertEquals(
                          kf.scale(), comp_kf.scale());
                        Assert.assertEquals(
                          kf.easing(), comp_kf.easing());
                        Assert.assertEquals(
                          kf.interpolation(), comp_kf.interpolation());
                        Assert.assertEquals(
                          (long) kf.index(), (long) comp_kf.index());
                      }

                      return unit();
                    });
                }
              }

              return unit();
            });
          }
        }
      });
  }
}
