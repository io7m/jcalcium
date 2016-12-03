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

package com.io7m.jcalcium.evaluator.api;

import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.Quaternion4DType;
import com.io7m.jtensors.QuaternionReadable4DType;
import com.io7m.jtensors.VectorReadable3DType;
import com.io7m.jtensors.VectorWritable3DType;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

import static com.io7m.jinterp.InterpolationD.interpolateLinear;

/**
 * Interpolation curve definitions for evaluators.
 */

public final class CaEvaluatorInterpolation
{
  private CaEvaluatorInterpolation()
  {
    throw new UnreachableCodeException();
  }

  /**
   * <p>Interpolate the given scalar value.</p>
   *
   * <p>The function returns a value in the range {@code [lower, upper]}
   * based on the given {@code alpha} value in the range {@code [0, 1]}.
   * </p>
   *
   * @param easing  The easing type
   * @param interp  The interpolation type
   * @param alpha   The alpha factor
   * @param x_lower The lower value
   * @param x_upper The upper value
   *
   * @return An interpolated value
   */

  public static double interpolateScalar(
    final CaCurveEasing easing,
    final CaCurveInterpolation interp,
    final double alpha,
    final double x_lower,
    final double x_upper)
  {
    NullCheck.notNull(easing, "Easing");
    NullCheck.notNull(interp, "Interpolation");

    switch (interp) {
      case CURVE_INTERPOLATION_CONSTANT:
        return x_upper;
      case CURVE_INTERPOLATION_LINEAR:
        return interpolateLinear(x_lower, x_upper, alpha);
      case CURVE_INTERPOLATION_QUADRATIC:
        throw new UnimplementedCodeException();
      case CURVE_INTERPOLATION_EXPONENTIAL:
        throw new UnimplementedCodeException();
    }

    throw new UnreachableCodeException();
  }

  /**
   * <p>Interpolate the given vector value.</p>
   *
   * <p>The function writes a value in the range {@code [lower, upper]}
   * based on the given {@code alpha} value in the range {@code [0, 1]}
   * to {@code x_out}.
   * </p>
   *
   * @param easing  The easing type
   * @param interp  The interpolation type
   * @param alpha   The alpha factor
   * @param x_lower The lower value
   * @param x_upper The upper value
   * @param x_out   The output value
   */

  public static void interpolateVector3D(
    final CaCurveEasing easing,
    final CaCurveInterpolation interp,
    final double alpha,
    final VectorReadable3DType x_lower,
    final VectorReadable3DType x_upper,
    final VectorWritable3DType x_out)
  {
    NullCheck.notNull(easing, "Easing");
    NullCheck.notNull(interp, "Interpolation");
    NullCheck.notNull(x_lower, "Lower");
    NullCheck.notNull(x_upper, "Upper");
    NullCheck.notNull(x_out, "Output");

    switch (interp) {
      case CURVE_INTERPOLATION_CONSTANT: {
        x_out.copyFrom3D(x_upper);
        return;
      }
      case CURVE_INTERPOLATION_LINEAR: {
        x_out.set3D(
          interpolateLinear(x_lower.getXD(), x_upper.getXD(), alpha),
          interpolateLinear(x_lower.getYD(), x_upper.getYD(), alpha),
          interpolateLinear(x_lower.getZD(), x_upper.getZD(), alpha));
        return;
      }
      case CURVE_INTERPOLATION_QUADRATIC:
        throw new UnimplementedCodeException();
      case CURVE_INTERPOLATION_EXPONENTIAL:
        throw new UnimplementedCodeException();
    }

    throw new UnreachableCodeException();
  }

  /**
   * <p>Interpolate the given quaternion value.</p>
   *
   * <p>The function writes a value in the range {@code [lower, upper]}
   * based on the given {@code alpha} value in the range {@code [0, 1]}
   * to {@code x_out}.
   * </p>
   *
   * @param easing  The easing type
   * @param interp  The interpolation type
   * @param alpha   The alpha factor
   * @param q_lower The lower value
   * @param q_upper The upper value
   * @param q_out   The output value
   */

  public static void interpolateQuaternion4D(
    final CaCurveEasing easing,
    final CaCurveInterpolation interp,
    final double alpha,
    final QuaternionReadable4DType q_lower,
    final QuaternionReadable4DType q_upper,
    final Quaternion4DType q_out)
  {
    NullCheck.notNull(easing, "Easing");
    NullCheck.notNull(interp, "Interpolation");
    NullCheck.notNull(q_lower, "Lower");
    NullCheck.notNull(q_upper, "Upper");
    NullCheck.notNull(q_out, "Output");

    switch (interp) {
      case CURVE_INTERPOLATION_CONSTANT: {
        q_out.copyFrom4D(q_upper);
        return;
      }
      case CURVE_INTERPOLATION_LINEAR: {
        q_out.set4D(
          interpolateLinear(q_lower.getXD(), q_upper.getXD(), alpha),
          interpolateLinear(q_lower.getYD(), q_upper.getYD(), alpha),
          interpolateLinear(q_lower.getZD(), q_upper.getZD(), alpha),
          interpolateLinear(q_lower.getWD(), q_upper.getWD(), alpha));
        return;
      }
      case CURVE_INTERPOLATION_QUADRATIC:
        throw new UnimplementedCodeException();
      case CURVE_INTERPOLATION_EXPONENTIAL:
        throw new UnimplementedCodeException();
    }

    throw new UnreachableCodeException();
  }
}
