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

package com.io7m.jcalcium.tests.parser.json.jackson.v1;

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.definitions.CaDefinitionBoneType;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeletonType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionCurvesType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeOrientationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeScaleType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeTranslationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveOrientationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveScaleType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveTranslationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveType;
import com.io7m.jcalcium.parser.json.jackson.CaJSON;
import com.io7m.jcalcium.parser.json.jackson.v1.CaV1JSONParser;
import com.io7m.jcalcium.parser.api.CaParseErrorType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import javaslang.collection.List;
import javaslang.control.Validation;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public final class CaV1JSONParserTest
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaV1JSONParserTest.class);
  }

  @Test
  public void testEmpty()
  {
    final CaV1JSONParser p = new CaV1JSONParser(CaJSON.createMapper());
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("empty.caj"), uri("empty.caj"));

    dump(r);
    Assert.assertTrue(r.isValid());
    Assert.assertEquals("empty", r.get().name().value());
  }

  @Test
  public void testBone0()
  {
    final CaV1JSONParser p = new CaV1JSONParser(CaJSON.createMapper());
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("bone0.caj"), uri("bone0.caj"));

    dump(r);
    Assert.assertTrue(r.isValid());
    final CaDefinitionSkeletonType s = r.get();
    Assert.assertEquals("bone0", s.name().value());
    Assert.assertEquals(1L, (long) s.bones().size());

    final CaDefinitionBoneType b = s.bones().get(CaBoneName.of("bone.000")).get();
    Assert.assertEquals("bone.000", b.name().value());

    Assert.assertEquals(0.0, b.translation().getXD(), 0.0);
    Assert.assertEquals(1.0, b.translation().getYD(), 0.0);
    Assert.assertEquals(2.0, b.translation().getZD(), 0.0);

    Assert.assertEquals(3.0, b.scale().getXD(), 0.0);
    Assert.assertEquals(4.0, b.scale().getYD(), 0.0);
    Assert.assertEquals(5.0, b.scale().getZD(), 0.0);

    Assert.assertEquals(6.0, b.orientation().getXD(), 0.0);
    Assert.assertEquals(7.0, b.orientation().getYD(), 0.0);
    Assert.assertEquals(8.0, b.orientation().getZD(), 0.0);
    Assert.assertEquals(9.0, b.orientation().getWD(), 0.0);
  }

  @Test
  public void testBone1()
  {
    final CaV1JSONParser p = new CaV1JSONParser(CaJSON.createMapper());
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("bone1.caj"), uri("bone1.caj"));

    dump(r);
    Assert.assertTrue(r.isValid());
    final CaDefinitionSkeletonType s = r.get();
    Assert.assertEquals("bone1", s.name().value());
    Assert.assertEquals(1L, (long) s.bones().size());

    final CaDefinitionBoneType b = s.bones().get(CaBoneName.of("bone.000")).get();
    Assert.assertEquals("bone.000", b.name().value());
    Assert.assertEquals("bone.001", b.parent().get().value());

    Assert.assertEquals(0.0, b.translation().getXD(), 0.0);
    Assert.assertEquals(1.0, b.translation().getYD(), 0.0);
    Assert.assertEquals(2.0, b.translation().getZD(), 0.0);

    Assert.assertEquals(3.0, b.scale().getXD(), 0.0);
    Assert.assertEquals(4.0, b.scale().getYD(), 0.0);
    Assert.assertEquals(5.0, b.scale().getZD(), 0.0);

    Assert.assertEquals(6.0, b.orientation().getXD(), 0.0);
    Assert.assertEquals(7.0, b.orientation().getYD(), 0.0);
    Assert.assertEquals(8.0, b.orientation().getZD(), 0.0);
    Assert.assertEquals(9.0, b.orientation().getWD(), 0.0);
  }

  @Test
  public void testAction0()
  {
    final CaV1JSONParser p = new CaV1JSONParser(CaJSON.createMapper());
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("action0.caj"), uri("action0.caj"));

    dump(r);
    Assert.assertTrue(r.isValid());
    final CaDefinitionSkeletonType s = r.get();
    Assert.assertEquals(0L, (long) s.actions().size());
  }

  @Test
  public void testAction1()
  {
    final CaV1JSONParser p = new CaV1JSONParser(CaJSON.createMapper());
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("action1.caj"), uri("action1.caj"));

    dump(r);
    Assert.assertTrue(r.isValid());
    final CaDefinitionSkeletonType s = r.get();
    Assert.assertEquals(1L, (long) s.actions().size());

    final CaDefinitionActionCurvesType act = (CaDefinitionActionCurvesType)
      s.actions().get(CaActionName.of("action0")).get();
    Assert.assertEquals("action0", act.name().value());
    Assert.assertEquals(60L, (long) act.framesPerSecond());
    Assert.assertEquals(1L, (long) act.curves().size());

    final List<CaDefinitionCurveType> cs =
      act.curves().get(CaBoneName.of("bone.000")).get();
    Assert.assertEquals(1L, (long) cs.size());

    final CaDefinitionCurveTranslationType c =
      (CaDefinitionCurveTranslationType) cs.get(0);
    Assert.assertEquals(1L, (long) c.keyframes().size());

    final CaDefinitionCurveKeyframeTranslationType kf = c.keyframes().get(0);
    Assert.assertEquals(new PVectorI3D<>(0.0, 1.0, 2.0), kf.translation());
    Assert.assertEquals(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR, kf.interpolation());
    Assert.assertEquals(CaCurveEasing.CURVE_EASING_IN_OUT, kf.easing());
  }

  @Test
  public void testAction2()
  {
    final CaV1JSONParser p = new CaV1JSONParser(CaJSON.createMapper());
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("action2.caj"), uri("action2.caj"));

    dump(r);
    Assert.assertTrue(r.isValid());
    final CaDefinitionSkeletonType s = r.get();
    Assert.assertEquals(1L, (long) s.actions().size());

    final CaDefinitionActionCurvesType act = (CaDefinitionActionCurvesType)
      s.actions().get(CaActionName.of("action0")).get();
    Assert.assertEquals("action0", act.name().value());
    Assert.assertEquals(60L, (long) act.framesPerSecond());
    Assert.assertEquals(1L, (long) act.curves().size());

    final List<CaDefinitionCurveType> cs =
      act.curves().get(CaBoneName.of("bone.000")).get();
    Assert.assertEquals(1L, (long) cs.size());

    final CaDefinitionCurveScaleType c =
      (CaDefinitionCurveScaleType) cs.get(0);
    Assert.assertEquals(1L, (long) c.keyframes().size());

    final CaDefinitionCurveKeyframeScaleType kf = c.keyframes().get(0);
    Assert.assertEquals(new VectorI3D(0.0, 1.0, 2.0), kf.scale());
    Assert.assertEquals(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR, kf.interpolation());
    Assert.assertEquals(CaCurveEasing.CURVE_EASING_IN_OUT, kf.easing());
  }

  @Test
  public void testAction3()
  {
    final CaV1JSONParser p = new CaV1JSONParser(CaJSON.createMapper());
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("action3.caj"), uri("action3.caj"));

    dump(r);
    Assert.assertTrue(r.isValid());
    final CaDefinitionSkeletonType s = r.get();
    Assert.assertEquals(1L, (long) s.actions().size());

    final CaDefinitionActionCurvesType act = (CaDefinitionActionCurvesType)
      s.actions().get(CaActionName.of("action0")).get();
    Assert.assertEquals("action0", act.name().value());
    Assert.assertEquals(60L, (long) act.framesPerSecond());
    Assert.assertEquals(1L, (long) act.curves().size());

    final List<CaDefinitionCurveType> cs =
      act.curves().get(CaBoneName.of("bone.000")).get();
    Assert.assertEquals(1L, (long) cs.size());

    final CaDefinitionCurveOrientationType c =
      (CaDefinitionCurveOrientationType) cs.get(0);
    Assert.assertEquals(1L, (long) c.keyframes().size());

    final CaDefinitionCurveKeyframeOrientationType kf = c.keyframes().get(0);
    Assert.assertEquals(new QuaternionI4D(0.0, 1.0, 2.0, 3.0), kf.orientation());
    Assert.assertEquals(CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR, kf.interpolation());
    Assert.assertEquals(CaCurveEasing.CURVE_EASING_IN_OUT, kf.easing());
  }

  @Test
  public void testDuplicateAction()
  {
    final CaV1JSONParser p = new CaV1JSONParser(CaJSON.createMapper());
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("dup_action.caj"), uri("dup_action.caj"));

    dump(r);
    Assert.assertFalse(r.isValid());
  }

  @Test
  public void testDuplicateBone()
  {
    final CaV1JSONParser p = new CaV1JSONParser(CaJSON.createMapper());
    final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> r =
      p.parseSkeletonFromStream(resource("dup_bone.caj"), uri("dup_bone.caj"));

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
      return CaV1JSONParserTest.class.getResource(s).toURI();
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static InputStream resource(final String s)
  {
    return CaV1JSONParserTest.class.getResourceAsStream(s);
  }
}
