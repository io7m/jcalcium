/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
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

package com.io7m.jcalcium.core.compiled;

import com.io7m.jcalcium.core.CaImmutableStyleType;
import org.immutables.value.Value;

import java.util.Objects;

import static com.io7m.jcalcium.core.compiled.CaSkeletonHashes.HASH_PATTERN;

/**
 * The type of skeleton hashes.
 */

@Value.Immutable
@CaImmutableStyleType
public interface CaSkeletonHashType
{
  /**
   * @return The name of the hash algorithm
   */

  @Value.Parameter
  String algorithm();

  /**
   * @return The hash value as an ASCII hexadecimal string
   */

  @Value.Parameter
  String value();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    if (!Objects.equals(this.algorithm(), "SHA2-256")) {
      throw new IllegalArgumentException("Algorithm must be SHA2-256");
    }

    if (!HASH_PATTERN.matcher(this.value()).matches()) {
      throw new IllegalArgumentException(
        "Hash value must match the pattern: " + HASH_PATTERN.pattern());
    }
  }
}
