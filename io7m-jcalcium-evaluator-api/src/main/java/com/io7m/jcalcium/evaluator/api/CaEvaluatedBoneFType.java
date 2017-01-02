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

import com.io7m.jcalcium.core.spaces.CaSpaceBoneAbsoluteType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jtensors.QuaternionReadable4FType;
import com.io7m.jtensors.VectorReadable3FType;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4FType;
import com.io7m.jtensors.parameterized.PVectorReadable3FType;

/**
 * The type of evaluated bones with single-precision components.
 */

public interface CaEvaluatedBoneFType extends CaEvaluatedBoneType
{
  /**
   * @return The absolute transform for the bone
   */

  PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> absoluteTransform4x4F();

  /**
   * @return The parent-relative offset for the bone
   */

  PVectorReadable3FType<CaSpaceBoneParentRelativeType> translation3F();

  /**
   * @return The parent-relative orientation of the bone
   */

  QuaternionReadable4FType orientation4F();

  /**
   * @return The parent-relative scale of the bone
   */

  VectorReadable3FType scale3F();
}
