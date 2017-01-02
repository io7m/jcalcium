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

package com.io7m.jcalcium.core.compiled.actions;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.Tuple;
import javaslang.collection.IndexedSeq;
import javaslang.collection.SortedMap;

import static com.io7m.jfunctional.Unit.unit;

/**
 * Functions to scale curve-based actions.
 */

public final class CaActionCurvesScaling
{
  private CaActionCurvesScaling()
  {
    throw new UnreachableCodeException();
  }

  private static CaCurveTranslation scaleCurveTranslation(
    final CaCurveTranslationType c,
    final double scale)
  {
    return CaCurveTranslation.builder()
      .from(c)
      .setKeyframes(scaleCurveTranslationKeyframes(c.keyframes(), scale))
      .build();
  }

  private static SortedMap<Integer, CaCurveKeyframeTranslation>
  scaleCurveTranslationKeyframes(
    final SortedMap<Integer, CaCurveKeyframeTranslation> keyframes,
    final double scale)
  {
    return keyframes.map((index, keyframe) -> {
      final int scaled_index = (int) Math.floor(index.doubleValue() * scale);
      return Tuple.of(
        Integer.valueOf(scaled_index), keyframe.withIndex(scaled_index));
    });
  }

  private static CaCurveOrientation scaleCurveOrientation(
    final CaCurveOrientationType c,
    final double scale)
  {
    return CaCurveOrientation.builder()
      .from(c)
      .setKeyframes(scaleCurveOrientationKeyframes(c.keyframes(), scale))
      .build();
  }

  private static SortedMap<Integer, CaCurveKeyframeOrientation>
  scaleCurveOrientationKeyframes(
    final SortedMap<Integer, CaCurveKeyframeOrientation> keyframes,
    final double scale)
  {
    return keyframes.map((index, keyframe) -> {
      final int scaled_index = (int) Math.floor(index.doubleValue() * scale);
      return Tuple.of(
        Integer.valueOf(scaled_index), keyframe.withIndex(scaled_index));
    });
  }

  private static CaCurveScale scaleCurveScale(
    final CaCurveScaleType c,
    final double scale)
  {
    return CaCurveScale.builder()
      .from(c)
      .setKeyframes(scaleCurveScaleKeyframes(c.keyframes(), scale))
      .build();
  }

  private static SortedMap<Integer, CaCurveKeyframeScale>
  scaleCurveScaleKeyframes(
    final SortedMap<Integer, CaCurveKeyframeScale> keyframes,
    final double scale)
  {
    return keyframes.map((index, keyframe) -> {
      final int scaled_index = (int) Math.floor(index.doubleValue() * scale);
      return Tuple.of(
        Integer.valueOf(scaled_index), keyframe.withIndex(scaled_index));
    });
  }

  /**
   * Scale the given action to the target frame rate.
   *
   * @param action     The action
   * @param target_fps The target frame rate
   *
   * @return A scaled action
   */

  public static CaActionCurvesType scale(
    final CaActionCurvesType action,
    final int target_fps)
  {
    NullCheck.notNull(action, "Action");

    Preconditions.checkPreconditionI(
      target_fps,
      target_fps > 0,
      i -> "Target FPS must be > 0");

    if (action.framesPerSecond() == target_fps) {
      return action;
    }

    final double scale =
      (double) target_fps / (double) action.framesPerSecond();

    final SortedMap<CaBoneName, IndexedSeq<CaCurveType>> curves =
      action.curves().map(
        (bone_name, bone_curves) ->
          Tuple.of(bone_name, bone_curves.map(
            curve -> curve.matchCurve(
              unit(),
              (ignored, c) -> scaleCurveTranslation(c, scale),
              (ignored, c) -> scaleCurveOrientation(c, scale),
              (ignored, c) -> scaleCurveScale(c, scale)))));

    return CaActionCurves.of(action.name(), target_fps, curves);
  }
}
