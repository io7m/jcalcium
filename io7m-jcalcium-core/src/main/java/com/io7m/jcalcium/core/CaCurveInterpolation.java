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
 * The interpolation type for a curve.
 */

public enum CaCurveInterpolation
{
  /**
   * Constant interpolation
   */

  CURVE_INTERPOLATION_CONSTANT("constant"),

  /**
   * Linear interpolation
   */

  CURVE_INTERPOLATION_LINEAR("linear"),

  /**
   * Quadratic interpolation
   */

  CURVE_INTERPOLATION_QUADRATIC("quadratic"),

  /**
   * Exponential interpolation
   */

  CURVE_INTERPOLATION_EXPONENTIAL("exponential");

  private final String name;

  CaCurveInterpolation(
    final String in_name)
  {
    this.name = NullCheck.notNull(in_name, "Name");
  }

  /**
   * @param name The name of an interpolation type
   *
   * @return An interpolation type based on the given name
   */

  public static CaCurveInterpolation of(
    final String name)
  {
    switch (NullCheck.notNull(name, "Name")) {
      case "linear":
        return CURVE_INTERPOLATION_LINEAR;
      case "exponential":
        return CURVE_INTERPOLATION_EXPONENTIAL;
      case "constant":
        return CURVE_INTERPOLATION_CONSTANT;
      case "quadratic":
        return CURVE_INTERPOLATION_QUADRATIC;
    }

    throw new IllegalArgumentException(
      "Unrecognized curve interpolation: " + name);
  }

  /**
   * @return The interpolation name
   */

  public String getName()
  {
    return this.name;
  }
}
