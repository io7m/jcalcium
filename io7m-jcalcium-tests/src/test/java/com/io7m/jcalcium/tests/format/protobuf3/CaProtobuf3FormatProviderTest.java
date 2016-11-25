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

package com.io7m.jcalcium.tests.format.protobuf3;

import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.format.json.jackson.CaJSONFormatProvider;
import com.io7m.jcalcium.format.protobuf3.CaProtobuf3FormatProvider;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jcalcium.serializer.api.CaDefinitionSerializerType;
import javaslang.collection.List;
import javaslang.control.Validation;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public final class CaProtobuf3FormatProviderTest
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaProtobuf3FormatProviderTest.class);
  }

  @Test
  public void testRoundTripAllv1_0()
    throws IOException
  {
    final CaDefinitionParserType pj =
      new CaJSONFormatProvider().parserCreate();
    final CaDefinitionSerializerType sj =
      new CaJSONFormatProvider().serializerCreate(CaFormatVersion.of(1, 0));

    final CaDefinitionParserType pp =
      new CaProtobuf3FormatProvider().parserCreate();
    final CaDefinitionSerializerType sp =
      new CaProtobuf3FormatProvider().serializerCreate(CaFormatVersion.of(
        1,
        0));

    final Validation<List<CaParseError>, CaDefinitionSkeleton> rj =
      pj.parseSkeletonFromStream(resource("all-1.0.caj"), uri("all-1.0.caj"));
    dump(rj);
    Assert.assertTrue(rj.isValid());

    final ByteArrayOutputStream bao = new ByteArrayOutputStream();
    sp.serializeSkeletonToStream(rj.get(), bao);

    final Validation<List<CaParseError>, CaDefinitionSkeleton> rp =
      pp.parseSkeletonFromStream(
        new ByteArrayInputStream(bao.toByteArray()),
        uri("all-1.0.caj"));

    dump(rp);
    Assert.assertTrue(rp.isValid());
    Assert.assertEquals(rj.get(), rp.get());
  }

  private static void dump(
    final Validation<List<CaParseError>, CaDefinitionSkeleton> r)
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
      return CaProtobuf3FormatProviderTest.class.getResource(s).toURI();
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static InputStream resource(final String s)
  {
    return CaProtobuf3FormatProviderTest.class.getResourceAsStream(s);
  }
}
