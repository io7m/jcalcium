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

package com.io7m.jcalcium.tests.evaluator.main;

import com.io7m.jcalcium.compiler.api.CaCompileError;
import com.io7m.jcalcium.compiler.api.CaCompilerProviderType;
import com.io7m.jcalcium.compiler.api.CaCompilerType;
import com.io7m.jcalcium.compiler.main.CaCompilerProvider;
import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurves;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jcalcium.evaluator.api.CaActionEvaluatorCurvesDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContext;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextType;
import com.io7m.jcalcium.evaluator.main.CaActionEvaluatorCurves;
import com.io7m.jcalcium.format.json.jackson.CaJSONFormatProvider;
import com.io7m.jcalcium.parser.api.CaDefinitionParserFormatProviderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jtensors.Quaternion4DType;
import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.Vector3DType;
import com.io7m.jtensors.VectorM3D;
import com.io7m.jtensors.parameterized.PVector3DType;
import com.io7m.jtensors.parameterized.PVectorM3D;
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.collection.List;
import javaslang.control.Validation;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Random;

public final class CaActionEvaluatorCurvesTest
{
  private CaSkeleton compile(
    final String name)
    throws IOException
  {
    final Class<CaActionEvaluatorCurvesTest> c =
      CaActionEvaluatorCurvesTest.class;
    final CaDefinitionParserFormatProviderType format =
      new CaJSONFormatProvider();
    final CaCompilerProviderType compiler_prov =
      new CaCompilerProvider();

    try (final InputStream is = c.getResourceAsStream(name)) {
      final CaDefinitionParserType parser = format.parserCreate();
      final Validation<List<CaParseError>, CaDefinitionSkeleton> pr =
        parser.parseSkeletonFromStream(is, URI.create(name));
      if (pr.isValid()) {
        final CaDefinitionSkeleton skel_d = pr.get();
        final CaCompilerType compiler = compiler_prov.create();
        final Validation<List<CaCompileError>, CaSkeleton> cr =
          compiler.compile(skel_d);
        if (cr.isValid()) {
          return cr.get();
        }
      }
    }

    throw new UnreachableCodeException();
  }

  @Test
  public void testSingleTranslateLinear()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-translate-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final Vector3DType s_out =
      new VectorM3D();
    final PVector3DType<CaSpaceJointType> t_out =
      new PVectorM3D<>();
    final Quaternion4DType q_out =
      new QuaternionM4D();
    final QuaternionM4D.ContextQM4D c =
      new QuaternionM4D.ContextQM4D();

    eval.evaluateScale3DForGlobalFrame(0, 0L, 0L, 1.0, s_out);
    Assert.assertEquals(1.0, s_out.getXD(), 0.0);
    Assert.assertEquals(1.0, s_out.getYD(), 0.0);
    Assert.assertEquals(1.0, s_out.getZD(), 0.0);
    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 0L, 1.0, q_out);
    Assert.assertEquals(0.0, q_out.getXD(), 0.0);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(1.0, q_out.getWD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 0L, 1.0, t_out);
    Assert.assertEquals(0.0, t_out.getXD(), 0.0);
    Assert.assertEquals(0.0, t_out.getYD(), 0.0);
    Assert.assertEquals(0.0, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 5L, 1.0, t_out);
    Assert.assertEquals(0.5, t_out.getXD(), 0.0);
    Assert.assertEquals(0.5, t_out.getYD(), 0.0);
    Assert.assertEquals(0.5, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 10L, 1.0, t_out);
    Assert.assertEquals(1.0, t_out.getXD(), 0.0);
    Assert.assertEquals(1.0, t_out.getYD(), 0.0);
    Assert.assertEquals(1.0, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 15L, 1.0, t_out);
    Assert.assertEquals(1.5, t_out.getXD(), 0.0);
    Assert.assertEquals(1.5, t_out.getYD(), 0.0);
    Assert.assertEquals(1.5, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 20L, 1.0, t_out);
    Assert.assertEquals(2.0, t_out.getXD(), 0.0);
    Assert.assertEquals(2.0, t_out.getYD(), 0.0);
    Assert.assertEquals(2.0, t_out.getZD(), 0.0);
  }

  @Test
  public void testSingleTranslateLinearScaled0_5()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-translate-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final PVector3DType<CaSpaceJointType> t_out =
      new PVectorM3D<>();

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 0L, 0.5, t_out);
    Assert.assertEquals(0.0, t_out.getXD(), 0.0);
    Assert.assertEquals(0.0, t_out.getYD(), 0.0);
    Assert.assertEquals(0.0, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 5L, 0.5, t_out);
    Assert.assertEquals(0.25, t_out.getXD(), 0.0);
    Assert.assertEquals(0.25, t_out.getYD(), 0.0);
    Assert.assertEquals(0.25, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 10L, 0.5, t_out);
    Assert.assertEquals(0.5, t_out.getXD(), 0.0);
    Assert.assertEquals(0.5, t_out.getYD(), 0.0);
    Assert.assertEquals(0.5, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 15L, 0.5, t_out);
    Assert.assertEquals(0.75, t_out.getXD(), 0.0);
    Assert.assertEquals(0.75, t_out.getYD(), 0.0);
    Assert.assertEquals(0.75, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 20L, 0.5, t_out);
    Assert.assertEquals(1.0, t_out.getXD(), 0.0);
    Assert.assertEquals(1.0, t_out.getYD(), 0.0);
    Assert.assertEquals(1.0, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 25L, 0.5, t_out);
    Assert.assertEquals(1.25, t_out.getXD(), 0.0);
    Assert.assertEquals(1.25, t_out.getYD(), 0.0);
    Assert.assertEquals(1.25, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 30L, 0.5, t_out);
    Assert.assertEquals(1.5, t_out.getXD(), 0.0);
    Assert.assertEquals(1.5, t_out.getYD(), 0.0);
    Assert.assertEquals(1.5, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 35L, 0.5, t_out);
    Assert.assertEquals(1.75, t_out.getXD(), 0.0);
    Assert.assertEquals(1.75, t_out.getYD(), 0.0);
    Assert.assertEquals(1.75, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 40L, 0.5, t_out);
    Assert.assertEquals(2.0, t_out.getXD(), 0.0);
    Assert.assertEquals(2.0, t_out.getYD(), 0.0);
    Assert.assertEquals(2.0, t_out.getZD(), 0.0);
  }

  @Test
  public void testSingleTranslateLinearScaled2_0()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-translate-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final PVector3DType<CaSpaceJointType> t_out =
      new PVectorM3D<>();

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 0L, 2.0, t_out);
    Assert.assertEquals(0.0, t_out.getXD(), 0.0);
    Assert.assertEquals(0.0, t_out.getYD(), 0.0);
    Assert.assertEquals(0.0, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 5L, 2.0, t_out);
    Assert.assertEquals(1.0, t_out.getXD(), 0.0);
    Assert.assertEquals(1.0, t_out.getYD(), 0.0);
    Assert.assertEquals(1.0, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 10L, 2.0, t_out);
    Assert.assertEquals(2.0, t_out.getXD(), 0.0);
    Assert.assertEquals(2.0, t_out.getYD(), 0.0);
    Assert.assertEquals(2.0, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 15L, 2.0, t_out);
    Assert.assertEquals(0.9, t_out.getXD(), 0.0);
    Assert.assertEquals(0.9, t_out.getYD(), 0.0);
    Assert.assertEquals(0.9, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 20L, 2.0, t_out);
    Assert.assertEquals(1.9, t_out.getXD(), 0.0);
    Assert.assertEquals(1.9, t_out.getYD(), 0.0);
    Assert.assertEquals(1.9, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 25L, 2.0, t_out);
    Assert.assertEquals(0.8, t_out.getXD(), 0.0);
    Assert.assertEquals(0.8, t_out.getYD(), 0.0);
    Assert.assertEquals(0.8, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 30L, 2.0, t_out);
    Assert.assertEquals(1.8, t_out.getXD(), 0.0);
    Assert.assertEquals(1.8, t_out.getYD(), 0.0);
    Assert.assertEquals(1.8, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 35L, 2.0, t_out);
    Assert.assertEquals(0.7, t_out.getXD(), 0.0);
    Assert.assertEquals(0.7, t_out.getYD(), 0.0);
    Assert.assertEquals(0.7, t_out.getZD(), 0.0);

    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 40L, 2.0, t_out);
    Assert.assertEquals(1.7, t_out.getXD(), 0.0);
    Assert.assertEquals(1.7, t_out.getYD(), 0.0);
    Assert.assertEquals(1.7, t_out.getZD(), 0.0);
  }

  @Test
  public void testSingleScaleLinear()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-scale-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final Vector3DType s_out =
      new VectorM3D();
    final PVector3DType<CaSpaceJointType> t_out =
      new PVectorM3D<>();
    final Quaternion4DType q_out =
      new QuaternionM4D();

    eval.evaluateScale3DForGlobalFrame(0, 0L, 0L, 1.0, t_out);
    Assert.assertEquals(0.0, t_out.getXD(), 0.0);
    Assert.assertEquals(0.0, t_out.getYD(), 0.0);
    Assert.assertEquals(0.0, t_out.getZD(), 0.0);
    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 0L, 1.0, q_out);
    Assert.assertEquals(0.0, q_out.getXD(), 0.0);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(1.0, q_out.getWD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 0L, 1.0, s_out);
    Assert.assertEquals(0.0, s_out.getXD(), 0.0);
    Assert.assertEquals(0.0, s_out.getYD(), 0.0);
    Assert.assertEquals(0.0, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 5L, 1.0, s_out);
    Assert.assertEquals(0.5, s_out.getXD(), 0.0);
    Assert.assertEquals(0.5, s_out.getYD(), 0.0);
    Assert.assertEquals(0.5, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 10L, 1.0, s_out);
    Assert.assertEquals(1.0, s_out.getXD(), 0.0);
    Assert.assertEquals(1.0, s_out.getYD(), 0.0);
    Assert.assertEquals(1.0, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 15L, 1.0, s_out);
    Assert.assertEquals(1.5, s_out.getXD(), 0.0);
    Assert.assertEquals(1.5, s_out.getYD(), 0.0);
    Assert.assertEquals(1.5, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 20L, 1.0, s_out);
    Assert.assertEquals(2.0, s_out.getXD(), 0.0);
    Assert.assertEquals(2.0, s_out.getYD(), 0.0);
    Assert.assertEquals(2.0, s_out.getZD(), 0.0);
  }

  @Test
  public void testSingleScaleLinearScaled0_5()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-scale-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final Vector3DType s_out =
      new VectorM3D();

    eval.evaluateScale3DForGlobalFrame(0, 0L, 0L, 0.5, s_out);
    Assert.assertEquals(0.0, s_out.getXD(), 0.0);
    Assert.assertEquals(0.0, s_out.getYD(), 0.0);
    Assert.assertEquals(0.0, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 5L, 0.5, s_out);
    Assert.assertEquals(0.25, s_out.getXD(), 0.0);
    Assert.assertEquals(0.25, s_out.getYD(), 0.0);
    Assert.assertEquals(0.25, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 10L, 0.5, s_out);
    Assert.assertEquals(0.5, s_out.getXD(), 0.0);
    Assert.assertEquals(0.5, s_out.getYD(), 0.0);
    Assert.assertEquals(0.5, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 15L, 0.5, s_out);
    Assert.assertEquals(0.75, s_out.getXD(), 0.0);
    Assert.assertEquals(0.75, s_out.getYD(), 0.0);
    Assert.assertEquals(0.75, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 20L, 0.5, s_out);
    Assert.assertEquals(1.0, s_out.getXD(), 0.0);
    Assert.assertEquals(1.0, s_out.getYD(), 0.0);
    Assert.assertEquals(1.0, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 25L, 0.5, s_out);
    Assert.assertEquals(1.25, s_out.getXD(), 0.0);
    Assert.assertEquals(1.25, s_out.getYD(), 0.0);
    Assert.assertEquals(1.25, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 30L, 0.5, s_out);
    Assert.assertEquals(1.5, s_out.getXD(), 0.0);
    Assert.assertEquals(1.5, s_out.getYD(), 0.0);
    Assert.assertEquals(1.5, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 35L, 0.5, s_out);
    Assert.assertEquals(1.75, s_out.getXD(), 0.0);
    Assert.assertEquals(1.75, s_out.getYD(), 0.0);
    Assert.assertEquals(1.75, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 40L, 0.5, s_out);
    Assert.assertEquals(2.0, s_out.getXD(), 0.0);
    Assert.assertEquals(2.0, s_out.getYD(), 0.0);
    Assert.assertEquals(2.0, s_out.getZD(), 0.0);
  }

  @Test
  public void testSingleScaleLinearScaled2_0()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-scale-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final Vector3DType s_out =
      new VectorM3D();

    eval.evaluateScale3DForGlobalFrame(0, 0L, 0L, 2.0, s_out);
    Assert.assertEquals(0.0, s_out.getXD(), 0.0);
    Assert.assertEquals(0.0, s_out.getYD(), 0.0);
    Assert.assertEquals(0.0, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 5L, 2.0, s_out);
    Assert.assertEquals(1.0, s_out.getXD(), 0.0);
    Assert.assertEquals(1.0, s_out.getYD(), 0.0);
    Assert.assertEquals(1.0, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 10L, 2.0, s_out);
    Assert.assertEquals(2.0, s_out.getXD(), 0.0);
    Assert.assertEquals(2.0, s_out.getYD(), 0.0);
    Assert.assertEquals(2.0, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 15L, 2.0, s_out);
    Assert.assertEquals(0.9, s_out.getXD(), 0.0);
    Assert.assertEquals(0.9, s_out.getYD(), 0.0);
    Assert.assertEquals(0.9, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 20L, 2.0, s_out);
    Assert.assertEquals(1.9, s_out.getXD(), 0.0);
    Assert.assertEquals(1.9, s_out.getYD(), 0.0);
    Assert.assertEquals(1.9, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 25L, 2.0, s_out);
    Assert.assertEquals(0.8, s_out.getXD(), 0.0);
    Assert.assertEquals(0.8, s_out.getYD(), 0.0);
    Assert.assertEquals(0.8, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 30L, 2.0, s_out);
    Assert.assertEquals(1.8, s_out.getXD(), 0.0);
    Assert.assertEquals(1.8, s_out.getYD(), 0.0);
    Assert.assertEquals(1.8, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 35L, 2.0, s_out);
    Assert.assertEquals(0.7, s_out.getXD(), 0.0);
    Assert.assertEquals(0.7, s_out.getYD(), 0.0);
    Assert.assertEquals(0.7, s_out.getZD(), 0.0);

    eval.evaluateScale3DForGlobalFrame(0, 0L, 40L, 2.0, s_out);
    Assert.assertEquals(1.7, s_out.getXD(), 0.0);
    Assert.assertEquals(1.7, s_out.getYD(), 0.0);
    Assert.assertEquals(1.7, s_out.getZD(), 0.0);
  }

  @Test
  public void testSingleOrientLinear()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-orient-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final Vector3DType s_out =
      new VectorM3D();
    final PVector3DType<CaSpaceJointType> t_out =
      new PVectorM3D<>();
    final Quaternion4DType q_out =
      new QuaternionM4D();

    eval.evaluateScale3DForGlobalFrame(0, 0L, 0L, 1.0, s_out);
    Assert.assertEquals(1.0, s_out.getXD(), 0.0);
    Assert.assertEquals(1.0, s_out.getYD(), 0.0);
    Assert.assertEquals(1.0, s_out.getZD(), 0.0);
    eval.evaluateTranslation3DForGlobalFrame(0, 0L, 0L, 1.0, t_out);
    Assert.assertEquals(0.0, t_out.getXD(), 0.0);
    Assert.assertEquals(0.0, t_out.getYD(), 0.0);
    Assert.assertEquals(0.0, t_out.getZD(), 0.0);

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 0L, 1.0, q_out);
    Assert.assertEquals(0.0, q_out.getXD(), 0.0);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(1.0, q_out.getWD(), 0.0);

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 10L, 1.0, q_out);
    Assert.assertEquals(0.707106, q_out.getXD(), 0.000001);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(0.707106, q_out.getWD(), 0.000001);

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 20L, 1.0, q_out);
    Assert.assertEquals(1.0, q_out.getXD(), 0.0);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(0.0, q_out.getWD(), 0.0);
  }

  @Test
  public void testSingleOrientLinearScaled0_5()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-orient-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final Quaternion4DType q_out =
      new QuaternionM4D();

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 0L, 0.5, q_out);
    Assert.assertEquals(0.0, q_out.getXD(), 0.0);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(1.0, q_out.getWD(), 0.0);

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 10L, 0.5, q_out);
    Assert.assertEquals(0.38268, q_out.getXD(), 0.00001);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(0.92387, q_out.getWD(), 0.00001);

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 20L, 0.5, q_out);
    Assert.assertEquals(0.707106, q_out.getXD(), 0.00001);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(0.707106, q_out.getWD(), 0.00001);

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 40L, 0.5, q_out);
    Assert.assertEquals(1.0, q_out.getXD(), 0.0);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(0.0, q_out.getWD(), 0.0);
  }

  @Test
  public void testSingleOrientLinearScaled2_0()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-orient-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final Quaternion4DType q_out =
      new QuaternionM4D();

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 0L, 2.0, q_out);
    Assert.assertEquals(0.0, q_out.getXD(), 0.0);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(1.0, q_out.getWD(), 0.0);

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 5L, 2.0, q_out);
    Assert.assertEquals(0.707106, q_out.getXD(), 0.00001);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(0.707106, q_out.getWD(), 0.00001);

    eval.evaluateOrientation4DForGlobalFrame(0, 0L, 10L, 2.0, q_out);
    Assert.assertEquals(1.0, q_out.getXD(), 0.0);
    Assert.assertEquals(0.0, q_out.getYD(), 0.0);
    Assert.assertEquals(0.0, q_out.getZD(), 0.0);
    Assert.assertEquals(0.0, q_out.getWD(), 0.0);
  }

  @Test
  public void testExtremeTimes()
    throws IOException
  {
    final CaEvaluationContextType context = CaEvaluationContext.create();
    final CaSkeleton skel =
      this.compile("single-translate-linear.csj");
    final CaActionCurves act =
      (CaActionCurves) skel.actionsByName().get(CaActionName.of("action0")).get();
    final CaActionEvaluatorCurvesDType eval =
      CaActionEvaluatorCurves.createD(context, skel, act, 60);

    final Vector3DType s_out =
      new VectorM3D();
    final PVector3DType<CaSpaceJointType> t_out =
      new PVectorM3D<>();
    final Quaternion4DType q_out =
      new QuaternionM4D();

    final Random random = new Random();

    for (int index = 0; index < 100; ++index) {
      eval.evaluateTranslation3DForGlobalFrame(
        0, 0L, random.nextLong(), 1.0, t_out);
    }

    for (int index = 0; index < 100; ++index) {
      eval.evaluateOrientation4DForGlobalFrame(
        0, 0L, random.nextLong(), 1.0, q_out);
    }

    for (int index = 0; index < 100; ++index) {
      eval.evaluateScale3DForGlobalFrame(
        0, 0L, random.nextLong(), 1.0, s_out);
    }
  }
}
