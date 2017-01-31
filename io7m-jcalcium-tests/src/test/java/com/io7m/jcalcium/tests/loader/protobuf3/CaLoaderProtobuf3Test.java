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

package com.io7m.jcalcium.tests.loader.protobuf3;

import com.io7m.jcalcium.compiler.api.CaCompilerType;
import com.io7m.jcalcium.compiler.main.CaCompiler;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.format.json.jackson.CaJSONFormatProvider;
import com.io7m.jcalcium.format.protobuf3.CaProtobuf3FormatProvider;
import com.io7m.jcalcium.loader.api.CaLoaderException;
import com.io7m.jcalcium.loader.api.CaLoaderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerType;
import com.io7m.jcalcium.tests.loader.api.CaLoaderContract;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Random;

public final class CaLoaderProtobuf3Test extends CaLoaderContract
{
  private static final Logger LOG;
  private static final CaProtobuf3FormatProvider PROVIDER =
    new CaProtobuf3FormatProvider();

  static {
    LOG = LoggerFactory.getLogger(CaLoaderProtobuf3Test.class);
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }

  @Override
  protected CaLoaderType loader()
  {
    return PROVIDER.loaderCreate();
  }

  @Override
  protected CaCompilerType compiler()
  {
    return CaCompiler.create();
  }

  @Override
  protected CaCompiledSerializerType serializer()
  {
    return PROVIDER.serializerCreate(CaFormatVersion.of(1, 0));
  }

  /**
   * Fuzz the loader with damaged data.
   */

  @Test
  public void testCompileDamagedLaterHalf()
    throws Exception
  {
    final CaDefinitionParserType pj = new CaJSONFormatProvider().parserCreate();
    final CaCompiledSerializerType cs = this.serializer();
    final CaCompilerType cc = this.compiler();
    final CaLoaderType cl = this.loader();

    final InputStream res =
      this.resource("/com/io7m/jcalcium/tests/format/json/jackson/all-1.0.csj");
    final URI uri =
      this.uri("/com/io7m/jcalcium/tests/format/json/jackson/all-1.0.csj");

    final CaDefinitionSkeleton skel_d =
      pj.parseSkeletonFromStream(res, uri).get();
    final CaSkeleton skel_c =
      cc.compile(skel_d).get();

    final int max_count = 100;
    int fail_count = 0;

    final Random rand = new Random();
    for (int index = 0; index < max_count; ++index) {
      final ByteArrayOutputStream bao = new ByteArrayOutputStream(4096);
      cs.serializeCompiledSkeletonToStream(skel_c, bao);
      final byte[] data = bao.toByteArray();

      for (int b = data.length / 2; b < data.length; ++b) {
        if (rand.nextDouble() < 0.35) {
          data[b] = (byte) rand.nextInt();
        }
      }

      try {
        final ByteArrayInputStream bai = new ByteArrayInputStream(data);
        cl.loadCompiledSkeletonFromStream(bai, uri);
      } catch (final CaLoaderException e) {
        this.log().error("damage: ", e);
        ++fail_count;
      }
    }

    this.log().debug(
      "expected count: {}, received count: {}",
      Integer.valueOf(max_count),
      Integer.valueOf(fail_count));
    Assert.assertEquals((long) max_count, (long) fail_count);
  }
}
