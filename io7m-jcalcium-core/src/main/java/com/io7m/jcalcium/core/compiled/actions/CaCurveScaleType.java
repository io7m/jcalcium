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

package com.io7m.jcalcium.core.compiled.actions;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.CaImmutableStyleType;
import com.io7m.jfunctional.PartialBiFunctionType;
import com.io7m.jnull.NullCheck;
import javaslang.collection.SortedMap;
import org.immutables.javaslang.encodings.JavaslangEncodingEnabled;
import org.immutables.value.Value;

/**
 * A curve that affects the scale of a joint.
 */

@CaImmutableStyleType
@JavaslangEncodingEnabled
@Value.Immutable
public interface CaCurveScaleType extends CaCurveType
{
  @Override
  default <A, B, E extends Exception> B matchCurve(
    final A context,
    final PartialBiFunctionType<A, CaCurveTranslationType, B, E> on_translation,
    final PartialBiFunctionType<A, CaCurveOrientationType, B, E> on_orientation,
    final PartialBiFunctionType<A, CaCurveScaleType, B, E> on_scale)
    throws E
  {
    return NullCheck.notNull(on_scale, "on_scale").call(context, this);
  }

  /**
   * @return The list of keyframes for the curve
   */

  SortedMap<Integer, CaCurveKeyframeScale> keyframes();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    this.keyframes().forEach(
      (index, keyframe) ->
        Preconditions.checkPreconditionI(
          index.intValue(),
          index.intValue() == keyframe.index(),
          i -> "Scale keyframe index must be " + keyframe.index()));
  }
}
