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

package com.io7m.jcalcium.tests.core.compiled.actions;

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurves;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurvesScaling;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurvesType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeScale;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveScale;
import com.io7m.jcalcium.core.compiled.actions.CaCurveTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import javaslang.collection.IndexedSeq;
import javaslang.collection.Vector;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public final class CaActionCurvesScalingTest
{
  @Test
  public void testScaleSame()
  {
    final CaActionCurves action =
      makeAction(60);

    final CaActionCurvesType action_scaled =
      CaActionCurvesScaling.scale(action, 60);

    Assert.assertEquals(action, action_scaled);
  }

  @Test
  public void testScale()
  {
    final CaActionCurves action =
      makeAction(30);
    final CaActionCurvesType action_scaled =
      CaActionCurvesScaling.scale(action, 60);

    Assert.assertEquals(
      60L, (long) action_scaled.framesPerSecond());
    Assert.assertEquals(
      action.name(), action_scaled.name());
    Assert.assertEquals(
      (long) action.curves().size(),
      (long) action_scaled.curves().size());

    final IndexedSeq<CaCurveType> curves =
      action.curves().get(CaJointName.of("joint0")).get();
    final IndexedSeq<CaCurveType> curves_scaled =
      action_scaled.curves().get(CaJointName.of("joint0")).get();

    final CaCurveTranslation curve_scaled_trans =
      (CaCurveTranslation) curves_scaled.get(0);
    final CaCurveTranslation curve_trans =
      (CaCurveTranslation) curves.get(0);

    Assert.assertEquals(curve_trans.action(), curve_scaled_trans.action());
    Assert.assertEquals(curve_trans.joint(), curve_scaled_trans.joint());

    {
      {
        final CaCurveKeyframeTranslation co_k =
          curve_trans.keyframes().get(Integer.valueOf(0)).get();
        final CaCurveKeyframeTranslation cs_k =
          curve_scaled_trans.keyframes().get(Integer.valueOf(0)).get();

        Assert.assertEquals((long) co_k.index(), (long) cs_k.index());
        Assert.assertEquals(co_k.interpolation(), cs_k.interpolation());
        Assert.assertEquals(co_k.easing(), cs_k.easing());
        Assert.assertEquals(co_k.translation(), cs_k.translation());
      }

      {
        final CaCurveKeyframeTranslation co_k =
          curve_trans.keyframes().get(Integer.valueOf(15)).get();
        final CaCurveKeyframeTranslation cs_k =
          curve_scaled_trans.keyframes().get(Integer.valueOf(30)).get();

        Assert.assertEquals(30L, (long) cs_k.index());
        Assert.assertEquals(co_k.interpolation(), cs_k.interpolation());
        Assert.assertEquals(co_k.easing(), cs_k.easing());
        Assert.assertEquals(co_k.translation(), cs_k.translation());
      }

      {
        final CaCurveKeyframeTranslation co_k =
          curve_trans.keyframes().get(Integer.valueOf(30)).get();
        final CaCurveKeyframeTranslation cs_k =
          curve_scaled_trans.keyframes().get(Integer.valueOf(60)).get();

        Assert.assertEquals(60L, (long) cs_k.index());
        Assert.assertEquals(co_k.interpolation(), cs_k.interpolation());
        Assert.assertEquals(co_k.easing(), cs_k.easing());
        Assert.assertEquals(co_k.translation(), cs_k.translation());
      }
    }

    final CaCurveOrientation curve_scaled_orient =
      (CaCurveOrientation) curves_scaled.get(1);
    final CaCurveOrientation curve_orient =
      (CaCurveOrientation) curves.get(1);

    Assert.assertEquals(curve_orient.action(), curve_scaled_orient.action());
    Assert.assertEquals(curve_orient.joint(), curve_scaled_orient.joint());

    {
      {
        final CaCurveKeyframeOrientation co_k =
          curve_orient.keyframes().get(Integer.valueOf(0)).get();
        final CaCurveKeyframeOrientation cs_k =
          curve_scaled_orient.keyframes().get(Integer.valueOf(0)).get();

        Assert.assertEquals((long) co_k.index(), (long) cs_k.index());
        Assert.assertEquals(co_k.interpolation(), cs_k.interpolation());
        Assert.assertEquals(co_k.easing(), cs_k.easing());
        Assert.assertEquals(co_k.orientation(), cs_k.orientation());
      }

      {
        final CaCurveKeyframeOrientation co_k =
          curve_orient.keyframes().get(Integer.valueOf(15)).get();
        final CaCurveKeyframeOrientation cs_k =
          curve_scaled_orient.keyframes().get(Integer.valueOf(30)).get();

        Assert.assertEquals(30L, (long) cs_k.index());
        Assert.assertEquals(co_k.interpolation(), cs_k.interpolation());
        Assert.assertEquals(co_k.easing(), cs_k.easing());
        Assert.assertEquals(co_k.orientation(), cs_k.orientation());
      }

      {
        final CaCurveKeyframeOrientation co_k =
          curve_orient.keyframes().get(Integer.valueOf(30)).get();
        final CaCurveKeyframeOrientation cs_k =
          curve_scaled_orient.keyframes().get(Integer.valueOf(60)).get();

        Assert.assertEquals(60L, (long) cs_k.index());
        Assert.assertEquals(co_k.interpolation(), cs_k.interpolation());
        Assert.assertEquals(co_k.easing(), cs_k.easing());
        Assert.assertEquals(co_k.orientation(), cs_k.orientation());
      }
    }

    final CaCurveScale curve_scaled_scale =
      (CaCurveScale) curves_scaled.get(2);
    final CaCurveScale curve_scale =
      (CaCurveScale) curves.get(2);

    Assert.assertEquals(curve_scale.action(), curve_scaled_scale.action());
    Assert.assertEquals(curve_scale.joint(), curve_scaled_scale.joint());

    {
      {
        final CaCurveKeyframeScale co_k =
          curve_scale.keyframes().get(Integer.valueOf(0)).get();
        final CaCurveKeyframeScale cs_k =
          curve_scaled_scale.keyframes().get(Integer.valueOf(0)).get();

        Assert.assertEquals((long) co_k.index(), (long) cs_k.index());
        Assert.assertEquals(co_k.interpolation(), cs_k.interpolation());
        Assert.assertEquals(co_k.easing(), cs_k.easing());
        Assert.assertEquals(co_k.scale(), cs_k.scale());
      }

      {
        final CaCurveKeyframeScale co_k =
          curve_scale.keyframes().get(Integer.valueOf(15)).get();
        final CaCurveKeyframeScale cs_k =
          curve_scaled_scale.keyframes().get(Integer.valueOf(30)).get();

        Assert.assertEquals(30L, (long) cs_k.index());
        Assert.assertEquals(co_k.interpolation(), cs_k.interpolation());
        Assert.assertEquals(co_k.easing(), cs_k.easing());
        Assert.assertEquals(co_k.scale(), cs_k.scale());
      }

      {
        final CaCurveKeyframeScale co_k =
          curve_scale.keyframes().get(Integer.valueOf(30)).get();
        final CaCurveKeyframeScale cs_k =
          curve_scaled_scale.keyframes().get(Integer.valueOf(60)).get();

        Assert.assertEquals(60L, (long) cs_k.index());
        Assert.assertEquals(co_k.interpolation(), cs_k.interpolation());
        Assert.assertEquals(co_k.easing(), cs_k.easing());
        Assert.assertEquals(co_k.scale(), cs_k.scale());
      }
    }
  }

  private static CaActionCurves makeAction(
    final int fps)
  {
    final Map<Integer, CaCurveKeyframeTranslation> curve_trans_keyframes =
      new HashMap<>(3);
    curve_trans_keyframes.put(
      Integer.valueOf(0),
      CaCurveKeyframeTranslation.of(
        0,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new PVectorI3D<>(0.0, 0.0, 0.0)));
    curve_trans_keyframes.put(
      Integer.valueOf(fps / 2),
      CaCurveKeyframeTranslation.of(
        fps / 2,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new PVectorI3D<>(1.0, 1.0, 1.0)));
    curve_trans_keyframes.put(
      Integer.valueOf(fps),
      CaCurveKeyframeTranslation.of(
        fps,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new PVectorI3D<>(0.0, 0.0, 0.0)));

    final CaCurveTranslation curve_trans =
      CaCurveTranslation.builder()
        .setAction(CaActionName.of("action"))
        .setJoint(CaJointName.of("joint0"))
        .setJavaMapKeyframes(curve_trans_keyframes)
        .build();

    final Map<Integer, CaCurveKeyframeOrientation> curve_orient_keyframes =
      new HashMap<>(3);
    curve_orient_keyframes.put(
      Integer.valueOf(0),
      CaCurveKeyframeOrientation.of(
        0,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new QuaternionI4D()));
    curve_orient_keyframes.put(
      Integer.valueOf(fps / 2),
      CaCurveKeyframeOrientation.of(
        fps / 2,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new QuaternionI4D(0.707, 0.0, 0.0, 0.707)));
    curve_orient_keyframes.put(
      Integer.valueOf(fps),
      CaCurveKeyframeOrientation.of(
        fps,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new QuaternionI4D()));

    final CaCurveOrientation curve_orient =
      CaCurveOrientation.builder()
        .setAction(CaActionName.of("action"))
        .setJoint(CaJointName.of("joint0"))
        .setJavaMapKeyframes(curve_orient_keyframes)
        .build();

    final Map<Integer, CaCurveKeyframeScale> curve_scale_keyframes =
      new HashMap<>(3);
    curve_scale_keyframes.put(
      Integer.valueOf(0),
      CaCurveKeyframeScale.of(
        0,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new VectorI3D(0.0, 0.0, 0.0)));
    curve_scale_keyframes.put(
      Integer.valueOf(fps / 2),
      CaCurveKeyframeScale.of(
        fps / 2,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new VectorI3D(1.0, 1.0, 1.0)));
    curve_scale_keyframes.put(
      Integer.valueOf(fps),
      CaCurveKeyframeScale.of(
        fps,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new VectorI3D(0.0, 0.0, 0.0)));

    final CaCurveScale curve_scale =
      CaCurveScale.builder()
        .setAction(CaActionName.of("action"))
        .setJoint(CaJointName.of("joint0"))
        .setJavaMapKeyframes(curve_scale_keyframes)
        .build();

    final Map<CaJointName, IndexedSeq<CaCurveType>> curves = new HashMap<>(1);
    curves.put(CaJointName.of("joint0"), Vector.of(
      curve_trans,
      curve_orient,
      curve_scale));

    return CaActionCurves.builder()
      .setFramesPerSecond(fps)
      .setName(CaActionName.of("action"))
      .setJavaMapCurves(curves)
      .build();
  }
}
