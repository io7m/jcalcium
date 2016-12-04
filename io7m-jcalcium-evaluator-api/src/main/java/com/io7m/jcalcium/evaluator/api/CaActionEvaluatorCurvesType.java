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

import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurvesType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jtensors.Quaternion4DType;
import com.io7m.jtensors.VectorWritable3DType;
import com.io7m.jtensors.parameterized.PVectorWritable3DType;

/**
 * An evaluator for a single curve-based action.
 */

public interface CaActionEvaluatorCurvesType
{
  /**
   * @return The skeleton targeted by the evaluator
   */

  CaSkeleton skeleton();

  /**
   * @return The action being evaluated
   */

  CaActionCurvesType action();

  /**
   * Evaluate the translation of the bone with ID {@code bone_id} at time
   * {@code time}, writing the resulting value to {@code out}.
   *
   * @param bone_id The bone ID
   * @param time    The current time
   * @param out     The output value
   */

  void evaluateTranslation(
    int bone_id,
    double time,
    PVectorWritable3DType<CaSpaceBoneParentRelativeType> out);

  /**
   * Evaluate the scale of the bone with ID {@code bone_id} at time
   * {@code time}, writing the resulting value to {@code out}.
   *
   * @param bone_id The bone ID
   * @param time    The current time
   * @param out     The output value
   */

  void evaluateScale(
    int bone_id,
    double time,
    VectorWritable3DType out);

  /**
   * Evaluate the orientation of the bone with ID {@code bone_id} at time
   * {@code time}, writing the resulting value to {@code out}.
   *
   * @param bone_id The bone ID
   * @param time    The current time
   * @param out     The output value
   */

  void evaluateOrientation(
    int bone_id,
    double time,
    Quaternion4DType out);
}
