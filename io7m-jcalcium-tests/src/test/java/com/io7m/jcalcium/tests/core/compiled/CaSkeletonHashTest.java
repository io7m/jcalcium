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

package com.io7m.jcalcium.tests.core.compiled;

import com.io7m.jcalcium.core.compiled.CaSkeletonHash;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class CaSkeletonHashTest
{
  @Rule public ExpectedException expected = ExpectedException.none();

  @Test public void testBadAlgorithm()
  {
    this.expected.expect(IllegalArgumentException.class);
    this.expected.expectMessage(
      StringContains.containsString("Algorithm must be SHA2-256"));
    CaSkeletonHash.of("wrong", "3cb4e2c9e926cce1aa345f1e1724db8683a2aa1056b236ecd7c3aba318a9416d");
  }

  @Test public void testBadHash()
  {
    this.expected.expect(IllegalArgumentException.class);
    this.expected.expectMessage(
      StringContains.containsString("Hash value must match the pattern"));
    CaSkeletonHash.of("SHA2-256", "x");
  }
}
