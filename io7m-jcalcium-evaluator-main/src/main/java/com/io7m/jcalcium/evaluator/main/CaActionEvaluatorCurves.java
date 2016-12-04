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

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.compiled.CaBone;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurvesType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeScale;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.evaluator.api.CaActionEvaluatorCurvesType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorInterpolation;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jtensors.Quaternion4DType;
import com.io7m.jtensors.QuaternionReadable4DType;
import com.io7m.jtensors.VectorReadable3DType;
import com.io7m.jtensors.VectorWritable3DType;
import com.io7m.jtensors.parameterized.PVectorReadable3DType;
import com.io7m.jtensors.parameterized.PVectorWritable3DType;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.doubles.DoubleSortedSet;
import javaslang.collection.IndexedSeq;
import javaslang.collection.SortedMap;
import javaslang.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.io7m.jcalcium.evaluator.api.CaEvaluatorInterpolation.interpolateVector3D;
import static com.io7m.jfunctional.Unit.unit;

/**
 * The default implementation of the {@link CaActionEvaluatorCurvesType}.
 */

public final class CaActionEvaluatorCurves
  implements CaActionEvaluatorCurvesType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaActionEvaluatorCurves.class);
  }

  private final CaSkeleton skeleton;
  private final CaActionCurvesType action;
  private final BoneTracks[] bone_tracks;
  private double eval_time;

  private CaActionEvaluatorCurves(
    final CaSkeleton in_skeleton,
    final CaActionCurvesType in_action)
  {
    this.skeleton = NullCheck.notNull(in_skeleton, "Skeleton");
    this.action = NullCheck.notNull(in_action, "Action");

    if (LOG.isDebugEnabled()) {
      LOG.debug(
        "instantiating bone tracks for action {}",
        in_action.name().value());
    }

    final SortedMap<Integer, JOTreeNodeReadableType<CaBone>> by_id =
      this.skeleton.bonesByID();

    this.bone_tracks = new BoneTracks[by_id.size()];
    for (final Integer bone_id : by_id.keySet()) {
      final JOTreeNodeReadableType<CaBone> node = by_id.get(bone_id).get();
      final CaBone bone = node.value();
      final CaBoneName bone_name = bone.name();
      final Option<IndexedSeq<CaCurveType>> curves_opt =
        this.action.curves().get(bone_name);

      final BoneTracks current_tracks =
        new BoneTracks(
          bone,
          new Double2ReferenceRBTreeMap<>(),
          new Double2ReferenceRBTreeMap<>(),
          new Double2ReferenceRBTreeMap<>());

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
   *
   * @return A new evaluator
   */

  public static CaActionEvaluatorCurvesType create(
    final CaSkeleton in_skeleton,
    final CaActionCurvesType in_action)
  {
    return new CaActionEvaluatorCurves(in_skeleton, in_action);
  }

  @Override
  public CaSkeleton skeleton()
  {
    return this.skeleton;
  }

  @Override
  public CaActionCurvesType action()
  {
    return this.action;
  }

  @Override
  public void evaluateTranslation(
    final int bone_id,
    final double time,
    final PVectorWritable3DType<CaSpaceBoneParentRelativeType> out)
  {
    this.bone_tracks[bone_id].evaluateTranslation(time, out);
  }

  @Override
  public void evaluateScale(
    final int bone_id,
    final double time,
    final VectorWritable3DType out)
  {
    this.bone_tracks[bone_id].evaluateScale(time, out);
  }

  @Override
  public void evaluateOrientation(
    final int bone_id,
    final double time,
    final Quaternion4DType out)
  {
    this.bone_tracks[bone_id].evaluateOrientation(time, out);
  }

  private static final class BoneTracks
  {
    private final Double2ReferenceRBTreeMap<CaCurveKeyframeTranslation> keyframes_translation;
    private final Double2ReferenceRBTreeMap<CaCurveKeyframeOrientation> keyframes_orientation;
    private final Double2ReferenceRBTreeMap<CaCurveKeyframeScale> keyframes_scale;
    private final CaBone bone;
    private double last_frame;

    BoneTracks(
      final CaBone in_bone,
      final Double2ReferenceRBTreeMap<CaCurveKeyframeTranslation> in_keyframes_translation,
      final Double2ReferenceRBTreeMap<CaCurveKeyframeOrientation> in_keyframes_orientation,
      final Double2ReferenceRBTreeMap<CaCurveKeyframeScale> in_keyframes_scale)
    {
      this.bone = in_bone;
      this.keyframes_translation = in_keyframes_translation;
      this.keyframes_orientation = in_keyframes_orientation;
      this.keyframes_scale = in_keyframes_scale;
    }

    private static double mod(
      final double x,
      final double y)
    {
      final double result = x % y;
      return result < 0.0 ? result + y : result;
    }

    private void evaluateTranslation(
      final double time,
      final PVectorWritable3DType<CaSpaceBoneParentRelativeType> out)
    {
      final double index = mod(time, this.last_frame + 1.0);
      final DoubleSortedSet keys = this.keyframes_translation.keySet();

      /*
       * Determine the set of frames that are strictly "less than the next"
       * index, and then pick the last one. The picked index is the current
       * index.
       */

      final DoubleSortedSet keys_curr = keys.headSet(index + 1.0);
      final PVectorReadable3DType<CaSpaceBoneParentRelativeType> trans_curr;
      final double index_curr;
      final CaCurveEasing easing;
      final CaCurveInterpolation interp;
      if (!keys_curr.isEmpty()) {
        index_curr = keys_curr.lastDouble();
        final CaCurveKeyframeTranslation keyf_curr =
          this.keyframes_translation.get(index_curr);
        trans_curr = keyf_curr.translation();
        easing = keyf_curr.easing();
        interp = keyf_curr.interpolation();
      } else {
        trans_curr = this.bone.translation();
        easing = CaCurveEasing.CURVE_EASING_IN_OUT;
        interp = CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
        index_curr = 0.0;
      }

      /*
       * Determine the set of frames that are strictly greater than the
       * current index, and then pick the first. The picked index is the
       * next index.
       */

      final DoubleSortedSet keys_next = keys.tailSet(index + 1.0);
      final PVectorReadable3DType<CaSpaceBoneParentRelativeType> trans_next;
      final double index_next;
      if (!keys_next.isEmpty()) {
        index_next = keys_next.firstDouble();
        final CaCurveKeyframeTranslation keyf_next =
          this.keyframes_translation.get(index_next);
        trans_next = keyf_next.translation();
      } else {
        trans_next = this.bone.translation();
        index_next = this.last_frame + 1.0;
      }

      final double index_norm =
        (index - index_curr) / (index_next - index_curr);

      Invariants.checkInvariantD(
        index_norm,
        index_norm >= 0.0,
        x -> "Index " + x + " must be in the range [0, 1]");
      Invariants.checkInvariantD(
        index_norm,
        index_norm <= 1.0,
        x -> "Index " + x + " must be in the range [0, 1]");

      interpolateVector3D(
        easing, interp, index_norm, trans_curr, trans_next, out);
    }

    private void evaluateScale(
      final double time,
      final VectorWritable3DType out)
    {
      final double index = mod(time, this.last_frame + 1.0);
      final DoubleSortedSet keys = this.keyframes_scale.keySet();

      /*
       * Determine the set of frames that are strictly "less than the next"
       * index, and then pick the last one. The picked index is the current
       * index.
       */

      final DoubleSortedSet keys_curr = keys.headSet(index + 1.0);
      final VectorReadable3DType trans_curr;
      final double index_curr;
      final CaCurveEasing easing;
      final CaCurveInterpolation interp;
      if (!keys_curr.isEmpty()) {
        index_curr = keys_curr.lastDouble();
        final CaCurveKeyframeScale keyf_curr =
          this.keyframes_scale.get(index_curr);
        trans_curr = keyf_curr.scale();
        easing = keyf_curr.easing();
        interp = keyf_curr.interpolation();
      } else {
        trans_curr = this.bone.scale();
        easing = CaCurveEasing.CURVE_EASING_IN_OUT;
        interp = CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
        index_curr = 0.0;
      }

      /*
       * Determine the set of frames that are strictly greater than the
       * current index, and then pick the first. The picked index is the
       * next index.
       */

      final DoubleSortedSet keys_next = keys.tailSet(index + 1.0);
      final VectorReadable3DType trans_next;
      final double index_next;
      if (!keys_next.isEmpty()) {
        index_next = keys_next.firstDouble();
        final CaCurveKeyframeScale keyf_next =
          this.keyframes_scale.get(index_next);
        trans_next = keyf_next.scale();
      } else {
        trans_next = this.bone.scale();
        index_next = this.last_frame + 1.0;
      }

      final double index_norm =
        (index - index_curr) / (index_next - index_curr);

      Invariants.checkInvariantD(
        index_norm,
        index_norm >= 0.0,
        x -> "Index " + x + " must be in the range [0, 1]");
      Invariants.checkInvariantD(
        index_norm,
        index_norm <= 1.0,
        x -> "Index " + x + " must be in the range [0, 1]");

      interpolateVector3D(
        easing, interp, index_norm, trans_curr, trans_next, out);
    }

    private void evaluateOrientation(
      final double time,
      final Quaternion4DType out)
    {
      final double index = mod(time, this.last_frame + 1.0);
      final DoubleSortedSet keys = this.keyframes_orientation.keySet();

      /*
       * Determine the set of frames that are strictly "less than the next"
       * index, and then pick the last one. The picked index is the current
       * index.
       */

      final DoubleSortedSet keys_curr = keys.headSet(index + 1.0);
      final QuaternionReadable4DType quat_curr;
      final double index_curr;
      final CaCurveEasing easing;
      final CaCurveInterpolation interp;
      if (!keys_curr.isEmpty()) {
        index_curr = keys_curr.lastDouble();
        final CaCurveKeyframeOrientation keyf_curr =
          this.keyframes_orientation.get(index_curr);
        quat_curr = keyf_curr.orientation();
        easing = keyf_curr.easing();
        interp = keyf_curr.interpolation();
      } else {
        quat_curr = this.bone.orientation();
        easing = CaCurveEasing.CURVE_EASING_IN_OUT;
        interp = CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
        index_curr = 0.0;
      }

      /*
       * Determine the set of frames that are strictly greater than the
       * current index, and then pick the first. The picked index is the
       * next index.
       */

      final DoubleSortedSet keys_next = keys.tailSet(index + 1.0);
      final QuaternionReadable4DType quat_next;
      final double index_next;
      if (!keys_next.isEmpty()) {
        index_next = keys_next.firstDouble();
        final CaCurveKeyframeOrientation keyf_next =
          this.keyframes_orientation.get(index_next);
        quat_next = keyf_next.orientation();
      } else {
        quat_next = this.bone.orientation();
        index_next = this.last_frame + 1.0;
      }

      final double index_norm =
        (index - index_curr) / (index_next - index_curr);

      Invariants.checkInvariantD(
        index_norm,
        index_norm >= 0.0,
        x -> "Index " + x + " must be in the range [0, 1]");
      Invariants.checkInvariantD(
        index_norm,
        index_norm <= 1.0,
        x -> "Index " + x + " must be in the range [0, 1]");

      CaEvaluatorInterpolation.interpolateQuaternion4D(
        easing, interp, index_norm, quat_curr, quat_next, out);
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
              p -> t.keyframes_translation.put(p._1.doubleValue(), p._2));
            return unit();
          },
          (t, orientation) -> {
            orientation.keyframes().forEach(
              p -> t.keyframes_orientation.put(p._1.doubleValue(), p._2));
            return unit();
          },
          (t, scale) -> {
            scale.keyframes().forEach(
              p -> t.keyframes_scale.put(p._1.doubleValue(), p._2));
            return unit();
          });
      }

      this.last_frame = 0.0;
      if (!this.keyframes_translation.isEmpty()) {
        this.last_frame = Math.max(
          this.keyframes_translation.lastDoubleKey(), this.last_frame);
      }
      if (!this.keyframes_orientation.isEmpty()) {
        this.last_frame = Math.max(
          this.keyframes_orientation.lastDoubleKey(), this.last_frame);
      }
      if (!this.keyframes_scale.isEmpty()) {
        this.last_frame = Math.max(
          this.keyframes_scale.lastDoubleKey(), this.last_frame);
      }
    }
  }
}
