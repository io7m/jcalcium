/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
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

import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.CaImmutableStyleType;
import org.immutables.value.Value;

/**
 * Interpolated keyframe values.
 */

@CaImmutableStyleType
@Value.Immutable
public interface CaActionKeyframeCurrentType
{
  /**
   * @return The previous keyframe
   */

  @Value.Parameter
  int keyframePrevious();

  /**
   * @return The next keyframe
   */

  @Value.Parameter
  int keyframeNext();

  /**
   * The <i>fractional</i> frame is the current frame expressed in fractional
   * form. This value is only different to the <i>integral</i> frame when a
   * time scale other than {@code 1.0} is used.
   *
   * @return The current fractional frame
   */

  @Value.Parameter
  double frameCurrentFractional();

  /**
   * @return The current integral frame
   */

  @Value.Derived
  default int frameCurrentIntegral()
  {
    return (int) Math.floor(this.frameCurrentFractional());
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    final int prev = this.keyframePrevious();
    Preconditions.checkPreconditionI(
      prev,
      prev >= 0,
      i -> "Previous keyframe must be non-negative");

    final int next = this.keyframeNext();
    Preconditions.checkPreconditionI(
      next,
      next >= 0,
      i -> "Next keyframe must be non-negative");

    final double p = this.progress();
    Postconditions.checkPostconditionD(
      p,
      p >= 0.0,
      x -> "Progress must be in the range [0, 1]");
    Postconditions.checkPostconditionD(
      p,
      p <= 1.0,
      x -> "Progress must be in the range [0, 1]");
  }

  /**
   * The progress value is a value in the range {@code [0.0, 1.0]} that
   * indicates how far between the previous and next keyframe the current frame
   * lies. This value can then be used to interpolate between the values stored
   * in each keyframe.
   *
   * @return The current progress
   */

  @Value.Derived
  default double progress()
  {
    final double curr = this.frameCurrentFractional();
    final double prev = (double) this.keyframePrevious();
    final double next = (double) this.keyframeNext();
    final double nmp = next - prev;
    if (nmp == 0.0) {
      return 0.0;
    }
    return (curr - prev) / nmp;
  }
}
