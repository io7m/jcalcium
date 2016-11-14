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
 * The target of a curve.
 */

public enum CaCurveTarget
{
  /**
   * The curve will affect the translation of a bone.
   */

  CURVE_TARGET_TRANSLATION("translation"),

  /**
   * The curve will affect the orientation of a bone.
   */

  CURVE_TARGET_ORIENTATION("orientation"),

  /**
   * The curve will affect the scale of a bone.
   */

  CURVE_TARGET_SCALE("scale");

  private final String name;

  CaCurveTarget(
    final String in_name)
  {
    this.name = NullCheck.notNull(in_name, "Name");
  }

  /**
   * @param name The name of a curve target
   *
   * @return A curve target based on the given name
   */

  public static CaCurveTarget of(
    final String name)
  {
    switch (NullCheck.notNull(name, "Name")) {
      case "translation":
        return CURVE_TARGET_TRANSLATION;
      case "orientation":
        return CURVE_TARGET_ORIENTATION;
      case "scale":
        return CURVE_TARGET_SCALE;
    }

    throw new IllegalArgumentException("Unrecognized curve kind: " + name);
  }
}
