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

package com.io7m.jcalcium.core;

import com.io7m.jnull.NullCheck;

/**
 * Curve easing specifications.
 */

public enum CaCurveEasing
{
  /**
   * Ease the given curve in.
   */

  CURVE_EASING_IN("in"),

  /**
   * Ease the given curve out.
   */

  CURVE_EASING_OUT("out"),

  /**
   * Ease the given curve in and out.
   */

  CURVE_EASING_IN_OUT("in-out");

  private final String name;

  CaCurveEasing(
    final String in_name)
  {
    this.name = NullCheck.notNull(in_name, "Name");
  }

  /**
   * @param name The name of an easing value
   *
   * @return The easing value with the given name
   */

  public static CaCurveEasing of(
    final String name)
  {
    switch (NullCheck.notNull(name, "Name")) {
      case "in":
        return CURVE_EASING_IN;
      case "out":
        return CURVE_EASING_OUT;
      case "in-out":
        return CURVE_EASING_IN_OUT;
      default: {
        throw new IllegalArgumentException(
          "Unrecognized curve easing: " + name);
      }
    }
  }
}
