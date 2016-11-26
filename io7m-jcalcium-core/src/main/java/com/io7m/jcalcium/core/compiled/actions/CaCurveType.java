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

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jfunctional.PartialBiFunctionType;

/**
 * A curve definition.
 */

public interface CaCurveType
{
  /**
   * Match on a curve definition.
   *
   * @param context        A contextual value
   * @param on_translation Evaluated for translation curves
   * @param on_orientation Evaluated for orientation curves
   * @param on_scale       Evaluated for scale curves
   * @param <A>            The type of contextual values
   * @param <B>            The type of returned values
   * @param <E>            The type of raised exceptions
   *
   * @return The value returned by the evaluated function
   *
   * @throws E If any of the given functions raise {@code E}
   */

  <A, B, E extends Exception>
  B matchCurve(
    A context,
    PartialBiFunctionType<A, CaCurveTranslationType, B, E> on_translation,
    PartialBiFunctionType<A, CaCurveOrientationType, B, E> on_orientation,
    PartialBiFunctionType<A, CaCurveScaleType, B, E> on_scale)
    throws E;

  /**
   * @return The action to which this curve belongs
   */

  CaActionName action();

  /**
   * @return The name of the bone to which the curve refers
   */

  CaBoneName bone();
}