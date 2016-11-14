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

package com.io7m.jcalcium.core.definitions.actions;

import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.core.ImmutableStyleType;
import com.io7m.jtensors.parameterized.PVectorI3D;
import org.immutables.value.Value;

/**
 * A keyframe for a curve that affects the translation of a bone.
 */

@ImmutableStyleType
@Value.Immutable
public interface CaDefinitionCurveKeyframeTranslationType extends
  CaDefinitionCurveKeyframeType
{
  /**
   * @return The translation of the bone at this keyframe
   */

  PVectorI3D<CaSpaceBoneParentRelativeType> translation();
}
