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

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.definitions.CaDefinitionBone;
import com.io7m.jcalcium.core.definitions.CaDefinitionBoneType;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeletonType;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionCurves;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionCurvesType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeOrientation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeOrientationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeScale;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeScaleType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeTranslation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeTranslationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveOrientation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveOrientationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveScale;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveScaleType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveTranslation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveTranslationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jcalcium.serializer.api.CaDefinitionSerializerType;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Seq;
import javaslang.collection.SortedSet;
import javaslang.collection.Traversable;
import javaslang.collection.TreeSet;
import javaslang.control.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static com.io7m.jfunctional.Unit.unit;
import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.valid;

/**
 * An implementation of a protobuf3 parser for version 1 types.
 */

public final class CaV1Protobuf3Format implements CaDefinitionParserType,
  CaDefinitionSerializerType
{
  /**
   * Construct a parser
   */

  public CaV1Protobuf3Format()
  {

  }

  private static <E, T> Validation<List<E>, T> flatten(
    final Validation<List<List<E>>, T> v)
  {
    return v.leftMap(xs -> xs.fold(List.empty(), List::appendAll));
  }

  /**
   * @return The parserSupportedVersions supported by this parser
   */

  public static SortedSet<CaFormatVersion> supported()
  {
    return TreeSet.of(CaFormatVersion.of(1, 0));
  }

  private static Skeleton.V1Skeleton fromCoreSkeleton(
    final CaDefinitionSkeletonType skeleton)
  {
    final Skeleton.V1Skeleton.Builder b = Skeleton.V1Skeleton.newBuilder();
    b.setName(skeleton.name().value());
    skeleton.actions().forEach(p -> b.addActions(fromCoreAction(p._2)));
    skeleton.bones().forEach(p -> b.addBones(fromCoreBone(p._2)));
    return b.build();
  }

  private static Skeleton.V1Bone fromCoreBone(final CaDefinitionBoneType bone)
  {
    final Skeleton.V1Bone.Builder b = Skeleton.V1Bone.newBuilder();
    b.setName(bone.name().value());
    bone.parent().ifPresent(p -> b.setParent(p.value()));
    b.setOrientation(fromCoreQuaternion(bone.orientation()));
    b.setScale(fromCoreScale(bone.scale()));
    b.setTranslation(fromCoreTranslation(bone.translation()));
    return b.build();
  }

  private static Skeleton.V1Action fromCoreAction(
    final CaDefinitionActionType action)
  {
    return action.matchAction(unit(), (bone_name, curve) -> {
      final Skeleton.V1Action.Builder b = Skeleton.V1Action.newBuilder();
      b.setCurves(fromCoreActionCurves(curve));
      return b.build();
    });
  }

  private static Skeleton.V1ActionCurves fromCoreActionCurves(
    final CaDefinitionActionCurvesType ac)
  {
    final Skeleton.V1ActionCurves.Builder b = Skeleton.V1ActionCurves.newBuilder();
    b.setName(ac.name().value());
    b.setFramesPerSecond(ac.framesPerSecond());
    ac.curves().forEach(p -> p._2.forEach(curve -> b.addCurves(fromCoreCurve(
      curve))));
    return b.build();
  }

  private static Skeleton.V1Curve fromCoreCurve(
    final CaDefinitionCurveType curve)
  {
    final Skeleton.V1Curve.Builder b = Skeleton.V1Curve.newBuilder();
    return curve.matchCurve(
      unit(),
      (name, c) -> {
        b.setTranslation(fromCoreCurveTranslation(c));
        return b.build();
      },
      (name, c) -> {
        b.setOrientation(fromCoreCurveOrientation(c));
        return b.build();
      },
      (name, c) -> {
        b.setScale(fromCoreCurveScale(c));
        return b.build();
      });
  }

  private static Skeleton.V1CurveScale fromCoreCurveScale(
    final CaDefinitionCurveScaleType c)
  {
    final Skeleton.V1CurveScale.Builder b = Skeleton.V1CurveScale.newBuilder();
    b.setBone(c.bone().value());
    c.keyframes().forEach(k -> b.addKeyframes(fromCoreCurveKeyframeScale(k)));
    return b.build();
  }

  private static Skeleton.V1CurveKeyframeScale fromCoreCurveKeyframeScale(
    final CaDefinitionCurveKeyframeScaleType k)
  {
    final Skeleton.V1CurveKeyframeScale.Builder b =
      Skeleton.V1CurveKeyframeScale.newBuilder();
    b.setEasing(fromCoreEasing(k.easing()));
    b.setInterpolation(fromCoreInterpolation(k.interpolation()));
    b.setIndex(k.index());
    b.setScale(fromCoreScale(k.scale()));
    return b.build();
  }

  private static Skeleton.V1CurveKeyframeTranslation fromCoreCurveKeyframeTranslation(
    final CaDefinitionCurveKeyframeTranslationType k)
  {
    final Skeleton.V1CurveKeyframeTranslation.Builder b =
      Skeleton.V1CurveKeyframeTranslation.newBuilder();
    b.setEasing(fromCoreEasing(k.easing()));
    b.setInterpolation(fromCoreInterpolation(k.interpolation()));
    b.setIndex(k.index());
    b.setTranslation(fromCoreTranslation(k.translation()));
    return b.build();
  }

  private static Skeleton.V1CurveKeyframeOrientation fromCoreCurveKeyframeOrientation(
    final CaDefinitionCurveKeyframeOrientationType k)
  {
    final Skeleton.V1CurveKeyframeOrientation.Builder b =
      Skeleton.V1CurveKeyframeOrientation.newBuilder();
    b.setEasing(fromCoreEasing(k.easing()));
    b.setInterpolation(fromCoreInterpolation(k.interpolation()));
    b.setIndex(k.index());
    b.setOrientation(fromCoreQuaternion(k.orientation()));
    return b.build();
  }

  private static Skeleton.V1Scale fromCoreScale(
    final VectorI3D scale)
  {
    final Skeleton.V1Scale.Builder b = Skeleton.V1Scale.newBuilder();
    b.setX(scale.getXD());
    b.setY(scale.getYD());
    b.setZ(scale.getZD());
    return b.build();
  }

  private static Skeleton.V1Translation fromCoreTranslation(
    final PVectorI3D<CaSpaceBoneParentRelativeType> translation)
  {
    final Skeleton.V1Translation.Builder b = Skeleton.V1Translation.newBuilder();
    b.setX(translation.getXD());
    b.setY(translation.getYD());
    b.setZ(translation.getZD());
    return b.build();
  }

  private static Skeleton.V1Quaternion fromCoreQuaternion(
    final QuaternionI4D orientation)
  {
    final Skeleton.V1Quaternion.Builder b = Skeleton.V1Quaternion.newBuilder();
    b.setX(orientation.getXD());
    b.setY(orientation.getYD());
    b.setZ(orientation.getZD());
    b.setW(orientation.getWD());
    return b.build();
  }

  private static Skeleton.V1Interpolation fromCoreInterpolation(
    final CaCurveInterpolation interpolation)
  {
    switch (interpolation) {
      case CURVE_INTERPOLATION_CONSTANT:
        return Skeleton.V1Interpolation.V1_INTERPOLATION_CONSTANT;
      case CURVE_INTERPOLATION_LINEAR:
        return Skeleton.V1Interpolation.V1_INTERPOLATION_LINEAR;
      case CURVE_INTERPOLATION_EXPONENTIAL:
        return Skeleton.V1Interpolation.V1_INTERPOLATION_EXPONENTIAL;
    }

    throw new UnreachableCodeException();
  }

  private static Skeleton.V1Easing fromCoreEasing(
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

  private static Skeleton.V1CurveTranslation fromCoreCurveTranslation(
    final CaDefinitionCurveTranslationType c)
  {
    final Skeleton.V1CurveTranslation.Builder b =
      Skeleton.V1CurveTranslation.newBuilder();
    b.setBone(c.bone().value());
    c.keyframes().forEach(k -> b.addKeyframes(fromCoreCurveKeyframeTranslation(k)));
    return b.build();
  }

  private static Skeleton.V1CurveOrientation fromCoreCurveOrientation(
    final CaDefinitionCurveOrientationType c)
  {
    final Skeleton.V1CurveOrientation.Builder b =
      Skeleton.V1CurveOrientation.newBuilder();
    b.setBone(c.bone().value());
    c.keyframes().forEach(k -> b.addKeyframes(fromCoreCurveKeyframeOrientation(k)));
    return b.build();
  }

  @Override
  public Validation<List<CaParseError>, CaDefinitionSkeletonType>
  parseSkeletonFromStream(
    final InputStream is,
    final URI uri)
  {
    return new Parsed(uri, is).run();
  }

  @Override
  public void serializeSkeletonToStream(
    final CaDefinitionSkeletonType skeleton,
    final OutputStream out)
    throws IOException
  {
    fromCoreSkeleton(skeleton).writeTo(out);
  }

  private static final class Parsed
  {
    private final URI uri;
    private final InputStream stream;

    Parsed(
      final URI in_uri,
      final InputStream in_stream)
    {
      this.uri = NullCheck.notNull(in_uri, "URI");
      this.stream = NullCheck.notNull(in_stream, "Stream");
    }

    private static <T, U> List<Validation<List<CaParseError>, U>> list(
      final Collection<T> v,
      final Function<T, Validation<List<CaParseError>, U>> f)
    {
      return v.stream().map(f).collect(List.collector());
    }

    private static <T> Validation<List<CaParseError>, List<T>> errorList(
      final List<Validation<List<CaParseError>, T>> results)
    {
      final List<Validation<List<CaParseError>, T>> invalids =
        results.filter(Validation::isInvalid);

      if (!invalids.isEmpty()) {
        return invalid(results.map(Validation::getError)
                         .fold(List.empty(), List::appendAll));
      }
      return valid(results.map(Validation::get));
    }

    private Validation<List<CaParseError>, VectorI3D> scale(
      final Skeleton.V1ScaleOrBuilder v)
    {
      return this.notNull(v, "Scale").flatMap(
        vnn -> valid(new VectorI3D(vnn.getX(), vnn.getY(), vnn.getZ())));
    }

    private Validation<List<CaParseError>, PVectorI3D<CaSpaceBoneParentRelativeType>> translation(
      final Skeleton.V1TranslationOrBuilder v)
    {
      return this.notNull(v, "Translation").flatMap(
        vnn -> valid(new PVectorI3D<>(vnn.getX(), vnn.getY(), vnn.getZ())));
    }

    private Validation<List<CaParseError>, QuaternionI4D> quaternion(
      final Skeleton.V1QuaternionOrBuilder v)
    {
      return this.notNull(v, "Quaternion").flatMap(
        qnn -> valid(new QuaternionI4D(
          qnn.getX(), qnn.getY(), qnn.getZ(), qnn.getW())));
    }

    private <T> Validation<List<CaParseError>, T> invalidList(
      final String message)
    {
      return Validation.invalid(List.of(this.errorFor(message)));
    }

    private <T> Validation<List<CaParseError>, T> notNull(
      final T x,
      final String name)
    {
      if (x == null) {
        return this.invalidList("Value " + name + " must not be null");
      }
      return valid(x);
    }

    private CaParseError errorFor(
      final String message)
    {
      final Path path = Paths.get(this.uri);
      return CaParseError.of(
        LexicalPosition.of(0, 0, Optional.of(path)),
        message);
    }

    private Validation<List<CaParseError>, String> nonEmptyString(
      final String value,
      final String name)
    {
      return this.notNull(value, name).flatMap(text -> {
        if (text.isEmpty()) {
          return this.invalidList(name + " is not set or is empty");
        }
        return valid(text);
      });
    }

    private Validation<List<CaParseError>, Optional<String>> maybeEmptyString(
      final String value,
      final String name)
    {
      return this.notNull(value, name).flatMap(text -> {
        if (!text.isEmpty()) {
          return valid(Optional.of(text));
        }
        return valid(Optional.empty());
      });
    }

    private Validation<List<CaParseError>, CaDefinitionBoneType> bone(
      final Skeleton.V1BoneOrBuilder v_bone)
    {
      return this.notNull(v_bone, "Bone").flatMap(bone -> {
        final Validation<List<CaParseError>, CaBoneName> v_name =
          this.nonEmptyString(
            v_bone.getName(),
            "Bone name").map(CaBoneName::of);
        final Validation<List<CaParseError>, Optional<CaBoneName>> v_parent =
          this.maybeEmptyString(
            v_bone.getParent(),
            "Parent bone").map(t -> t.map(CaBoneName::of));
        final Validation<List<CaParseError>, PVectorI3D<CaSpaceBoneParentRelativeType>> v_translation =
          this.translation(v_bone.getTranslation());
        final Validation<List<CaParseError>, QuaternionI4D> v_orientation =
          this.quaternion(v_bone.getOrientation());
        final Validation<List<CaParseError>, VectorI3D> v_scale =
          this.scale(v_bone.getScale());

        return flatten(
          Validation.combine(
            v_name,
            v_parent,
            v_translation,
            v_orientation,
            v_scale)
            .ap((b_name, b_parent, b_trans, b_quat, b_scale) -> {
              final CaDefinitionBone.Builder bb = CaDefinitionBone.builder();
              bb.setName(b_name);
              bb.setParent(b_parent);
              bb.setScale(b_scale);
              bb.setOrientation(b_quat);
              bb.setTranslation(b_trans);
              return bb.build();
            }));
      });
    }

    private Validation<List<CaParseError>, CaDefinitionActionType> action(
      final Skeleton.V1ActionOrBuilder v_action)
    {
      return this.notNull(v_action.getActionCase(), "Action case")
        .flatMap(c -> {
          switch (c) {
            case CURVES:
              return this.actionCurves(v_action.getCurves());
            case ACTION_NOT_SET:
              return this.invalidList("Action not set");
          }
          throw new UnreachableCodeException();
        });
    }

    private Validation<List<CaParseError>, CaDefinitionActionType> actionCurves(
      final Skeleton.V1ActionCurvesOrBuilder action_curves)
    {
      return this.notNull(action_curves, "Action").flatMap(ac -> {
        final Validation<List<CaParseError>, CaActionName> v_name =
          this.nonEmptyString(
            ac.getName(),
            "Action name").map(CaActionName::of);
        final Validation<List<CaParseError>, Map<CaBoneName, List<CaDefinitionCurveType>>> v_curves =
          this.notNull(ac.getCurvesList(), "Curves")
            .flatMap(curves -> errorList(list(curves, this::curve)))
            .map(curves -> curves.groupBy(CaDefinitionCurveType::bone));

        return flatten(
          Validation.combine(v_name, v_curves)
            .ap((c_name, c_curves) -> {
              final CaDefinitionActionCurves.Builder ab =
                CaDefinitionActionCurves.builder();
              ab.setName(c_name);
              ab.setCurves(c_curves);
              ab.setFramesPerSecond(ac.getFramesPerSecond());
              return ab.build();
            }));
      });
    }

    private Validation<List<CaParseError>, CaDefinitionCurveType> curve(
      final Skeleton.V1Curve curve)
    {
      return this.notNull(curve.getCurveCase(), "Curve case")
        .flatMap(c -> {
          switch (c) {
            case TRANSLATION:
              return this.curveTranslation(curve.getTranslation());
            case SCALE:
              return this.curveScale(curve.getScale());
            case ORIENTATION:
              return this.curveOrientation(curve.getOrientation());
            case CURVE_NOT_SET:
              return this.invalidList("Curve not set");
          }
          throw new UnreachableCodeException();
        });
    }

    private Validation<List<CaParseError>, CaCurveInterpolation>
    interpolation(final Skeleton.V1Interpolation interpolation)
    {
      return this.notNull(interpolation, "Interpolation").flatMap(i -> {
        switch (i) {
          case V1_INTERPOLATION_LINEAR:
            return valid(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR);
          case V1_INTERPOLATION_EXPONENTIAL:
            return valid(CaCurveInterpolation.CURVE_INTERPOLATION_EXPONENTIAL);
          case V1_INTERPOLATION_CONSTANT:
            return valid(CaCurveInterpolation.CURVE_INTERPOLATION_CONSTANT);
          case UNRECOGNIZED:
            return this.invalidList("Unrecognized interpolation type");
        }
        throw new UnreachableCodeException();
      });
    }

    private Validation<List<CaParseError>, CaCurveEasing>
    easing(final Skeleton.V1Easing easing)
    {
      return this.notNull(easing, "Easing").flatMap(i -> {
        switch (i) {
          case V1_EASING_IN:
            return valid(CaCurveEasing.CURVE_EASING_IN);
          case V1_EASING_IN_OUT:
            return valid(CaCurveEasing.CURVE_EASING_IN_OUT);
          case V1_EASING_OUT:
            return valid(CaCurveEasing.CURVE_EASING_OUT);
          case UNRECOGNIZED:
            return this.invalidList("Unrecognized easing type");
        }
        throw new UnreachableCodeException();
      });
    }

    private Validation<List<CaParseError>, CaDefinitionCurveKeyframeTranslationType>
    curveKeyframeTranslation(
      final Skeleton.V1CurveKeyframeTranslation translation)
    {
      return this.notNull(translation, "Translation").flatMap(kf -> {
        final Validation<List<CaParseError>, PVectorI3D<CaSpaceBoneParentRelativeType>> v_trans =
          this.translation(translation.getTranslation());
        final Validation<List<CaParseError>, CaCurveEasing> v_ease =
          this.easing(translation.getEasing());
        final Validation<List<CaParseError>, CaCurveInterpolation> v_inter =
          this.interpolation(translation.getInterpolation());
        return flatten(
          Validation.combine(v_trans, v_ease, v_inter)
            .ap((c_trans, c_easing, c_interpolation) -> {
              final CaDefinitionCurveKeyframeTranslation.Builder b =
                CaDefinitionCurveKeyframeTranslation.builder();
              b.setTranslation(c_trans);
              b.setEasing(c_easing);
              b.setInterpolation(c_interpolation);
              b.setIndex(translation.getIndex());
              return b.build();
            }));
      });
    }

    private Validation<List<CaParseError>, CaDefinitionCurveTranslationType>
    curveTranslation(
      final Skeleton.V1CurveTranslation translation)
    {
      return this.notNull(translation, "Translation").flatMap(tr -> {
        final Validation<List<CaParseError>, CaBoneName> v_name =
          this.nonEmptyString(tr.getBone(), "Bone name").map(CaBoneName::of);
        final Validation<List<CaParseError>, List<CaDefinitionCurveKeyframeTranslationType>> v_frames =
          this.curveTranslationKeyframes(tr.getKeyframesList());

        return flatten(
          Validation.combine(v_name, v_frames)
            .ap((c_bone, c_frames) -> {
              final CaDefinitionCurveTranslation.Builder b =
                CaDefinitionCurveTranslation.builder();
              b.setKeyframes(c_frames);
              b.setBone(c_bone);
              return b.build();
            }));
      });
    }

    private Validation<List<CaParseError>, List<CaDefinitionCurveKeyframeTranslationType>>
    curveTranslationKeyframes(
      final java.util.List<Skeleton.V1CurveKeyframeTranslation> tr)
    {
      return this.notNull(tr, "Keyframes").flatMap(
        ts -> errorList(ts.stream()
                          .map(this::curveKeyframeTranslation)
                          .collect(List.collector())));
    }

    private Validation<List<CaParseError>, CaDefinitionCurveKeyframeScaleType>
    curveKeyframeScale(
      final Skeleton.V1CurveKeyframeScale scale)
    {
      return this.notNull(scale, "Scale").flatMap(kf -> {
        final Validation<List<CaParseError>, VectorI3D> v_scale =
          this.scale(scale.getScale());
        final Validation<List<CaParseError>, CaCurveEasing> v_ease =
          this.easing(scale.getEasing());
        final Validation<List<CaParseError>, CaCurveInterpolation> v_inter =
          this.interpolation(scale.getInterpolation());
        return flatten(
          Validation.combine(v_scale, v_ease, v_inter)
            .ap((c_scale, c_easing, c_interpolation) -> {
              final CaDefinitionCurveKeyframeScale.Builder b =
                CaDefinitionCurveKeyframeScale.builder();
              b.setScale(c_scale);
              b.setEasing(c_easing);
              b.setInterpolation(c_interpolation);
              b.setIndex(scale.getIndex());
              return b.build();
            }));
      });
    }

    private Validation<List<CaParseError>, List<CaDefinitionCurveKeyframeScaleType>>
    curveScaleKeyframes(
      final java.util.List<Skeleton.V1CurveKeyframeScale> tr)
    {
      return this.notNull(tr, "Keyframes").flatMap(
        ts -> errorList(ts.stream()
                          .map(this::curveKeyframeScale)
                          .collect(List.collector())));
    }

    private Validation<List<CaParseError>, CaDefinitionCurveScaleType>
    curveScale(
      final Skeleton.V1CurveScale scale)
    {
      return this.notNull(scale, "Scale").flatMap(tr -> {
        final Validation<List<CaParseError>, CaBoneName> v_name =
          this.nonEmptyString(tr.getBone(), "Bone name").map(CaBoneName::of);
        final Validation<List<CaParseError>, List<CaDefinitionCurveKeyframeScaleType>> v_frames =
          this.curveScaleKeyframes(tr.getKeyframesList());

        return flatten(
          Validation.combine(v_name, v_frames)
            .ap((c_bone, c_frames) -> {
              final CaDefinitionCurveScale.Builder b =
                CaDefinitionCurveScale.builder();
              b.setKeyframes(c_frames);
              b.setBone(c_bone);
              return b.build();
            }));
      });
    }

    private Validation<List<CaParseError>, CaDefinitionCurveKeyframeOrientationType>
    curveKeyframeOrientation(
      final Skeleton.V1CurveKeyframeOrientation orientation)
    {
      return this.notNull(orientation, "Orientation").flatMap(kf -> {
        final Validation<List<CaParseError>, QuaternionI4D> v_orientation =
          this.quaternion(orientation.getOrientation());
        final Validation<List<CaParseError>, CaCurveEasing> v_ease =
          this.easing(orientation.getEasing());
        final Validation<List<CaParseError>, CaCurveInterpolation> v_inter =
          this.interpolation(orientation.getInterpolation());
        return flatten(
          Validation.combine(v_orientation, v_ease, v_inter)
            .ap((c_orientation, c_easing, c_interpolation) -> {
              final CaDefinitionCurveKeyframeOrientation.Builder b =
                CaDefinitionCurveKeyframeOrientation.builder();
              b.setOrientation(c_orientation);
              b.setEasing(c_easing);
              b.setInterpolation(c_interpolation);
              b.setIndex(orientation.getIndex());
              return b.build();
            }));
      });
    }

    private Validation<List<CaParseError>, List<CaDefinitionCurveKeyframeOrientationType>>
    curveOrientationKeyframes(
      final java.util.List<Skeleton.V1CurveKeyframeOrientation> tr)
    {
      return this.notNull(tr, "Keyframes").flatMap(
        ts -> errorList(ts.stream()
                          .map(this::curveKeyframeOrientation)
                          .collect(List.collector())));
    }

    private Validation<List<CaParseError>, CaDefinitionCurveOrientationType>
    curveOrientation(
      final Skeleton.V1CurveOrientation orientation)
    {
      return this.notNull(orientation, "Orientation").flatMap(tr -> {
        final Validation<List<CaParseError>, CaBoneName> v_name =
          this.nonEmptyString(tr.getBone(), "Bone name").map(CaBoneName::of);
        final Validation<List<CaParseError>, List<CaDefinitionCurveKeyframeOrientationType>> v_frames =
          this.curveOrientationKeyframes(tr.getKeyframesList());

        return flatten(
          Validation.combine(v_name, v_frames)
            .ap((c_bone, c_frames) -> {
              final CaDefinitionCurveOrientation.Builder b =
                CaDefinitionCurveOrientation.builder();
              b.setKeyframes(c_frames);
              b.setBone(c_bone);
              return b.build();
            }));
      });
    }

    private Validation<List<CaParseError>, CaDefinitionSkeletonType> run()
    {
      try {
        final Skeleton.V1Skeleton sk =
          Skeleton.V1Skeleton.parseFrom(this.stream);

        final Validation<List<CaParseError>, CaSkeletonName> v_name =
          this.nonEmptyString(
            sk.getName(),
            "Skeleton name").map(CaSkeletonName::of);

        final Validation<List<CaParseError>, Map<CaBoneName, CaDefinitionBoneType>> v_bones =
          this.notNull(sk.getBonesList(), "Bones")
            .flatMap(bones -> errorList(list(bones, this::bone)))
            .flatMap(bones -> this.toMap(
              bones, "bone", CaDefinitionBoneType::name));

        final Validation<List<CaParseError>, Map<CaActionName, CaDefinitionActionType>> v_actions =
          this.notNull(sk.getActionsList(), "Actions")
            .flatMap(actions -> errorList(list(actions, this::action)))
            .flatMap(actions -> this.toMap(
              actions, "action", CaDefinitionActionType::name));

        return flatten(
          Validation.combine(v_name, v_bones, v_actions)
            .ap((c_name, c_bones, c_actions) -> {
              final CaDefinitionSkeleton.Builder b = CaDefinitionSkeleton.builder();
              b.setActions(c_actions);
              b.setBones(c_bones);
              b.setName(c_name);
              return b.build();
            }));

      } catch (final IOException e) {
        return this.invalidList(e.getMessage());
      }
    }

    private <K, V> Validation<List<CaParseError>, Map<K, V>> toMap(
      final Traversable<V> results,
      final String name,
      final Function<V, K> classify)
    {
      final Seq<Tuple2<K, Integer>> duplicates = results.groupBy(
        classify)
        .map(t -> Tuple.of(t._1, Integer.valueOf(t._2.size())))
        .filter(t -> t._2.intValue() > 1);

      if (duplicates.isEmpty()) {
        return valid(
          results.toMap(x -> Tuple.of(classify.apply(x), x)));
      }

      return Validation.invalid(duplicates.map(
        t -> this.errorFor(
          String.format("Duplicate %s: %s", name, t._1))).toList());
    }
  }
}
