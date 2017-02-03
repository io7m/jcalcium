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

import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.CaJointName;
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
import com.io7m.jcalcium.evaluator.api.CaActionKeyframeCurrent;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextType;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextVectorsType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorInterpolation;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jtensors.Quaternion4DType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.VectorM3D;
import com.io7m.jtensors.VectorWritable3DType;
import com.io7m.jtensors.parameterized.PVectorReadable3DType;
import com.io7m.jtensors.parameterized.PVectorWritable3DType;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
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
  private final CaEvaluationContextType context;

  private CaActionEvaluatorCurves(
    final CaEvaluationContextType in_context,
    final CaSkeleton in_skeleton,
    final CaActionCurvesType in_action,
    final int global_fps)
  {
    this.context = NullCheck.notNull(in_context, "Context");
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

      final JointTracks current_tracks;
      if (curves_opt.isDefined()) {
        current_tracks = JointTracks.createFromCurves(joint, curves_opt.get());
      } else {
        current_tracks = new JointTracks(
          joint,
          new Int2ReferenceRBTreeMap<>(),
          new Int2ReferenceRBTreeMap<>(),
          new Int2ReferenceRBTreeMap<>(),
          0);
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
      NullCheck.notNull(this.joint_tracks[index], "Joint track");
    }
  }

  /**
   * Create a new evaluator for the given skeleton and action.
   *
   * @param in_context  An evaluation context
   * @param in_skeleton The skeleton
   * @param in_action   The action
   * @param global_fps  The global FPS rate
   *
   * @return A new evaluator
   */

  public static CaActionEvaluatorCurvesDType createD(
    final CaEvaluationContextType in_context,
    final CaSkeleton in_skeleton,
    final CaActionCurvesType in_action,
    final int global_fps)
  {
    return new CaActionEvaluatorCurves(
      in_context,
      in_skeleton,
      in_action,
      global_fps);
  }

  @Override
  public void evaluateTranslation3DForGlobalFrame(
    final int joint_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final PVectorWritable3DType<CaSpaceJointType> out)
  {
    try (final CaEvaluationContextVectorsType v = this.context.newVectors()) {
      this.joint_tracks[joint_id].evaluateTranslation3D(
        v.vectorContext3D(), frame_start, frame_current, time_scale, out);
    }
  }

  @Override
  public void evaluateScale3DForGlobalFrame(
    final int joint_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final VectorWritable3DType out)
  {
    try (final CaEvaluationContextVectorsType v = this.context.newVectors()) {
      this.joint_tracks[joint_id].evaluateScale3D(
        v.vectorContext3D(), frame_start, frame_current, time_scale, out);
    }
  }

  @Override
  public void evaluateOrientation4DForGlobalFrame(
    final int joint_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final Quaternion4DType out)
  {
    try (final CaEvaluationContextVectorsType v = this.context.newVectors()) {
      this.joint_tracks[joint_id].evaluateOrientation4D(
        v.quaternionContext4D(), frame_start, frame_current, time_scale, out);
    }
  }

  private static final class JointTracks
  {
    private final Int2ReferenceRBTreeMap<CaCurveKeyframeTranslation> keyframes_translation;
    private final Int2ReferenceRBTreeMap<CaCurveKeyframeOrientation> keyframes_orientation;
    private final Int2ReferenceRBTreeMap<CaCurveKeyframeScale> keyframes_scale;
    private final @Nullable CaActionTimeline timeline_translation;
    private final @Nullable CaActionTimeline timeline_orientation;
    private final @Nullable CaActionTimeline timeline_scale;
    private final CaJoint joint;

    private JointTracks(
      final CaJoint in_joint,
      final Int2ReferenceRBTreeMap<CaCurveKeyframeTranslation> in_keyframes_translation,
      final Int2ReferenceRBTreeMap<CaCurveKeyframeOrientation> in_keyframes_orientation,
      final Int2ReferenceRBTreeMap<CaCurveKeyframeScale> in_keyframes_scale,
      final int in_last_frame)
    {
      this.joint = in_joint;

      this.keyframes_translation = in_keyframes_translation;
      this.keyframes_orientation = in_keyframes_orientation;
      this.keyframes_scale = in_keyframes_scale;

      if (!in_keyframes_translation.isEmpty()) {
        this.timeline_translation = new CaActionTimeline(
          in_keyframes_translation.keySet(), in_last_frame);
      } else {
        this.timeline_translation = null;
      }

      if (!in_keyframes_orientation.isEmpty()) {
        this.timeline_orientation = new CaActionTimeline(
          in_keyframes_orientation.keySet(), in_last_frame);
      } else {
        this.timeline_orientation = null;
      }

      if (!in_keyframes_scale.isEmpty()) {
        this.timeline_scale = new CaActionTimeline(
          in_keyframes_scale.keySet(), in_last_frame);
      } else {
        this.timeline_scale = null;
      }
    }

    private void evaluateTranslation3D(
      final VectorM3D.ContextVM3D c,
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final PVectorWritable3DType<CaSpaceJointType> out)
    {
      if (this.timeline_translation != null) {
        final CaActionKeyframeCurrent r =
          this.timeline_translation.keyframeCurrent(
            frame_start,
            frame_current,
            time_scale);

        final CaCurveKeyframeTranslation kf_prev =
          this.keyframes_translation.get(r.keyframePrevious());
        final CaCurveKeyframeTranslation kf_next =
          this.keyframes_translation.get(r.keyframeNext());

        final CaCurveEasing easing = kf_prev.easing();
        final CaCurveInterpolation interp = kf_prev.interpolation();
        final PVectorReadable3DType<CaSpaceJointType> trans_prev =
          kf_prev.translation();
        final PVectorReadable3DType<CaSpaceJointType> trans_next =
          kf_next.translation();

        CaEvaluatorInterpolation.interpolateVector3D(
          c, easing, interp, r.progress(), trans_prev, trans_next, out);
      } else {
        out.copyFromTyped3D(this.joint.translation());
      }
    }

    private void evaluateScale3D(
      final VectorM3D.ContextVM3D c,
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final VectorWritable3DType out)
    {
      if (this.timeline_scale != null) {
        final CaActionKeyframeCurrent r =
          this.timeline_scale.keyframeCurrent(
            frame_start,
            frame_current,
            time_scale);

        final CaCurveKeyframeScale kf_prev =
          this.keyframes_scale.get(r.keyframePrevious());
        final CaCurveKeyframeScale kf_next =
          this.keyframes_scale.get(r.keyframeNext());

        final CaCurveEasing easing = kf_prev.easing();
        final CaCurveInterpolation interp = kf_prev.interpolation();
        final VectorI3D val_prev = kf_prev.scale();
        final VectorI3D val_next = kf_next.scale();

        CaEvaluatorInterpolation.interpolateVector3D(
          c, easing, interp, r.progress(), val_prev, val_next, out);
      } else {
        out.copyFrom3D(this.joint.scale());
      }
    }

    private void evaluateOrientation4D(
      final QuaternionM4D.ContextQM4D c,
      final long frame_start,
      final long frame_current,
      final double time_scale,
      final Quaternion4DType out)
    {
      if (this.timeline_orientation != null) {
        final CaActionKeyframeCurrent r =
          this.timeline_orientation.keyframeCurrent(
            frame_start,
            frame_current,
            time_scale);

        final CaCurveKeyframeOrientation kf_prev =
          this.keyframes_orientation.get(r.keyframePrevious());
        final CaCurveKeyframeOrientation kf_next =
          this.keyframes_orientation.get(r.keyframeNext());

        final CaCurveEasing easing = kf_prev.easing();
        final CaCurveInterpolation interp = kf_prev.interpolation();
        final QuaternionI4D val_prev = kf_prev.orientation();
        final QuaternionI4D val_next = kf_next.orientation();

        CaEvaluatorInterpolation.interpolateQuaternion4D(
          c, easing, interp, r.progress(), val_prev, val_next, out);
      } else {
        out.copyFrom4D(this.joint.orientation());
      }
    }

    public static JointTracks createFromCurves(
      final CaJoint in_joint,
      final IndexedSeq<CaCurveType> curves)
    {
      final Int2ReferenceRBTreeMap<CaCurveKeyframeScale> keyframes_scale =
        new Int2ReferenceRBTreeMap<>();
      final Int2ReferenceRBTreeMap<CaCurveKeyframeTranslation> keyframes_translation =
        new Int2ReferenceRBTreeMap<>();
      final Int2ReferenceRBTreeMap<CaCurveKeyframeOrientation> keyframes_orientation =
        new Int2ReferenceRBTreeMap<>();

      for (int index = 0; index < curves.length(); ++index) {
        final CaCurveType curve = curves.get(index);
        curve.matchCurve(
          unit(),
          (t, translation) -> {
            translation.keyframes().forEach(
              p -> keyframes_translation.put(p._1.intValue(), p._2));
            return unit();
          },
          (t, orientation) -> {
            orientation.keyframes().forEach(
              p -> keyframes_orientation.put(p._1.intValue(), p._2));
            return unit();
          },
          (t, scale) -> {
            scale.keyframes().forEach(
              p -> keyframes_scale.put(p._1.intValue(), p._2));
            return unit();
          });
      }

      int last_frame = 0;
      if (!keyframes_translation.isEmpty()) {
        last_frame = Math.max(
          keyframes_translation.lastIntKey(), last_frame);
      }
      if (!keyframes_orientation.isEmpty()) {
        last_frame = Math.max(
          keyframes_orientation.lastIntKey(), last_frame);
      }
      if (!keyframes_scale.isEmpty()) {
        last_frame = Math.max(
          keyframes_scale.lastIntKey(), last_frame);
      }

      return new JointTracks(
        in_joint,
        keyframes_translation,
        keyframes_orientation,
        keyframes_scale,
        last_frame);
    }
  }
}
