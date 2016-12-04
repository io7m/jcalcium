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

package com.io7m.jcalcium.format.protobuf3.v1;

import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.compiled.CaBone;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurvesType;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeScaleType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveOrientationType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveScaleType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveTranslationType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerType;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.collection.IndexedSeq;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import static com.io7m.jfunctional.Unit.unit;

final class CaV1Serializer implements CaCompiledSerializerType
{
  CaV1Serializer()
  {

  }

  private static Skeleton.V1Skeleton fromSkeleton(
    final CaSkeleton skeleton)
  {
    final Skeleton.V1Skeleton.Builder b = Skeleton.V1Skeleton.newBuilder();
    b.setName(skeleton.name().value());
    skeleton.actionsByName().forEach(
      p -> b.putActions(p._1.value(), fromAction(p._2)));
    skeleton.bonesByID().forEach(
      p -> b.putBones(p._1.intValue(), fromBone(p._2)));
    return b.build();
  }

  private static Skeleton.V1Bone fromBone(
    final JOTreeNodeReadableType<CaBone> bone_node)
  {
    final CaBone bone = bone_node.value();
    final Skeleton.V1Bone.Builder b = Skeleton.V1Bone.newBuilder();
    b.setName(bone.name().value());
    b.setId(bone.id());

    final Optional<JOTreeNodeReadableType<CaBone>> parent_opt =
      bone_node.parentReadable();
    if (parent_opt.isPresent()) {
      final JOTreeNodeReadableType<CaBone> parent = parent_opt.get();
      b.setParent(parent.value().id());
    }

    b.setOrientation(fromQuaternion(bone.orientation()));
    b.setScale(fromScale(bone.scale()));
    b.setTranslation(fromTranslation(bone.translation()));
    return b.build();
  }

  private static Skeleton.V1Action fromAction(
    final CaActionType action)
  {
    return action.matchAction(unit(), (bone_name, curve) -> {
      final Skeleton.V1Action.Builder b = Skeleton.V1Action.newBuilder();
      b.setCurves(fromActionCurves(curve));
      return b.build();
    });
  }

  private static Skeleton.V1ActionCurves fromActionCurves(
    final CaActionCurvesType ac)
  {
    final Skeleton.V1ActionCurves.Builder b = Skeleton.V1ActionCurves.newBuilder();
    b.setName(ac.name().value());
    b.setFramesPerSecond(ac.framesPerSecond());
    ac.curves().forEach(p -> b.putCurves(p._1.value(), fromCurves(p._2)));
    return b.build();
  }

  private static Skeleton.V1CurveList fromCurves(
    final IndexedSeq<CaCurveType> curves)
  {
    final Skeleton.V1CurveList.Builder b = Skeleton.V1CurveList.newBuilder();
    curves.forEach(curve -> b.addCurves(fromCurve(curve)));
    return b.build();
  }

  private static Skeleton.V1Curve fromCurve(
    final CaCurveType curve)
  {
    final Skeleton.V1Curve.Builder b = Skeleton.V1Curve.newBuilder();
    return curve.matchCurve(
      unit(),
      (name, c) -> {
        b.setTranslation(fromCurveTranslation(c));
        return b.build();
      },
      (name, c) -> {
        b.setOrientation(fromCurveOrientation(c));
        return b.build();
      },
      (name, c) -> {
        b.setScale(fromCurveScale(c));
        return b.build();
      });
  }

  private static Skeleton.V1CurveScale fromCurveScale(
    final CaCurveScaleType c)
  {
    final Skeleton.V1CurveScale.Builder b = Skeleton.V1CurveScale.newBuilder();
    b.setBone(c.bone().value());
    c.keyframes().forEach(
      p -> b.putKeyframes(p._1.intValue(), fromCurveKeyframeScale(p._2)));
    return b.build();
  }

  private static Skeleton.V1CurveKeyframeScale fromCurveKeyframeScale(
    final CaCurveKeyframeScaleType k)
  {
    final Skeleton.V1CurveKeyframeScale.Builder b =
      Skeleton.V1CurveKeyframeScale.newBuilder();
    b.setEasing(fromEasing(k.easing()));
    b.setInterpolation(fromInterpolation(k.interpolation()));
    b.setIndex(k.index());
    b.setScale(fromScale(k.scale()));
    return b.build();
  }

  private static Skeleton.V1CurveKeyframeTranslation fromCurveKeyframeTranslation(
    final CaCurveKeyframeTranslation k)
  {
    final Skeleton.V1CurveKeyframeTranslation.Builder b =
      Skeleton.V1CurveKeyframeTranslation.newBuilder();
    b.setEasing(fromEasing(k.easing()));
    b.setInterpolation(fromInterpolation(k.interpolation()));
    b.setIndex(k.index());
    b.setTranslation(fromTranslation(k.translation()));
    return b.build();
  }

  private static Skeleton.V1CurveTranslation fromCurveTranslation(
    final CaCurveTranslationType c)
  {
    final Skeleton.V1CurveTranslation.Builder b =
      Skeleton.V1CurveTranslation.newBuilder();
    b.setBone(c.bone().value());
    c.keyframes().forEach(
      p -> b.putKeyframes(p._1.intValue(), fromCurveKeyframeTranslation(p._2)));
    return b.build();
  }

  private static Skeleton.V1CurveOrientation fromCurveOrientation(
    final CaCurveOrientationType c)
  {
    final Skeleton.V1CurveOrientation.Builder b =
      Skeleton.V1CurveOrientation.newBuilder();
    b.setBone(c.bone().value());
    c.keyframes().forEach(
      p -> b.putKeyframes(p._1.intValue(), fromCurveKeyframeOrientation(p._2)));
    return b.build();
  }

  private static Skeleton.V1CurveKeyframeOrientation fromCurveKeyframeOrientation(
    final CaCurveKeyframeOrientation k)
  {
    final Skeleton.V1CurveKeyframeOrientation.Builder b =
      Skeleton.V1CurveKeyframeOrientation.newBuilder();
    b.setEasing(fromEasing(k.easing()));
    b.setInterpolation(fromInterpolation(k.interpolation()));
    b.setIndex(k.index());
    b.setOrientation(fromQuaternion(k.orientation()));
    return b.build();
  }

  private static Skeleton.V1Scale fromScale(
    final VectorI3D scale)
  {
    final Skeleton.V1Scale.Builder b = Skeleton.V1Scale.newBuilder();
    b.setX(scale.getXD());
    b.setY(scale.getYD());
    b.setZ(scale.getZD());
    return b.build();
  }

  private static Skeleton.V1Translation fromTranslation(
    final PVectorI3D<CaSpaceBoneParentRelativeType> translation)
  {
    final Skeleton.V1Translation.Builder b = Skeleton.V1Translation.newBuilder();
    b.setX(translation.getXD());
    b.setY(translation.getYD());
    b.setZ(translation.getZD());
    return b.build();
  }

  private static Skeleton.V1Quaternion fromQuaternion(
    final QuaternionI4D orientation)
  {
    final Skeleton.V1Quaternion.Builder b = Skeleton.V1Quaternion.newBuilder();
    b.setX(orientation.getXD());
    b.setY(orientation.getYD());
    b.setZ(orientation.getZD());
    b.setW(orientation.getWD());
    return b.build();
  }

  private static Skeleton.V1Interpolation fromInterpolation(
    final CaCurveInterpolation interpolation)
  {
    switch (interpolation) {
      case CURVE_INTERPOLATION_CONSTANT:
        return Skeleton.V1Interpolation.V1_INTERPOLATION_CONSTANT;
      case CURVE_INTERPOLATION_LINEAR:
        return Skeleton.V1Interpolation.V1_INTERPOLATION_LINEAR;
      case CURVE_INTERPOLATION_QUADRATIC:
        return Skeleton.V1Interpolation.V1_INTERPOLATION_QUADRATIC;
      case CURVE_INTERPOLATION_EXPONENTIAL:
        return Skeleton.V1Interpolation.V1_INTERPOLATION_EXPONENTIAL;
    }

    throw new UnreachableCodeException();
  }

  private static Skeleton.V1Easing fromEasing(
    final CaCurveEasing easing)
  {
    switch (easing) {
      case CURVE_EASING_IN:
        return Skeleton.V1Easing.V1_EASING_IN;
      case CURVE_EASING_OUT:
        return Skeleton.V1Easing.V1_EASING_OUT;
      case CURVE_EASING_IN_OUT:
        return Skeleton.V1Easing.V1_EASING_IN_OUT;
    }

    throw new UnreachableCodeException();
  }

  @Override
  public void serializeCompiledSkeletonToStream(
    final CaSkeleton skeleton,
    final OutputStream out)
    throws IOException
  {
    final Skeleton.V1Skeleton skel = fromSkeleton(skeleton);
    out.write(skel.toByteArray());
  }
}
