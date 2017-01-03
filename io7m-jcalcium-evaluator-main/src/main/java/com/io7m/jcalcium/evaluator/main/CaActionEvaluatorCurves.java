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
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurvesScaling;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurvesType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeScale;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveType;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
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
import com.io7m.junsigned.core.UnsignedDouble;
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

  private final JointTracks[] joint_tracks;

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
        "instantiating joint tracks for action {}",
        action_scaled.name().value());
    }

    final SortedMap<Integer, JOTreeNodeReadableType<CaJoint>> by_id =
      in_skeleton.jointsByID();

    this.joint_tracks = new JointTracks[by_id.size()];
    for (final Integer joint_id : by_id.keySet()) {
      final JOTreeNodeReadableType<CaJoint> node = by_id.get(joint_id).get();
      final CaJoint joint = node.value();
      final CaJointName joint_name = joint.name();
      final Option<IndexedSeq<CaCurveType>> curves_opt =
        action_scaled.curves().get(joint_name);

      final JointTracks current_tracks =
        new JointTracks(
          joint,
          new Int2ReferenceRBTreeMap<>(),
          new Int2ReferenceRBTreeMap<>(),
          new Int2ReferenceRBTreeMap<>());

      if (curves_opt.isDefined()) {
        current_tracks.populate(curves_opt.get());
      }

      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "joint [{}] {} keyframes translation",
          joint_id,
          Integer.valueOf(current_tracks.keyframes_translation.size()));
        LOG.trace(
          "joint [{}] {} keyframes scale",
          joint_id,
          Integer.valueOf(current_tracks.keyframes_scale.size()));
        LOG.trace(
          "joint [{}] {} keyframes orientation",
          joint_id,
          Integer.valueOf(current_tracks.keyframes_orientation.size()));
      }

      this.joint_tracks[joint_id.intValue()] = current_tracks;
    }

    /*
     * Check that all joint tracks exist. This can only fail if the compiler
     * fails to assign monotonically increasing numbers to joints.
     */

    for (int index = 0; index < this.joint_tracks.length; ++index) {
      NullCheck.notNull(this.joint_tracks[index], "this.joint_tracks[index]");
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
    final int joint_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final PVectorWritable3DType<CaSpaceJointType> out)
  {
    this.joint_tracks[joint_id].evaluateTranslation3D(
      frame_start, frame_current, time_scale, out);
  }

  @Override
  public void evaluateScale3DForGlobalFrame(
    final int joint_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final VectorWritable3DType out)
  {
    this.joint_tracks[joint_id].evaluateScale3D(
      frame_start, frame_current, time_scale, out);
  }

  @Override
  public void evaluateOrientation4DForGlobalFrame(
    final int joint_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final Quaternion4DType out)
  {
    this.joint_tracks[joint_id].evaluateOrientation4D(
      frame_start, frame_current, time_scale, out);
  }

  private static final class JointTracks
  {
    private final Int2ReferenceRBTreeMap<CaCurveKeyframeTranslation> keyframes_translation;
    private final Int2ReferenceRBTreeMap<CaCurveKeyframeOrientation> keyframes_orientation;
    private final Int2ReferenceRBTreeMap<CaCurveKeyframeScale> keyframes_scale;
    private final CaJoint joint;
    private int last_frame;

    JointTracks(
      final CaJoint in_joint,
      final Int2ReferenceRBTreeMap<CaCurveKeyframeTranslation> in_keyframes_translation,
      final Int2ReferenceRBTreeMap<CaCurveKeyframeOrientation> in_keyframes_orientation,
      final Int2ReferenceRBTreeMap<CaCurveKeyframeScale> in_keyframes_scale)
    {
      this.joint = in_joint;
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

    private static double calculateFrame(
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final int bound)
    {
      final long frame_local =
        Math.subtractExact(frame_current, frame_start);
      final double frame_scaled =
        (double) frame_local * time_scale;
      final double result =
        UnsignedDouble.modulo(frame_scaled, (double) bound);
      return result;
    }

    private void evaluateTranslation3D(
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final PVectorWritable3DType<CaSpaceJointType> out)
    {
      final int bound = Math.addExact(this.last_frame, 1);

      final double frame =
        calculateFrame(frame_start, frame_current, time_scale, bound);
      int key_frame_prev = keyframeIndexCurrent(
        this.keyframes_translation.keySet(), (int) Math.floor(frame));
      int key_frame_next = keyframeIndexNext(
        this.keyframes_translation.keySet(), (int) Math.floor(frame));

      final PVectorReadable3DType<CaSpaceJointType> trans_prev;
      final CaCurveEasing easing;
      final CaCurveInterpolation interp;
      if (key_frame_prev >= 0) {
        final CaCurveKeyframeTranslation kf =
          this.keyframes_translation.get(key_frame_prev);
        trans_prev = kf.translation();
        easing = kf.easing();
        interp = kf.interpolation();
      } else {
        trans_prev = this.joint.translation();
        easing = CaCurveEasing.CURVE_EASING_IN_OUT;
        interp = CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
        key_frame_prev = 0;
      }

      final PVectorReadable3DType<CaSpaceJointType> trans_next;
      if (key_frame_next >= 0) {
        final CaCurveKeyframeTranslation kf =
          this.keyframes_translation.get(key_frame_next);
        trans_next = kf.translation();
      } else {
        trans_next = this.joint.translation();
        key_frame_next = bound;
      }

      final double mix =
        calculateMix(frame, (double) key_frame_prev, (double) key_frame_next);
      CaEvaluatorInterpolation.interpolateVector3D(
        easing, interp, mix, trans_prev, trans_next, out);
    }

    private void evaluateScale3D(
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final VectorWritable3DType out)
    {
      final int bound = Math.addExact(this.last_frame, 1);

      final double frame =
        calculateFrame(frame_start, frame_current, time_scale, bound);
      int key_frame_prev = keyframeIndexCurrent(
        this.keyframes_scale.keySet(), (int) Math.floor(frame));
      int key_frame_next = keyframeIndexNext(
        this.keyframes_scale.keySet(), (int) Math.floor(frame));

      final VectorI3D val_prev;
      final CaCurveEasing easing;
      final CaCurveInterpolation interp;

      if (key_frame_prev >= 0) {
        final CaCurveKeyframeScale kf =
          this.keyframes_scale.get(key_frame_prev);
        val_prev = kf.scale();
        easing = kf.easing();
        interp = kf.interpolation();
      } else {
        val_prev = this.joint.scale();
        easing = CaCurveEasing.CURVE_EASING_IN_OUT;
        interp = CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
        key_frame_prev = 0;
      }

      final VectorI3D val_next;
      if (key_frame_next >= 0) {
        final CaCurveKeyframeScale kf =
          this.keyframes_scale.get(key_frame_next);
        val_next = kf.scale();
      } else {
        val_next = this.joint.scale();
        key_frame_next = bound;
      }

      final double mix =
        calculateMix(frame, (double) key_frame_prev, (double) key_frame_next);
      CaEvaluatorInterpolation.interpolateVector3D(
        easing, interp, mix, val_prev, val_next, out);
    }

    private void evaluateOrientation4D(
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final Quaternion4DType out)
    {
      final int bound = Math.addExact(this.last_frame, 1);

      final double frame =
        calculateFrame(frame_start, frame_current, time_scale, bound);
      int key_frame_prev = keyframeIndexCurrent(
        this.keyframes_orientation.keySet(), (int) Math.floor(frame));
      int key_frame_next = keyframeIndexNext(
        this.keyframes_orientation.keySet(), (int) Math.floor(frame));

      final QuaternionI4D val_prev;
      final CaCurveEasing easing;
      final CaCurveInterpolation interp;

      if (key_frame_prev >= 0) {
        final CaCurveKeyframeOrientation kf =
          this.keyframes_orientation.get(key_frame_prev);
        val_prev = kf.orientation();
        easing = kf.easing();
        interp = kf.interpolation();
      } else {
        val_prev = this.joint.orientation();
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
        val_next = this.joint.orientation();
        key_frame_next = bound;
      }

      final double mix =
        calculateMix(frame, (double) key_frame_prev, (double) key_frame_next);
      CaEvaluatorInterpolation.interpolateQuaternion4D(
        easing, interp, mix, val_prev, val_next, out);
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
