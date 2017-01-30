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

import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jcalcium.evaluator.api.CaActionEvaluatorCurvesDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointMutableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedSkeletonMutableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorSingleType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.Quaternion4DType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.Vector3DType;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVector3DType;
import com.io7m.jtensors.parameterized.PVectorI3D;
import com.io7m.jtensors.parameterized.PVectorM3D;
import com.io7m.junreachable.UnreachableCodeException;

import static com.io7m.jfunctional.Unit.unit;

/**
 * The default implementation of the {@link CaEvaluatorSingleType} type.
 */

public final class CaEvaluatorSingleD implements CaEvaluatorSingleType
{
  private final Quaternion4DType temp_orientation;
  private final PVector3DType<CaSpaceJointType> temp_translation;
  private final Vector3DType temp_scale;
  private final CaEvaluatedSkeletonMutableDType skeleton;
  private final CaEvaluationContextType context;
  private ActionKind kind;
  private CaActionEvaluatorCurvesDType eval_curves;
  private long frame_start;
  private long frame_current;
  private double time_scale;

  private CaEvaluatorSingleD(
    final CaEvaluationContextType in_context,
    final CaEvaluatedSkeletonMutableDType in_skeleton,
    final CaActionType in_action,
    final int global_fps)
  {
    this.context = NullCheck.notNull(in_context, "Context");
    this.skeleton = NullCheck.notNull(in_skeleton, "Skeleton");
    NullCheck.notNull(in_action, "Action");

    this.temp_orientation = new QuaternionM4D();
    this.temp_translation = new PVectorM3D<>();
    this.temp_scale = new PVectorM3D<>();

    in_action.matchAction(this, (t, curves) -> {
      t.kind = ActionKind.ACTION_CURVES;
      t.eval_curves = CaActionEvaluatorCurves.createD(
        t.context,
        t.skeleton.restPose().skeleton(),
        curves,
        global_fps);
      return unit();
    });
  }

  /**
   * Create a new single-action evaluator.
   *
   * @param in_context  An evaluation context
   * @param in_skeleton The evaluated skeleton
   * @param in_action   The action
   * @param global_fps  The global FPS rate
   *
   * @return An evaluator
   */

  public static CaEvaluatorSingleType create(
    final CaEvaluationContextType in_context,
    final CaEvaluatedSkeletonMutableDType in_skeleton,
    final CaActionType in_action,
    final int global_fps)
  {
    return new CaEvaluatorSingleD(
      in_context, in_skeleton, in_action, global_fps);
  }

  @Override
  public void evaluateForGlobalFrame(
    final long in_frame_start,
    final long in_frame_current,
    final double in_time_scale)
  {
    switch (this.kind) {
      case ACTION_CURVES: {
        this.evaluateCurves(in_frame_start, in_frame_current, in_time_scale);
        return;
      }
    }

    throw new UnreachableCodeException();
  }

  private Unit evaluateCurves(
    final long in_frame_start,
    final long in_frame_current,
    final double in_time_scale)
  {
    this.frame_start = in_frame_start;
    this.frame_current = in_frame_current;
    this.time_scale = in_time_scale;

    this.skeleton.jointsMutable().forEachBreadthFirst(
      this, (t, depth, node) -> {

        final CaEvaluatedJointMutableDType joint = node.value();
        t.eval_curves.evaluateOrientation4DForGlobalFrame(
          joint.id(),
          t.frame_start,
          t.frame_current,
          t.time_scale,
          t.temp_orientation);
        joint.setOrientation(new QuaternionI4D(t.temp_orientation));

        t.eval_curves.evaluateTranslation3DForGlobalFrame(
          joint.id(),
          t.frame_start,
          t.frame_current,
          t.time_scale,
          t.temp_translation);
        joint.setTranslation3D(new PVectorI3D<>(t.temp_translation));

        t.eval_curves.evaluateScale3DForGlobalFrame(
          joint.id(),
          t.frame_start,
          t.frame_current,
          t.time_scale,
          t.temp_scale);
        joint.setScale(new VectorI3D(t.temp_scale));
      });

    return unit();
  }

  private enum ActionKind
  {
    ACTION_CURVES
  }
}
