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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.format.json.jackson.v1.CaV1JSONParser;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Functions shared by parser implementations.
 *
 * <p>Note: This is not part of the public API</p>
 */

public final class CaJSON
{
  private CaJSON()
  {
    throw new UnreachableCodeException();
  }

  /**
   * @return An object mapper initialized with various modules and features
   */

  public static ObjectMapper createMapper()
  {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(createModule());
    mapper.registerModule(new Jdk8Module());
    mapper.enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES);
    mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    return mapper;
  }

  /**
   * @return A initialized with various deserializers
   */

  public static SimpleModule createModule()
  {
    final SimpleModule module = new SimpleModule();
    module.addDeserializer(
      PVectorI3D.class, new CaPVectorI3DDeserializer());
    module.addDeserializer(
      VectorI3D.class, new CaVectorI3DDeserializer());
    module.addDeserializer(
      QuaternionI4D.class, new CaQuaternionI4DDeserializer());
    module.addDeserializer(
      CaCurveInterpolation.class,
      new CaV1JSONParser.CaCurveInterpolationDeserializer());
    module.addDeserializer(
      CaCurveEasing.class, new CaV1JSONParser.CaCurveEasingDeserializer());
    return module;
  }
}
