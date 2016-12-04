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

package com.io7m.jcalcium.tests.loader.api;

import com.io7m.jcalcium.compiler.api.CaCompileError;
import com.io7m.jcalcium.compiler.api.CaCompilerType;
import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.compiled.CaBone;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.format.json.jackson.CaJSONFormatProvider;
import com.io7m.jcalcium.loader.api.CaLoaderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerType;
import com.io7m.jcalcium.tests.format.json.jackson.v1.CaV1JSONParserTest;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import javaslang.collection.List;
import javaslang.collection.SortedMap;
import javaslang.control.Validation;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;

import static javaslang.control.Validation.valid;

public abstract class CaLoaderContract
{
  protected abstract Logger log();

  protected abstract CaLoaderType loader();

  protected abstract CaCompilerType compiler();

  protected abstract CaCompiledSerializerType serializer();

  @Test
  public final void testCompileLoadEqual()
    throws Exception
  {
    final CaDefinitionParserType pj = new CaJSONFormatProvider().parserCreate();
    final CaCompiledSerializerType cs = this.serializer();
    final CaCompilerType cc = this.compiler();
    final CaLoaderType cl = this.loader();

    final InputStream res = this.resource(
      "/com/io7m/jcalcium/tests/format/json/jackson/all-1.0.csj");
    final URI uri =
      this.uri("/com/io7m/jcalcium/tests/format/json/jackson/all-1.0.csj");

    this.log().debug("parsing skeleton");

    final Instant parse_time_then = Instant.now();
    final Validation<List<CaParseError>, CaDefinitionSkeleton> rj =
      pj.parseSkeletonFromStream(res, uri);
    final Instant parse_time_now = Instant.now();

    this.log().debug(
      "parsed in {}ms",
      Long.valueOf(Duration.between(
        parse_time_then,
        parse_time_now).toMillis()));

    this.dumpParse(rj);
    Assert.assertTrue(rj.isValid());

    this.log().debug("compiling skeleton");

    final Instant compile_time_then = Instant.now();
    final Validation<List<CaCompileError>, CaSkeleton> cr = cc.compile(rj.get());
    final Instant compile_time_now = Instant.now();

    this.log().debug(
      "compiled in {}ms",
      Long.valueOf(Duration.between(
        compile_time_then,
        compile_time_now).toMillis()));

    this.dumpCompile(cr);
    Assert.assertTrue(cr.isValid());

    this.log().debug("serializing skeleton");

    final ByteArrayOutputStream bao = new ByteArrayOutputStream(4096);
    final CaSkeleton sk_c = cr.get();

    final Instant serial_time_then = Instant.now();
    cs.serializeCompiledSkeletonToStream(sk_c, bao);
    final Instant serial_time_now = Instant.now();

    this.log().debug(
      "serialized in {}ms",
      Long.valueOf(Duration.between(
        serial_time_then,
        serial_time_now).toMillis()));

    this.log().debug("loading skeleton");

    final ByteArrayInputStream bai = new ByteArrayInputStream(bao.toByteArray());

    final Instant load_time_then = Instant.now();
    final CaSkeleton sk_l = cl.loadCompiledSkeletonFromStream(bai, uri);
    final Instant load_time_now = Instant.now();

    this.log().debug(
      "loaded in {}ms",
      Long.valueOf(Duration.between(load_time_then, load_time_now).toMillis()));

    this.dumpCompile(valid(sk_l));

    Assert.assertEquals(sk_c.name(), sk_l.name());

    {
      final SortedMap<CaActionName, CaActionType> c_actions = sk_c.actionsByName();
      final SortedMap<CaActionName, CaActionType> l_actions = sk_l.actionsByName();
      Assert.assertEquals(c_actions, l_actions);
    }

    {
      final SortedMap<CaBoneName, JOTreeNodeReadableType<CaBone>> l_bones =
        sk_l.bonesByName();
      final SortedMap<CaBoneName, JOTreeNodeReadableType<CaBone>> c_bones =
        sk_c.bonesByName();
      Assert.assertEquals((long) c_bones.size(), (long) l_bones.size());

      for (final CaBoneName id : c_bones.keySet()) {
        final JOTreeNodeReadableType<CaBone> c_node = c_bones.get(id).get();
        final JOTreeNodeReadableType<CaBone> l_node = l_bones.get(id).get();
        Assert.assertEquals(c_node.value(), l_node.value());

        if (c_node.parentReadable().isPresent()) {
          final CaBone c_parent = c_node.parentReadable().get().value();
          final CaBone l_parent = l_node.parentReadable().get().value();
          Assert.assertEquals(c_parent, l_parent);
        }

        Assert.assertEquals(
          (long) c_node.childrenReadable().size(),
          (long) l_node.childrenReadable().size());
      }
    }

    {
      final SortedMap<Integer, JOTreeNodeReadableType<CaBone>> l_bones =
        sk_l.bonesByID();
      final SortedMap<Integer, JOTreeNodeReadableType<CaBone>> c_bones =
        sk_c.bonesByID();
      Assert.assertEquals((long) c_bones.size(), (long) l_bones.size());

      for (final Integer id : c_bones.keySet()) {
        final JOTreeNodeReadableType<CaBone> c_node = c_bones.get(id).get();
        final JOTreeNodeReadableType<CaBone> l_node = l_bones.get(id).get();
        Assert.assertEquals(c_node.value(), l_node.value());

        if (c_node.parentReadable().isPresent()) {
          final CaBone c_parent = c_node.parentReadable().get().value();
          final CaBone l_parent = l_node.parentReadable().get().value();
          Assert.assertEquals(c_parent, l_parent);
        }

        Assert.assertEquals(
          (long) c_node.childrenReadable().size(),
          (long) l_node.childrenReadable().size());
      }
    }
  }

  protected final void dumpParse(
    final Validation<List<CaParseError>, CaDefinitionSkeleton> r)
  {
    if (r.isValid()) {
      this.log().debug("valid: {}", r.get());
    } else {
      r.getError().forEach(e -> this.log().error("invalid: {}", e));
    }
  }

  protected final void dumpCompile(
    final Validation<List<CaCompileError>, CaSkeleton> r)
  {
    if (r.isValid()) {
      this.log().debug("valid: {}", r.get());
    } else {
      r.getError().forEach(e -> this.log().error("invalid: {}", e));
    }
  }

  protected final URI uri(
    final String s)
  {
    try {
      return CaV1JSONParserTest.class.getResource(s).toURI();
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected final InputStream resource(
    final String s)
  {
    return CaV1JSONParserTest.class.getResourceAsStream(s);
  }
}
