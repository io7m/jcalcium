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

package com.io7m.jcalcium.core.definitions.actions;

import com.io7m.jcalcium.core.CaActionNameType;
import com.io7m.jfunctional.PartialBiFunctionType;

/**
 * A definition of an action.
 */

public interface CaDefinitionActionType
{
  /**
   * Match on an action definition.
   *
   * @param context        A contextual value
   * @param on_curves Evaluated for curve actions
   * @param <A>            The type of contextual values
   * @param <B>            The type of returned values
   * @param <E>            The type of raised exceptions
   *
   * @return The value returned by the evaluated function
   *
   * @throws E If any of the given functions raise {@code E}
   */

  <A, B, E extends Exception>
  B matchAction(
    A context,
    PartialBiFunctionType<A, CaDefinitionActionCurvesType, B, E> on_curves)
    throws E;

  /**
   * @return The name of the action
   */

  CaActionNameType name();

  /**
   * @return The number of frames per second the action uses
   */

  int framesPerSecond();
}
