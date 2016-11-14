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

package com.io7m.jcalcium.parser.json.jackson.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.definitions.CaDefinitionBone;
import com.io7m.jcalcium.core.definitions.CaDefinitionBoneType;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeletonType;
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
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveTranslation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jcalcium.parser.api.CaParseErrorType;
import com.io7m.jcalcium.parser.api.CaParserVersion;
import com.io7m.jcalcium.parser.api.CaParserVersionType;
import com.io7m.jlexing.core.ImmutableLexicalPosition;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import javaslang.Tuple;
import javaslang.control.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A parser for the 1.* JSON format.
 */

public final class CaV1JSONParser implements CaDefinitionParserType
{
  private final ObjectMapper mapper;

  /**
   * Construct a new parser.
   *
   * @param in_mapper An object mapper
   */

  public CaV1JSONParser(
    final ObjectMapper in_mapper)
  {
    this.mapper = NullCheck.notNull(in_mapper, "Mapper");
  }

  @Override
  public Validation<
    javaslang.collection.List<CaParseErrorType>,
    CaDefinitionSkeletonType>
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
      final javaslang.collection.List<CaParseErrorType> xs =
        javaslang.collection.List.of(
          CaParseError.of(
            ImmutableLexicalPosition.newPositionWithFile(
              loc.getLineNr(),
              loc.getColumnNr(),
              Paths.get(uri)),
            e.getMessage()
          ));
      return Validation.invalid(xs);
    } catch (final IOException e) {
      final javaslang.collection.List<CaParseErrorType> xs =
        javaslang.collection.List.of(
          CaParseError.of(
            ImmutableLexicalPosition.newPositionWithFile(
              -1,
              -1,
              Paths.get(uri)),
            e.getMessage()
          ));
      return Validation.invalid(xs);
    }
  }

  /**
   * @return The versions supported by this parser
   */

  public static javaslang.collection.List<CaParserVersionType> supported()
  {
    return javaslang.collection.List.of(CaParserVersion.of(1, 0));
  }

  /**
   * A version 1 action.
   */

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(name = "curves", value = CaV1ActionCurves.class)
  })
  @JsonDeserialize
  public abstract static class CaV1Action
  {
    private final String name;
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
     * @return An action
     */

    public abstract CaDefinitionActionType toAction();
  }

  /**
   * A version 1 action constructed from curves.
   */

  @JsonDeserialize
  public static final class CaV1ActionCurves extends CaV1Action
  {
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
  public static final class CaV1Bone
  {
    private final String name;
    private final Optional<String> parent;
    private final PVectorI3D<CaSpaceBoneParentRelativeType> translation;
    private final QuaternionI4D orientation;
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
     * @return A bone
     */

    public CaDefinitionBoneType toBone()
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
  public abstract static class CaV1Curve
  {
    private final String bone;

    protected CaV1Curve(
      @JsonProperty(value = "bone", required = true)
      final String in_bone)
    {
      this.bone = NullCheck.notNull(in_bone, "Bone");
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
  public static final class CaV1CurveQuaternion extends CaV1Curve
  {
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
  public static final class CaV1CurveScale extends CaV1Curve
  {
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
  public static final class CaV1CurveTranslation extends CaV1Curve
  {
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
  public static final class CaV1KeyframeCurveOrientation
  {
    private final int index;
    private final CaCurveInterpolation interpolation;
    private final CaCurveEasing easing;
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
  public static final class CaV1KeyframeCurveTranslation
  {
    private final int index;
    private final CaCurveInterpolation interpolation;
    private final CaCurveEasing easing;
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
  public static final class CaV1KeyframeCurveScale
  {
    private final int index;
    private final CaCurveInterpolation interpolation;
    private final CaCurveEasing easing;
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
  public static final class CaV1Skeleton
  {
    private final String name;
    private final List<CaV1Bone> bones;
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
     * @return A skeleton
     */

    public CaDefinitionSkeletonType toSkeleton()
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
}
