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

import com.io7m.jcalcium.core.ImmutableStyleType;
import com.io7m.jfunctional.PartialBiFunctionType;
import com.io7m.jnull.NullCheck;
import javaslang.collection.List;
import javaslang.collection.SortedMap;
import org.immutables.value.Value;

/**
 * A curve that affects the translation of a bone.
 */

@ImmutableStyleType
@Value.Immutable
public interface CaCurveTranslationType extends CaCurveType
{
  @Override
  default <A, B, E extends Exception> B matchCurve(
    final A context,
    final PartialBiFunctionType<A, CaCurveTranslationType, B, E> on_translation,
    final PartialBiFunctionType<A, CaCurveOrientationType, B, E> on_orientation,
    final PartialBiFunctionType<A, CaCurveScaleType, B, E> on_scale)
    throws E
  {
    return NullCheck.notNull(on_translation, "on_translation")
      .call(context, this);
  }

  /**
   * @return The list of keyframes for the curve
   */

  SortedMap<Integer, CaCurveKeyframeTranslationType> keyframes();

}
