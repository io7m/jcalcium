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

package com.io7m.jcalcium.loader.api;

import com.io7m.jnull.NullCheck;

import java.net.URI;

/**
 * The type of exceptions raised by loaders.
 */

public abstract class CaLoaderException extends Exception
{
  private final URI uri;

  /**
   * Construct an exception.
   *
   * @param in_uri     The source URI
   * @param in_message The message
   */

  public CaLoaderException(
    final URI in_uri,
    final String in_message)
  {
    super(in_message);
    this.uri = NullCheck.notNull(in_uri, "URI");
  }

  /**
   * Construct an exception.
   *
   * @param in_uri     The source URI
   * @param in_message The message
   * @param in_cause   The cause
   */

  public CaLoaderException(
    final URI in_uri,
    final String in_message,
    final Throwable in_cause)
  {
    super(in_message, in_cause);
    this.uri = NullCheck.notNull(in_uri, "URI");
  }

  /**
   * Construct an exception.
   *
   * @param in_uri   The source URI
   * @param in_cause The cause
   */

  public CaLoaderException(
    final URI in_uri,
    final Throwable in_cause)
  {
    super(in_cause);
    this.uri = NullCheck.notNull(in_uri, "URI");
  }

  /**
   * @return The URI of the source
   */

  public final URI uri()
  {
    return this.uri;
  }
}
