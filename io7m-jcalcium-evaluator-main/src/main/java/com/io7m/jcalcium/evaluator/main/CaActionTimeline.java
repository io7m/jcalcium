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

package com.io7m.jcalcium.evaluator.main;

import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.evaluator.api.CaActionKeyframeCurrent;
import com.io7m.jnull.NullCheck;
import com.io7m.junsigned.core.UnsignedDouble;
import it.unimi.dsi.fastutil.ints.IntSortedSet;

/**
 * A simple timeline.
 */

public final class CaActionTimeline
{
  private final IntSortedSet keyframes;
  private final int last_frame;
  private final CaActionKeyframeCurrent keyframe_default;

  /**
   * Construct a timeline.
   *
   * @param in_keyframes  The set of available keyframes
   * @param in_last_frame The total length of the timeline
   */

  public CaActionTimeline(
    final IntSortedSet in_keyframes,
    final int in_last_frame)
  {
    this.keyframes =
      NullCheck.notNull(in_keyframes, "Keyframes");

    Preconditions.checkPrecondition(
      !in_keyframes.isEmpty(),
      "Must provide at least one keyframe");

    Preconditions.checkPreconditionI(
      in_last_frame,
      in_last_frame >= 0,
      i -> "Last frame must be positive");

    {
      final int f = in_keyframes.firstInt();
      this.keyframe_default = CaActionKeyframeCurrent.of(f, f, (double) f);
    }

    this.last_frame = in_last_frame;
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

  private static int keyframeIndexPrevious(
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

  /**
   * Calculate keyframe values.
   *
   * @param frame_start   The global frame that this timeline is assumed to have
   *                      started
   * @param frame_current The current global frame
   * @param time_scale    The current time scale
   *
   * @return Calculated keyframe values
   */

  public CaActionKeyframeCurrent keyframeCurrent(
    final long frame_start,
    final long frame_current,
    final double time_scale)
  {
    if (this.keyframes.size() == 1) {
      return this.keyframe_default;
    }

    final double frame =
      calculateFrame(frame_start, frame_current, time_scale, this.last_frame);
    final int iframe = (int) Math.floor(frame);

    final int key_frame_prev =
      keyframeIndexPrevious(this.keyframes, iframe);
    final int key_frame_next =
      keyframeIndexNext(this.keyframes, iframe);

    return CaActionKeyframeCurrent.of(key_frame_prev, key_frame_next, frame);
  }
}
