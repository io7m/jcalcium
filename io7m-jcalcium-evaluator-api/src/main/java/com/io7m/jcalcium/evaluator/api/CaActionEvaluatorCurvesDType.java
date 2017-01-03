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

package com.io7m.jcalcium.evaluator.api;

import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jtensors.Quaternion4DType;
import com.io7m.jtensors.VectorWritable3DType;
import com.io7m.jtensors.parameterized.PVectorWritable3DType;

/**
 * <p>An evaluator for a single curve-based action that yields double-precision
 * results.</p>
 */

public interface CaActionEvaluatorCurvesDType extends
  CaActionEvaluatorCurvesType
{
  /**
   * Evaluate the translation of the joint with ID {@code joint_id} at global
   * frame {@code frame}, writing the resulting value to {@code out}. The
   * action is assumed to have started at {@code frame_start}.
   *
   * @param joint_id      The joint ID
   * @param frame_start   The global frame at which the action is assumed to
   *                      have started
   * @param frame_current The current global frame
   * @param time_scale    The animation time scale
   * @param out           The output value
   */

  void evaluateTranslation3DForGlobalFrame(
    final int joint_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final PVectorWritable3DType<CaSpaceJointType> out);

  /**
   * Evaluate the scale of the joint with ID {@code joint_id} at frame
   * {@code frame}, writing the resulting value to {@code out}. The
   * action is assumed to have started at {@code frame_start}.
   *
   * @param joint_id      The joint ID
   * @param frame_start   The global frame at which the action is assumed to
   *                      have started
   * @param frame_current The current global frame
   * @param time_scale    The animation time scale
   * @param out           The output value
   */

  void evaluateScale3DForGlobalFrame(
    final int joint_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final VectorWritable3DType out);

  /**
   * Evaluate the orientation of the joint with ID {@code joint_id} at global
   * frame {@code frame}, writing the resulting value to {@code out}.
   *
   * @param joint_id      The joint ID
   * @param frame_start   The global frame at which the action is assumed to
   *                      have started
   * @param frame_current The current global frame
   * @param time_scale    The animation time scale
   * @param out           The output value
   */

  void evaluateOrientation4DForGlobalFrame(
    final int joint_id,
    final long frame_start,
    final long frame_current,
    final double time_scale,
    final Quaternion4DType out);
}
