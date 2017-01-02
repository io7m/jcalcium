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

package com.io7m.jcalcium.evaluator.main;

import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.compiled.CaBone;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurvesScaling;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurvesType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeScale;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.evaluator.api.CaActionEvaluatorCurvesDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorInterpolation;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jtensors.Quaternion4DType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.VectorWritable3DType;
import com.io7m.jtensors.parameterized.PVectorReadable3DType;
import com.io7m.jtensors.parameterized.PVectorWritable3DType;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import javaslang.collection.IndexedSeq;
import javaslang.collection.SortedMap;
import javaslang.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.io7m.jfunctional.Unit.unit;

/**
 * The default implementation of the {@link CaActionEvaluatorCurvesDType}.
 */

public final class CaActionEvaluatorCurves
  implements CaActionEvaluatorCurvesDType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaActionEvaluatorCurves.class);
  }

  private final BoneTracks[] bone_tracks;

  private CaActionEvaluatorCurves(
    final CaSkeleton in_skeleton,
    final CaActionCurvesType in_action,
    final int global_fps)
  {
    NullCheck.notNull(in_skeleton, "Skeleton");
    NullCheck.notNull(in_action, "Action");

    final CaActionCurvesType action_scaled =
      CaActionCurvesScaling.scale(in_action, global_fps);

    if (LOG.isDebugEnabled()) {
      LOG.debug(
        "instantiating bone tracks for action {}",
        action_scaled.name().value());
    }

    final SortedMap<Integer, JOTreeNodeReadableType<CaBone>> by_id =
      in_skeleton.bonesByID();

    this.bone_tracks = new BoneTracks[by_id.size()];
    for (final Integer bone_id : by_id.keySet()) {
      final JOTreeNodeReadableType<CaBone> node = by_id.get(bone_id).get();
      final CaBone bone = node.value();
      final CaBoneName bone_name = bone.name();
      final Option<IndexedSeq<CaCurveType>> curves_opt =
        action_scaled.curves().get(bone_name);

      final BoneTracks current_tracks =
        new BoneTracks(
          bone,
          new Int2ReferenceRBTreeMap<>(),
          new Int2ReferenceRBTreeMap<>(),
          new Int2ReferenceRBTreeMap<>());

      if (curves_opt.isDefined()) {
        current_tracks.populate(curves_opt.get());
      }

      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "bone [{}] {} keyframes translation",
          bone_id,
          Integer.valueOf(current_tracks.keyframes_translation.size()));
        LOG.trace(
          "bone [{}] {} keyframes scale",
          bone_id,
          Integer.valueOf(current_tracks.keyframes_scale.size()));
        LOG.trace(
          "bone [{}] {} keyframes orientation",
          bone_id,
          Integer.valueOf(current_tracks.keyframes_orientation.size()));
      }

      this.bone_tracks[bone_id.intValue()] = current_tracks;
    }

    /*
     * Check that all bone tracks exist. This can only fail if the compiler
     * fails to assign monotonically increasing numbers to bones.
     */

    for (int index = 0; index < this.bone_tracks.length; ++index) {
      NullCheck.notNull(this.bone_tracks[index], "this.bone_tracks[index]");
    }
  }

  /**
   * Create a new evaluator for the given skeleton and action.
   *
   * @param in_skeleton The skeleton
   * @param in_action   The action
   * @param global_fps  The global FPS rate
   *
   * @return A new evaluator
   */

  public static CaActionEvaluatorCurvesDType createD(
    final CaSkeleton in_skeleton,
    final CaActionCurvesType in_action,
    final int global_fps)
  {
    return new CaActionEvaluatorCurves(in_skeleton, in_action, global_fps);
  }

  @Override
  public void evaluateTranslation3DForGlobalFrame(
    final int bone_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final PVectorWritable3DType<CaSpaceBoneParentRelativeType> out)
  {
    this.bone_tracks[bone_id].evaluateTranslation3D(
      frame_start, frame_current, time_scale, out);
  }

  @Override
  public void evaluateScale3DForGlobalFrame(
    final int bone_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final VectorWritable3DType out)
  {
    this.bone_tracks[bone_id].evaluateScale3D(
      frame_start, frame_current, time_scale, out);
  }

  @Override
  public void evaluateOrientation4DForGlobalFrame(
    final int bone_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final Quaternion4DType out)
  {
    this.bone_tracks[bone_id].evaluateOrientation4D(
      frame_start, frame_current, time_scale, out);
  }

  private static final class BoneTracks
  {
    private final Int2ReferenceRBTreeMap<CaCurveKeyframeTranslation> keyframes_translation;
    private final Int2ReferenceRBTreeMap<CaCurveKeyframeOrientation> keyframes_orientation;
    private final Int2ReferenceRBTreeMap<CaCurveKeyframeScale> keyframes_scale;
    private final CaBone bone;
    private int last_frame;

    BoneTracks(
      final CaBone in_bone,
      final Int2ReferenceRBTreeMap<CaCurveKeyframeTranslation> in_keyframes_translation,
      final Int2ReferenceRBTreeMap<CaCurveKeyframeOrientation> in_keyframes_orientation,
      final Int2ReferenceRBTreeMap<CaCurveKeyframeScale> in_keyframes_scale)
    {
      this.bone = in_bone;
      this.keyframes_translation = in_keyframes_translation;
      this.keyframes_orientation = in_keyframes_orientation;
      this.keyframes_scale = in_keyframes_scale;
      this.last_frame = 0;
    }

    private static int keyframeIndexNext(
      final IntSortedSet keys,
      final int frame)
    {
      final IntSortedSet tails = keys.tailSet(Math.addExact(frame, 1));
      if (tails.isEmpty()) {
        return -1;
      }

      final int result = tails.firstInt();
      Postconditions.checkPostconditionI(
        result,
        frame < result,
        i -> "Keyframe index + " + i + " must be >= current index");
      return result;
    }

    private static int keyframeIndexCurrent(
      final IntSortedSet keys,
      final int frame)
    {
      final IntSortedSet heads = keys.headSet(Math.addExact(frame, 1));
      if (heads.isEmpty()) {
        return -1;
      }

      final int result = heads.lastInt();
      Postconditions.checkPostconditionI(
        result,
        frame >= result,
        i -> "Keyframe index " + i + " must be < current index");
      return result;
    }

    private static double calculateMix(
      final double curr,
      final double prev,
      final double next)
    {
      Preconditions.checkPreconditionD(
        curr,
        curr >= prev,
        i -> "Index " + i + " must be after or on the current keyframe");
      Preconditions.checkPreconditionD(
        curr,
        curr < next,
        i -> "Index " + i + " must be before the next keyframe");

      final double mix =
        (curr - prev) / (next - prev);

      Postconditions.checkPostconditionD(
        mix,
        mix >= 0.0,
        x -> "Index " + x + " must be in the range [0, 1]");
      Postconditions.checkPostconditionD(
        mix,
        mix <= 1.0,
        x -> "Index " + x + " must be in the range [0, 1]");

      return mix;
    }

    private static int wrapFrame(
      final long frame_start,
      final long frame_current,
      final long bound)
    {
      final long sum = Math.addExact(frame_start, frame_current);
      final long mod = sum % bound;
      return Math.toIntExact(mod < 0L ? mod + bound : mod);
    }

    private void evaluateTranslation3D(
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final PVectorWritable3DType<CaSpaceBoneParentRelativeType> out)
    {
      final int bound =
        Math.addExact(this.last_frame, 1);

      final int frame_bounded =
        wrapFrame(frame_start, frame_current, (long) bound);

      int key_frame_prev = keyframeIndexCurrent(
        this.keyframes_translation.keySet(), frame_bounded);
      int key_frame_next = keyframeIndexNext(
        this.keyframes_translation.keySet(), frame_bounded);

      final PVectorReadable3DType<CaSpaceBoneParentRelativeType> trans_curr;
      final CaCurveEasing easing;
      final CaCurveInterpolation interp;
      if (key_frame_prev >= 0) {
        final CaCurveKeyframeTranslation keyf_curr =
          this.keyframes_translation.get(key_frame_prev);
        trans_curr = keyf_curr.translation();
        easing = keyf_curr.easing();
        interp = keyf_curr.interpolation();
      } else {
        trans_curr = this.bone.translation();
        easing = CaCurveEasing.CURVE_EASING_IN_OUT;
        interp = CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
        key_frame_prev = 0;
      }

      final PVectorReadable3DType<CaSpaceBoneParentRelativeType> trans_next;
      if (key_frame_next >= 0) {
        final CaCurveKeyframeTranslation keyf_next =
          this.keyframes_translation.get(key_frame_next);
        trans_next = keyf_next.translation();
      } else {
        trans_next = this.bone.translation();
        key_frame_next = Math.addExact(this.last_frame, 1);
      }

      final double mix =
        calculateMix(
          (double) frame_bounded,
          (double) key_frame_prev,
          (double) key_frame_next);

      CaEvaluatorInterpolation.interpolateVector3D(
        easing, interp, mix, trans_curr, trans_next, out);
    }

    private void evaluateScale3D(
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final VectorWritable3DType out)
    {
      final int bound =
        Math.addExact(this.last_frame, 1);

      final int frame_bounded =
        wrapFrame(frame_start, frame_current, (long) bound);

      int key_frame_prev = keyframeIndexCurrent(
        this.keyframes_scale.keySet(), frame_bounded);
      int key_frame_next = keyframeIndexNext(
        this.keyframes_scale.keySet(), frame_bounded);

      final VectorI3D val_curr;
      final CaCurveEasing easing;
      final CaCurveInterpolation interp;

      if (key_frame_prev >= 0) {
        final CaCurveKeyframeScale keyf_curr =
          this.keyframes_scale.get(key_frame_prev);
        val_curr = keyf_curr.scale();
        easing = keyf_curr.easing();
        interp = keyf_curr.interpolation();
      } else {
        val_curr = this.bone.scale();
        easing = CaCurveEasing.CURVE_EASING_IN_OUT;
        interp = CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
        key_frame_prev = 0;
      }

      final VectorI3D val_next;
      if (key_frame_next >= 0) {
        final CaCurveKeyframeScale keyf_next =
          this.keyframes_scale.get(key_frame_next);
        val_next = keyf_next.scale();
      } else {
        val_next = this.bone.scale();
        key_frame_next = Math.addExact(this.last_frame, 1);
      }

      final double mix =
        calculateMix(
          (double) frame_bounded,
          (double) key_frame_prev,
          (double) key_frame_next);

      CaEvaluatorInterpolation.interpolateVector3D(
        easing, interp, mix, val_curr, val_next, out);
    }

    private void evaluateOrientation4D(
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final Quaternion4DType out)
    {
      final int bound =
        Math.addExact(this.last_frame, 1);

      final int frame_bounded =
        wrapFrame(frame_start, frame_current, (long) bound);

      int key_frame_prev = keyframeIndexCurrent(
        this.keyframes_orientation.keySet(), frame_bounded);
      int key_frame_next = keyframeIndexNext(
        this.keyframes_orientation.keySet(), frame_bounded);

      final QuaternionI4D val_curr;
      final CaCurveEasing easing;
      final CaCurveInterpolation interp;

      if (key_frame_prev >= 0) {
        final CaCurveKeyframeOrientation kf =
          this.keyframes_orientation.get(key_frame_prev);
        val_curr = kf.orientation();
        easing = kf.easing();
        interp = kf.interpolation();
      } else {
        val_curr = this.bone.orientation();
        easing = CaCurveEasing.CURVE_EASING_IN_OUT;
        interp = CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
        key_frame_prev = 0;
      }

      final QuaternionI4D val_next;
      if (key_frame_next >= 0) {
        final CaCurveKeyframeOrientation kf =
          this.keyframes_orientation.get(key_frame_next);
        val_next = kf.orientation();
      } else {
        val_next = this.bone.orientation();
        key_frame_next = bound;
      }

      final double mix =
        calculateMix(
          (double) frame_bounded,
          (double) key_frame_prev,
          (double) key_frame_next);

      CaEvaluatorInterpolation.interpolateQuaternion4D(
        easing, interp, mix, val_curr, val_next, out);
    }

    private void populate(
      final IndexedSeq<CaCurveType> curves)
    {
      this.keyframes_scale.clear();
      this.keyframes_translation.clear();
      this.keyframes_orientation.clear();

      for (int index = 0; index < curves.length(); ++index) {
        final CaCurveType curve = curves.get(index);
        curve.matchCurve(
          this,
          (t, translation) -> {
            translation.keyframes().forEach(
              p -> t.keyframes_translation.put(p._1.intValue(), p._2));
            return unit();
          },
          (t, orientation) -> {
            orientation.keyframes().forEach(
              p -> t.keyframes_orientation.put(p._1.intValue(), p._2));
            return unit();
          },
          (t, scale) -> {
            scale.keyframes().forEach(
              p -> t.keyframes_scale.put(p._1.intValue(), p._2));
            return unit();
          });
      }

      this.last_frame = 0;
      if (!this.keyframes_translation.isEmpty()) {
        this.last_frame = Math.max(
          this.keyframes_translation.lastIntKey(), this.last_frame);
      }
      if (!this.keyframes_orientation.isEmpty()) {
        this.last_frame = Math.max(
          this.keyframes_orientation.lastIntKey(), this.last_frame);
      }
      if (!this.keyframes_scale.isEmpty()) {
        this.last_frame = Math.max(
          this.keyframes_scale.lastIntKey(), this.last_frame);
      }
    }
  }
}
