/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
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

package com.io7m.jcalcium.mesh.deformation.cpu;

import com.io7m.junreachable.UnreachableCodeException;

/**
 * The default conventions for attributes in deformable meshes.
 */

public final class CaMeshDeformableAttributes
{
  /**
   * The default configuration for mesh vertex position data.
   */

  public static final CaMeshDeformableAttributeSourceSelection POSITION;

  /**
   * The default configuration for mesh tangent vectors.
   */

  public static final CaMeshDeformableAttributeSourceSelection TANGENT4;

  /**
   * The default configuration for mesh normal vectors.
   */

  public static final CaMeshDeformableAttributeSourceSelection NORMAL;

  static {
    POSITION = CaMeshDeformableAttributeSourceSelection.of(
      "POSITION",
      CaMeshDeformableAttributeSemantic.POSITION,
      CaMeshDeformableAttributeCursorKind.CURSOR_FLOAT_3);

    TANGENT4 = CaMeshDeformableAttributeSourceSelection.of(
      "TANGENT4",
      CaMeshDeformableAttributeSemantic.DIRECTION,
      CaMeshDeformableAttributeCursorKind.CURSOR_FLOAT_4);

    NORMAL = CaMeshDeformableAttributeSourceSelection.of(
      "NORMAL",
      CaMeshDeformableAttributeSemantic.DIRECTION,
      CaMeshDeformableAttributeCursorKind.CURSOR_FLOAT_3);
  }

  private CaMeshDeformableAttributes()
  {
    throw new UnreachableCodeException();
  }
}
