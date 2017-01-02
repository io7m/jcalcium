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

package com.io7m.jcalcium.core.definitions;

import com.io7m.jcalcium.core.CaImmutableStyleType;
import org.immutables.value.Value;

/**
 * A description of a definition format.
 */

@Value.Immutable
@CaImmutableStyleType
public interface CaFormatDescriptionType
{
  /**
   * @return A human-readable description of the format
   */

  String description();

  /**
   * @return The filename suffix used for the format
   */

  String suffix();

  /**
   * @return The mime type for the format
   */

  String mimeType();

  /**
   * @return The unique name for the format
   */

  String name();
}
