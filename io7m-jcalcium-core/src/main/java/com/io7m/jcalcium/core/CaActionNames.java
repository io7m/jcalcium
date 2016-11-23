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
import com.io7m.junreachable.UnreachableCodeException;

import java.util.regex.Pattern;

/**
 * Validity checks for action names.
 */

public final class CaActionNames
{
  /**
   * The pattern that defines a valid action name.
   */

  public static final Pattern PATTERN;

  private static final String PATTERN_TEXT;

  static {
    PATTERN_TEXT = "[\\p{IsAlphabetic}\\p{IsDigit}_\\-\\.:]+";
    PATTERN = NullCheck.notNull(
      Pattern.compile(PATTERN_TEXT, Pattern.UNICODE_CHARACTER_CLASS));
  }

  /**
   * @param text The text
   *
   * @return {@code true} iff the given text is a valid action name
   */

  public static boolean isValid(
    final CharSequence text)
  {
    return PATTERN.matcher(text).matches();
  }

  private CaActionNames()
  {
    throw new UnreachableCodeException();
  }
}
