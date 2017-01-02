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

package com.io7m.jcalcium.core.compiled;

import com.io7m.jcalcium.core.spaces.CaSpaceBoneAbsoluteType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4FType;

import java.util.NoSuchElementException;

/**
 * Transform matrices for the rest pose of a skeleton, with single-precision
 * elements.
 */

public interface CaSkeletonTransformsFType
{
  /**
   * @param bone_id A bone ID
   *
   * @return The absolute transform for the given bone
   *
   * @throws NoSuchElementException If the bone does not exist
   */

  PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceBoneAbsoluteType>
  transformAbsolute4x4F(
    final int bone_id)
    throws NoSuchElementException;
}
