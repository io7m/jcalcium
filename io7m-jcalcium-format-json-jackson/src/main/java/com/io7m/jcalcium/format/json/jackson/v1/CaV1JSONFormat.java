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

package com.io7m.jcalcium.format.json.jackson.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.CaBoneNameType;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.definitions.CaDefinitionBone;
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
import javaslang.Tuple;
import javaslang.collection.SortedSet;
import javaslang.collection.TreeSet;
import javaslang.control.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.jfunctional.Unit.unit;

/**
 * A parser and serializer for the 1.* JSON format.
 */

public final class CaV1JSONFormat implements CaDefinitionParserType,
  CaDefinitionSerializerType
{
  private final ObjectMapper mapper;

  /**
   * Construct a new parser/serializer.
   *
   * @param in_mapper An object mapper
   */

  public CaV1JSONFormat(
    final ObjectMapper in_mapper)
  {
    this.mapper = NullCheck.notNull(in_mapper, "Mapper");
  }

  /**
   * @return The parserSupportedVersions supported by this parser
   */

  public static SortedSet<CaFormatVersion> supported()
  {
    return TreeSet.of(CaFormatVersion.of(1, 0));
  }

  @Override
  public Validation<javaslang.collection.List<CaParseError>, CaDefinitionSkeleton>
  parseSkeletonFromStream(
    final InputStream is,
    final URI uri)
  {
    NullCheck.notNull(is, "Input stream");
    NullCheck.notNull(uri, "URI");

    try {
      return Validation.valid(
        this.mapper.readValue(is, CaV1Skeleton.class).toSkeleton());
    } catch (final JsonParseException e) {
      final JsonParser proc = e.getProcessor();
      final JsonLocation loc = proc.getCurrentLocation();
      final javaslang.collection.List<CaParseError> xs =
        javaslang.collection.List.of(
          CaParseError.of(
            LexicalPosition.of(
              loc.getLineNr(),
              loc.getColumnNr(),
              Optional.of(Paths.get(uri))),
            e.getMessage()
          ));
      return Validation.invalid(xs);
    } catch (final IOException e) {
      final javaslang.collection.List<CaParseError> xs =
        javaslang.collection.List.of(
          CaParseError.of(
            LexicalPosition.of(
              -1,
              -1,
              Optional.of(Paths.get(uri))),
            e.getMessage()
          ));
      return Validation.invalid(xs);
    }
  }

  @Override
  public void serializeSkeletonToStream(
    final CaDefinitionSkeletonType skeleton,
    final OutputStream out)
    throws IOException
  {
    NullCheck.notNull(skeleton, "Skeleton");
    NullCheck.notNull(out, "Out");
    this.mapper.writeValue(out, CaV1Skeleton.fromCore(skeleton));
  }

  /**
   * A version 1 action.
   */

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(name = "curves", value = CaV1ActionCurves.class)
  })
  @JsonDeserialize
  @JsonSerialize
  public abstract static class CaV1Action
  {
    @JsonProperty(value = "name", required = true)
    private final String name;
    @JsonProperty(value = "frames-per-second", required = true)
    private final int frames_per_second;

    protected CaV1Action(
      @JsonProperty(value = "name", required = true)
      final String in_name,
      @JsonProperty(value = "frames-per-second", required = true)
      final int in_fps)
    {
      this.name = NullCheck.notNull(in_name, "Name");
      this.frames_per_second = in_fps;
    }

    /**
     * @param a An action
     *
     * @return An action
     */

    public static CaV1Action fromCore(
      final CaDefinitionActionType a)
    {
      return a.matchAction(
        unit(),
        (ac_name, ac) -> CaV1ActionCurves.fromCore(ac));
    }

    /**
     * @return An action
     */

    public abstract CaDefinitionActionType toAction();
  }

  /**
   * A version 1 action constructed from curves.
   */

  @JsonDeserialize
  @JsonSerialize
  public static final class CaV1ActionCurves extends CaV1Action
  {
    @JsonProperty(value = "curves", required = true)
    private final List<CaV1Curve> curves;

    protected CaV1ActionCurves(
      @JsonProperty(value = "name", required = true)
      final String in_name,
      @JsonProperty(value = "frames-per-second", required = true)
      final int in_fps,
      @JsonProperty(value = "curves", required = true)
      final List<CaV1Curve> in_curves)
    {
      super(in_name, in_fps);
      this.curves = NullCheck.notNull(in_curves, "Curves");
    }

    /**
     * @param c A curve-based action
     *
     * @return A curve-based action
     */

    public static CaV1ActionCurves fromCore(
      final CaDefinitionActionCurvesType c)
    {
      final List<CaV1Curve> curves = new ArrayList<>(8);
      c.curves().forEach(
        p -> p._2.forEach(curve -> {
          curves.add(CaV1Curve.fromCore(curve));
        }));

      return new CaV1ActionCurves(
        c.name().value(),
        c.framesPerSecond(),
        curves);
    }

    @Override
    public CaDefinitionActionCurvesType toAction()
    {
      final CaDefinitionActionCurves.Builder act_b =
        CaDefinitionActionCurves.builder();
      act_b.setFramesPerSecond(super.frames_per_second);
      act_b.setName(CaActionName.of(super.name));
      act_b.setCurves(this.curves.stream()
                        .map(CaV1Curve::toCurve)
                        .collect(javaslang.collection.List.collector())
                        .groupBy(CaDefinitionCurveType::bone));
      return act_b.build();
    }
  }

  /**
   * A version 1 bone.
   */

  @JsonDeserialize
  @JsonSerialize
  public static final class CaV1Bone
  {
    @JsonProperty(value = "name", required = true)
    private final String name;
    @JsonProperty("parent")
    private final Optional<String> parent;
    @JsonProperty(value = "translation", required = true)
    private final PVectorI3D<CaSpaceBoneParentRelativeType> translation;
    @JsonProperty(value = "orientation-xyzw", required = true)
    private final QuaternionI4D orientation;
    @JsonProperty(value = "scale", required = true)
    private final VectorI3D scale;

    /**
     * Construct a bone
     *
     * @param in_name        The bone name
     * @param in_parent      The bone parent
     * @param in_translation The bone translation
     * @param in_orientation The bone orientation
     * @param in_scale       The bone scale
     */

    @JsonCreator
    public CaV1Bone(
      @JsonProperty(value = "name", required = true)
      final String in_name,
      @JsonProperty("parent")
      final Optional<String> in_parent,
      @JsonProperty(value = "translation", required = true)
      final PVectorI3D<CaSpaceBoneParentRelativeType> in_translation,
      @JsonProperty(value = "orientation-xyzw", required = true)
      final QuaternionI4D in_orientation,
      @JsonProperty(value = "scale", required = true)
      final VectorI3D in_scale)
    {
      this.name = NullCheck.notNull(in_name, "Name");
      this.parent = NullCheck.notNull(in_parent, "Parent");
      this.translation = NullCheck.notNull(in_translation, "Translation");
      this.orientation = NullCheck.notNull(in_orientation, "Orientation");
      this.scale = NullCheck.notNull(in_scale, "Scale");
    }

    /**
     * @param b A bone
     *
     * @return A bone
     */

    public static CaV1Bone fromCore(
      final CaDefinitionBone b)
    {
      return new CaV1Bone(
        b.name().value(),
        b.parent().map(CaBoneNameType::value),
        b.translation(),
        b.orientation(),
        b.scale());
    }

    /**
     * @return A bone
     */

    public CaDefinitionBone toBone()
    {
      final CaDefinitionBone.Builder bb = CaDefinitionBone.builder();
      bb.setParent(this.parent.map(CaBoneName::of));
      bb.setOrientation(this.orientation);
      bb.setTranslation(this.translation);
      bb.setScale(this.scale);
      bb.setName(CaBoneName.of(this.name));
      return bb.build();
    }
  }

  /**
   * A version 1 curve.
   */

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(name = "orientation", value = CaV1CurveQuaternion.class),
    @JsonSubTypes.Type(name = "scale", value = CaV1CurveScale.class),
    @JsonSubTypes.Type(name = "translation", value = CaV1CurveTranslation.class)
  })
  @JsonDeserialize
  @JsonSerialize
  public abstract static class CaV1Curve
  {
    @JsonProperty(value = "bone", required = true)
    private final String bone;

    protected CaV1Curve(
      @JsonProperty(value = "bone", required = true)
      final String in_bone)
    {
      this.bone = NullCheck.notNull(in_bone, "Bone");
    }

    /**
     * @param curve A curve
     *
     * @return A curve
     */

    public static CaV1Curve fromCore(
      final CaDefinitionCurveType curve)
    {
      return curve.matchCurve(
        unit(),
        (name, ct) -> CaV1CurveTranslation.fromCore(ct),
        (name, co) -> CaV1CurveQuaternion.fromCore(co),
        (name, cs) -> CaV1CurveScale.fromCore(cs));
    }

    /**
     * @return A curve
     */

    public abstract CaDefinitionCurveType toCurve();
  }

  /**
   * A version 1 orientation curve.
   */

  @JsonDeserialize
  @JsonSerialize
  public static final class CaV1CurveQuaternion extends CaV1Curve
  {
    @JsonProperty(value = "keyframes", required = true)
    private final List<CaV1KeyframeCurveOrientation> keyframes;

    protected CaV1CurveQuaternion(
      @JsonProperty(value = "bone", required = true)
      final String in_bone,
      @JsonProperty(value = "keyframes", required = true)
      final List<CaV1KeyframeCurveOrientation> in_keyframes)
    {
      super(in_bone);
      this.keyframes = NullCheck.notNull(in_keyframes, "Keyframes");
    }

    /**
     * @param c A curve
     *
     * @return A serialized curve
     */

    public static CaV1CurveQuaternion fromCore(
      final CaDefinitionCurveOrientationType c)
    {
      return new CaV1CurveQuaternion(
        c.bone().value(),
        c.keyframes().map(CaV1KeyframeCurveOrientation::fromCore).toJavaList());
    }

    @Override
    public CaDefinitionCurveOrientationType toCurve()
    {
      final CaDefinitionCurveOrientation.Builder cb =
        CaDefinitionCurveOrientation.builder();
      cb.setBone(CaBoneName.of(super.bone));
      cb.setKeyframes(this.keyframes.stream()
                        .map(CaV1KeyframeCurveOrientation::toKeyframe)
                        .collect(javaslang.collection.List.collector()));
      return cb.build();
    }
  }

  /**
   * A version 1 scale curve.
   */

  @JsonDeserialize
  @JsonSerialize
  public static final class CaV1CurveScale extends CaV1Curve
  {
    @JsonProperty(value = "keyframes", required = true)
    private final List<CaV1KeyframeCurveScale> keyframes;

    protected CaV1CurveScale(
      @JsonProperty(value = "bone", required = true)
      final String in_bone,
      @JsonProperty(value = "keyframes", required = true)
      final List<CaV1KeyframeCurveScale> in_keyframes)
    {
      super(in_bone);
      this.keyframes = NullCheck.notNull(in_keyframes, "Keyframes");
    }

    /**
     * @param c A curve
     *
     * @return A serialized curve
     */

    public static CaV1CurveScale fromCore(
      final CaDefinitionCurveScaleType c)
    {
      return new CaV1CurveScale(
        c.bone().value(),
        c.keyframes().map(CaV1KeyframeCurveScale::fromCore).toJavaList());
    }

    @Override
    public CaDefinitionCurveType toCurve()
    {
      final CaDefinitionCurveScale.Builder cb =
        CaDefinitionCurveScale.builder();
      cb.setBone(CaBoneName.of(super.bone));
      cb.setKeyframes(this.keyframes.stream()
                        .map(CaV1KeyframeCurveScale::toKeyframe)
                        .collect(javaslang.collection.List.collector()));
      return cb.build();
    }
  }

  /**
   * A version 1 translation curve.
   */

  @JsonDeserialize
  @JsonSerialize
  public static final class CaV1CurveTranslation extends CaV1Curve
  {
    @JsonProperty(value = "keyframes", required = true)
    private final List<CaV1KeyframeCurveTranslation> keyframes;

    protected CaV1CurveTranslation(
      @JsonProperty(value = "bone", required = true)
      final String in_bone,
      @JsonProperty(value = "keyframes", required = true)
      final List<CaV1KeyframeCurveTranslation> in_keyframes)
    {
      super(in_bone);
      this.keyframes = NullCheck.notNull(in_keyframes, "Keyframes");
    }

    /**
     * @param c A curve
     *
     * @return A serialized curve
     */

    public static CaV1CurveTranslation fromCore(
      final CaDefinitionCurveTranslationType c)
    {
      return new CaV1CurveTranslation(
        c.bone().value(),
        c.keyframes().map(CaV1KeyframeCurveTranslation::fromCore).toJavaList());
    }

    @Override
    public CaDefinitionCurveType toCurve()
    {
      final CaDefinitionCurveTranslation.Builder cb =
        CaDefinitionCurveTranslation.builder();
      cb.setBone(CaBoneName.of(super.bone));
      cb.setKeyframes(this.keyframes.stream()
                        .map(CaV1KeyframeCurveTranslation::toKeyframe)
                        .collect(javaslang.collection.List.collector()));
      return cb.build();
    }
  }

  /**
   * A version 1 orientation curve keyframe.
   */

  @JsonDeserialize
  @JsonSerialize
  public static final class CaV1KeyframeCurveOrientation
  {
    @JsonProperty(value = "index", required = true)
    private final int index;
    @JsonProperty(value = "interpolation", required = true)
    private final CaCurveInterpolation interpolation;
    @JsonProperty(value = "easing", required = true)
    private final CaCurveEasing easing;
    @JsonProperty(value = "quaternion-xyzw", required = true)
    private final QuaternionI4D quaternion;

    CaV1KeyframeCurveOrientation(
      @JsonProperty(value = "index", required = true)
      final int in_index,
      @JsonProperty(value = "interpolation", required = true)
      final CaCurveInterpolation in_interpolation,
      @JsonProperty(value = "easing", required = true)
      final CaCurveEasing in_easing,
      @JsonProperty(value = "quaternion-xyzw", required = true)
      final QuaternionI4D in_quaternion)
    {
      this.index = in_index;
      this.interpolation = NullCheck.notNull(in_interpolation, "Interpolation");
      this.easing = NullCheck.notNull(in_easing, "Easing");
      this.quaternion = NullCheck.notNull(in_quaternion, "Quaternion");
    }

    /**
     * @param d A keyframe
     *
     * @return A serialized keyframe
     */

    public static CaV1KeyframeCurveOrientation fromCore(
      final CaDefinitionCurveKeyframeOrientationType d)
    {
      return new CaV1KeyframeCurveOrientation(
        d.index(),
        d.interpolation(),
        d.easing(),
        d.orientation());
    }

    /**
     * @return A keyframe
     */

    public CaDefinitionCurveKeyframeOrientationType toKeyframe()
    {
      final CaDefinitionCurveKeyframeOrientation.Builder cb =
        CaDefinitionCurveKeyframeOrientation.builder();
      cb.setEasing(this.easing);
      cb.setInterpolation(this.interpolation);
      cb.setIndex(this.index);
      cb.setOrientation(this.quaternion);
      return cb.build();
    }
  }

  /**
   * A version 1 translation curve keyframe.
   */

  @JsonDeserialize
  @JsonSerialize
  public static final class CaV1KeyframeCurveTranslation
  {
    @JsonProperty(value = "index", required = true)
    private final int index;
    @JsonProperty(value = "interpolation", required = true)
    private final CaCurveInterpolation interpolation;
    @JsonProperty(value = "easing", required = true)
    private final CaCurveEasing easing;
    @JsonProperty(value = "translation", required = true)
    private final PVectorI3D<CaSpaceBoneParentRelativeType> translation;

    CaV1KeyframeCurveTranslation(
      @JsonProperty(value = "index", required = true)
      final int in_index,
      @JsonProperty(value = "interpolation", required = true)
      final CaCurveInterpolation in_interpolation,
      @JsonProperty(value = "easing", required = true)
      final CaCurveEasing in_easing,
      @JsonProperty(value = "translation", required = true)
      final PVectorI3D<CaSpaceBoneParentRelativeType> in_translation)
    {
      this.index = in_index;
      this.interpolation = NullCheck.notNull(in_interpolation, "Interpolation");
      this.easing = NullCheck.notNull(in_easing, "Easing");
      this.translation = NullCheck.notNull(in_translation, "Translation");
    }

    /**
     * @param d A keyframe
     *
     * @return A serialized keyframe
     */

    public static CaV1KeyframeCurveTranslation fromCore(
      final CaDefinitionCurveKeyframeTranslationType d)
    {
      return new CaV1KeyframeCurveTranslation(
        d.index(),
        d.interpolation(),
        d.easing(),
        d.translation());
    }

    /**
     * @return A keyframe
     */

    public CaDefinitionCurveKeyframeTranslationType toKeyframe()
    {
      final CaDefinitionCurveKeyframeTranslation.Builder cb =
        CaDefinitionCurveKeyframeTranslation.builder();
      cb.setEasing(this.easing);
      cb.setInterpolation(this.interpolation);
      cb.setIndex(this.index);
      cb.setTranslation(this.translation);
      return cb.build();
    }
  }

  /**
   * A version 1 scale curve keyframe.
   */

  @JsonDeserialize
  @JsonSerialize
  public static final class CaV1KeyframeCurveScale
  {
    @JsonProperty(value = "index", required = true)
    private final int index;
    @JsonProperty(value = "interpolation", required = true)
    private final CaCurveInterpolation interpolation;
    @JsonProperty(value = "easing", required = true)
    private final CaCurveEasing easing;
    @JsonProperty(value = "scale", required = true)
    private final VectorI3D scale;

    CaV1KeyframeCurveScale(
      @JsonProperty(value = "index", required = true)
      final int in_index,
      @JsonProperty(value = "interpolation", required = true)
      final CaCurveInterpolation in_interpolation,
      @JsonProperty(value = "easing", required = true)
      final CaCurveEasing in_easing,
      @JsonProperty(value = "scale", required = true)
      final VectorI3D in_scale)
    {
      this.index = in_index;
      this.interpolation = NullCheck.notNull(in_interpolation, "Interpolation");
      this.easing = NullCheck.notNull(in_easing, "Easing");
      this.scale = NullCheck.notNull(in_scale, "Scale");
    }

    /**
     * @param d A keyframe
     *
     * @return A serialized keyframe
     */

    public static CaV1KeyframeCurveScale fromCore(
      final CaDefinitionCurveKeyframeScaleType d)
    {
      return new CaV1KeyframeCurveScale(
        d.index(),
        d.interpolation(),
        d.easing(),
        d.scale());
    }

    /**
     * @return A keyframe
     */

    public CaDefinitionCurveKeyframeScaleType toKeyframe()
    {
      final CaDefinitionCurveKeyframeScale.Builder cb =
        CaDefinitionCurveKeyframeScale.builder();
      cb.setEasing(this.easing);
      cb.setInterpolation(this.interpolation);
      cb.setIndex(this.index);
      cb.setScale(this.scale);
      return cb.build();
    }
  }

  /**
   * A version 1 skeleton.
   */

  @JsonDeserialize
  @JsonSerialize
  public static final class CaV1Skeleton
  {
    @JsonProperty(value = "name", required = true)
    private final String name;
    @JsonProperty(value = "bones", required = true)
    private final List<CaV1Bone> bones;
    @JsonProperty(value = "actions", required = true)
    private final List<CaV1Action> actions;

    @JsonCreator
    CaV1Skeleton(
      @JsonProperty(value = "name", required = true)
      final String in_name,
      @JsonProperty(value = "bones", required = true)
      final List<CaV1Bone> in_bones,
      @JsonProperty(value = "actions", required = true)
      final List<CaV1Action> in_actions)
    {
      this.name = NullCheck.notNull(in_name, "Name");
      this.bones = NullCheck.notNull(in_bones, "Bones");
      this.actions = NullCheck.notNull(in_actions, "Actions");

      final Collection<String> act_names = new HashSet<>(this.actions.size());
      final Collection<String> act_dup = new HashSet<>(this.actions.size());
      this.actions.forEach(act -> {
        if (act_names.contains(act.name)) {
          act_dup.add(act.name);
        }
        act_names.add(act.name);
      });

      final Collection<String> bone_names = new HashSet<>(this.bones.size());
      final Collection<String> bone_dup = new HashSet<>(this.bones.size());
      this.bones.forEach(act -> {
        if (bone_names.contains(act.name)) {
          bone_dup.add(act.name);
        }
        bone_names.add(act.name);
      });

      if ((!act_dup.isEmpty()) || (!bone_dup.isEmpty())) {
        final StringBuilder b = new StringBuilder(128);
        if (!act_dup.isEmpty()) {
          b.append("Duplicate actions: ");
          b.append(act_dup.stream().collect(Collectors.joining(" ")));
          b.append(System.lineSeparator());
        }
        if (!bone_dup.isEmpty()) {
          b.append("Duplicate bones: ");
          b.append(bone_dup.stream().collect(Collectors.joining(" ")));
          b.append(System.lineSeparator());
        }
        throw new IllegalArgumentException(b.toString());
      }
    }

    /**
     * @param c A skeleton
     *
     * @return A 1.* skeleton
     */

    public static CaV1Skeleton fromCore(
      final CaDefinitionSkeletonType c)
    {
      return new CaV1Skeleton(
        c.name().value(),
        c.bones().values().map(CaV1Bone::fromCore).toJavaList(),
        c.actions().values().map(CaV1Action::fromCore).toJavaList());
    }

    /**
     * @return A skeleton
     */

    public CaDefinitionSkeleton toSkeleton()
    {
      final CaDefinitionSkeleton.Builder sk_b = CaDefinitionSkeleton.builder();
      sk_b.setName(CaSkeletonName.of(this.name));
      sk_b.setActions(this.actions.stream()
                        .map(CaV1Action::toAction)
                        .collect(javaslang.collection.List.collector())
                        .toMap(act -> Tuple.of(act.name(), act)));
      sk_b.setBones(this.bones.stream()
                      .map(CaV1Bone::toBone)
                      .collect(javaslang.collection.List.collector())
                      .toMap(bone -> Tuple.of(bone.name(), bone)));
      return sk_b.build();
    }
  }

  /**
   * A deserializer for {@link CaCurveInterpolation} values.
   */

  public static final class CaCurveInterpolationDeserializer
    extends StdDeserializer<CaCurveInterpolation>
  {
    /**
     * Construct a deserializer
     */

    public CaCurveInterpolationDeserializer()
    {
      super(PVectorI3D.class);
    }

    @Override
    public CaCurveInterpolation deserialize(
      final JsonParser p,
      final DeserializationContext ctxt)
      throws IOException, JsonProcessingException
    {
      final TreeNode n = p.getCodec().readTree(p);
      if (n instanceof TextNode) {
        switch (((TextNode) n).asText()) {
          case "linear":
            return CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
          case "constant":
            return CaCurveInterpolation.CURVE_INTERPOLATION_CONSTANT;
          case "exponential":
            return CaCurveInterpolation.CURVE_INTERPOLATION_EXPONENTIAL;
        }
      }

      throw new JsonParseException(
        p,
        "Expected: linear | constant | exponential");
    }
  }

  /**
   * A deserializer for {@link CaCurveEasing} values.
   */

  public static final class CaCurveEasingDeserializer
    extends StdDeserializer<CaCurveEasing>
  {
    /**
     * Construct a deserializer
     */

    public CaCurveEasingDeserializer()
    {
      super(PVectorI3D.class);
    }

    @Override
    public CaCurveEasing deserialize(
      final JsonParser p,
      final DeserializationContext ctxt)
      throws IOException, JsonProcessingException
    {
      final TreeNode n = p.getCodec().readTree(p);
      if (n instanceof TextNode) {
        switch (((TextNode) n).asText()) {
          case "in":
            return CaCurveEasing.CURVE_EASING_IN;
          case "out":
            return CaCurveEasing.CURVE_EASING_OUT;
          case "in-out":
            return CaCurveEasing.CURVE_EASING_IN_OUT;
        }
      }

      throw new JsonParseException(p, "Expected: in | out | in-out");
    }
  }

  /**
   * A serializer for {@link CaCurveInterpolation} values.
   */

  public static final class CaCurveInterpolationSerializer
    extends StdSerializer<CaCurveInterpolation>
  {
    /**
     * Construct a serializer.
     */

    public CaCurveInterpolationSerializer()
    {
      super(CaCurveInterpolation.class);
    }

    @Override
    public void serialize(
      final CaCurveInterpolation value,
      final JsonGenerator gen,
      final SerializerProvider provider)
      throws IOException
    {
      switch (value) {
        case CURVE_INTERPOLATION_CONSTANT: {
          gen.writeRawValue("\"constant\"");
          break;
        }
        case CURVE_INTERPOLATION_LINEAR: {
          gen.writeRawValue("\"linear\"");
          break;
        }
        case CURVE_INTERPOLATION_EXPONENTIAL: {
          gen.writeRawValue("\"exponential\"");
          break;
        }
      }
    }
  }

  /**
   * A serializer for {@link CaCurveEasing} values.
   */

  public static final class CaCurveEasingSerializer
    extends StdSerializer<CaCurveEasing>
  {
    /**
     * Construct a serializer.
     */

    public CaCurveEasingSerializer()
    {
      super(CaCurveEasing.class);
    }

    @Override
    public void serialize(
      final CaCurveEasing value,
      final JsonGenerator gen,
      final SerializerProvider provider)
      throws IOException
    {
      switch (value) {
        case CURVE_EASING_IN: {
          gen.writeRawValue("\"in\"");
          break;
        }
        case CURVE_EASING_OUT: {
          gen.writeRawValue("\"out\"");
          break;
        }
        case CURVE_EASING_IN_OUT: {
          gen.writeRawValue("\"in-out\"");
          break;
        }
      }
    }
  }
}
