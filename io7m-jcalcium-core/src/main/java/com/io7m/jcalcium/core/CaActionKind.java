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
 * The kind of supported actions.
 */

public enum CaActionKind
{
  /**
   * An action constructed from curves that drive components of bones.
   */

  ACTION_CURVES("curves");

  private final String name;

  CaActionKind(
    final String in_name)
  {
    this.name = NullCheck.notNull(in_name, "Name");
  }

  /**
   * @param name A kind name
   *
   * @return The kind for the given name
   */

  public static CaActionKind of(
    final String name)
  {
    switch (NullCheck.notNull(name, "Name")) {
      case "curves":
        return ACTION_CURVES;
      default: {
        throw new IllegalArgumentException(
          "Unrecognized action kind: " + name);
      }
    }
  }
}
