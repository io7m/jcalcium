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

package com.io7m.jcalcium.format.json.jackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.io7m.jtensors.parameterized.PVectorI3D;

import java.io.IOException;

/**
 * A Jackson deserializer for {@code PVectorI3D} values.
 */

public final class CaPVectorI3DDeserializer
  extends StdDeserializer<PVectorI3D<?>>
{
  /**
   * Construct a deserializer.
   */

  public CaPVectorI3DDeserializer()
  {
    super(PVectorI3D.class);
  }

  @Override
  public PVectorI3D<?> deserialize(
    final JsonParser p,
    final DeserializationContext ctxt)
    throws IOException, JsonProcessingException
  {
    final TreeNode n = p.getCodec().readTree(p);
    if (n instanceof ArrayNode) {
      final ArrayNode a = (ArrayNode) n;

      boolean ok = a.size() == 3;
      ok &= a.get(0) instanceof DoubleNode;
      ok &= a.get(1) instanceof DoubleNode;
      ok &= a.get(2) instanceof DoubleNode;

      if (ok) {
        return new PVectorI3D(
          a.get(0).doubleValue(),
          a.get(1).doubleValue(),
          a.get(2).doubleValue());
      }
    }

    throw new JsonParseException(p, "Expected an array of three real values");
  }
}
