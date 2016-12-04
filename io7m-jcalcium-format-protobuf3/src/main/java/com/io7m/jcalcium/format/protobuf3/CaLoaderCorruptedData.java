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

package com.io7m.jcalcium.format.protobuf3;

import com.io7m.jcalcium.loader.api.CaLoaderException;

import java.net.URI;

/**
 * The data was corrupted in some manner.
 */

public final class CaLoaderCorruptedData extends CaLoaderException
{
  /**
   * Construct an exception.
   *
   * @param uri     The URI
   * @param message The error message
   */

  public CaLoaderCorruptedData(
    final URI uri,
    final String message)
  {
    super(uri, message);
  }

  /**
   * Construct an exception.
   *
   * @param uri     The URI
   * @param cause   The cause
   * @param message The error message
   */

  public CaLoaderCorruptedData(
    final URI uri,
    final Throwable cause,
    final String message)
  {
    super(uri, message, cause);
  }
}
