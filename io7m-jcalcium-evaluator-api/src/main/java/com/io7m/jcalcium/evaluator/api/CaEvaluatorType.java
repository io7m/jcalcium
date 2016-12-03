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

import com.io7m.jorchard.core.JOTreeNodeReadableType;
import javaslang.collection.List;

/**
 * <p>The type of evaluators.</p>
 *
 * <p>An evaluator calculates the positions, orientations, and scales of all
 * bones in a skeleton based on the current set of active actions. With
 * no actions active, the skeleton remains in its resting pose.</p>
 */

public interface CaEvaluatorType
{
  /**
   * @return The tree of evaluated bones
   */

  JOTreeNodeReadableType<CaEvaluatedBoneType> evaluatedBones();

  /**
   * <p>Transition to the given set of {@code actions} over the next {@code
   * time} seconds.</p>
   *
   * <p>If {@code actions} is empty, the skeleton will transition to the
   * resting pose.</p>
   *
   * @param actions The target set of actions
   * @param time    The transition period
   */

  void evaluateTransitionTo(
    List<CaEvaluatorWeightedAction> actions,
    double time);

  /**
   * Evaluate all bones assuming the current time is {@code time}.
   *
   * @param time The current time in seconds
   */

  void evaluate(double time);
}
