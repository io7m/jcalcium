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

package com.io7m.jcalcium.tests.format.json.jackson;

import com.io7m.jcalcium.core.definitions.CaDefinitionSkeletonType;
import com.io7m.jcalcium.format.json.jackson.CaJSONFormatProvider;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseErrorType;
import javaslang.collection.List;
import javaslang.control.Validation;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public final class CaJSONFormatProviderTest
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaJSONFormatProviderTest.class);
  }

  @Test
  public void testEmpty()
  {
    final CaDefinitionParserType p = new CaJSONFormatProvider().create();
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("empty.caj"), uri("empty.caj"));

    dump(r);
    Assert.assertTrue(r.isValid());
    Assert.assertEquals("empty", r.get().name().value());
  }

  @Test
  public void testNoVersion()
  {
    final CaDefinitionParserType p = new CaJSONFormatProvider().create();
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("no_version.caj"), uri("no_version.caj"));

    dump(r);
    Assert.assertFalse(r.isValid());
  }

  @Test
  public void testBadVersion()
  {
    final CaDefinitionParserType p = new CaJSONFormatProvider().create();
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("bad_version.caj"), uri("bad_version.caj"));

    dump(r);
    Assert.assertFalse(r.isValid());
  }

  private static void dump(
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r)
  {
    if (r.isValid()) {
      LOG.debug("valid: {}", r.get());
    } else {
      r.getError().forEach(e -> LOG.error("invalid: {}", e));
    }
  }

  private static URI uri(final String s)
  {
    try {
      return CaJSONFormatProviderTest.class.getResource(s).toURI();
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static InputStream resource(final String s)
  {
    return CaJSONFormatProviderTest.class.getResourceAsStream(s);
  }
}
