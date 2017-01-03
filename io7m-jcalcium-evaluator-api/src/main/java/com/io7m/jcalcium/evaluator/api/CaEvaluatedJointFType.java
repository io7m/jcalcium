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

import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectDeformedType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jtensors.QuaternionReadable4FType;
import com.io7m.jtensors.VectorReadable3FType;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4FType;
import com.io7m.jtensors.parameterized.PVectorReadable3FType;

/**
 * The type of evaluated joints with single-precision components.
 */

public interface CaEvaluatedJointFType extends CaEvaluatedJointType
{
  /**
   * A specification of the matrix that represents the concatenation of all
   * joint transforms up to and including the current joint. This is a matrix
   * that, for example, if used to transform a vector {@code (0.0, 0.0, 0.0,
   * 1.0)} will yield a vector equal to the current object-space position of the
   * joint.
   *
   * @return The absolute transform for the joint
   */

  PMatrixReadable4x4FType<CaSpaceJointType, CaSpaceObjectType> transformJointObject4x4F();

  /**
   * A specification of the matrix that deforms an object-space position and
   * yields (unsurprisingly) a deformed object-space position. This is the
   * matrix that is used, for example, to deform meshes on the GPU when
   * animating.
   *
   * @return The deform transform for the joint
   */

  PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> transformDeform4x4F();

  /**
   * @return The parent-relative offset for the joint
   */

  PVectorReadable3FType<CaSpaceJointType> translation3F();

  /**
   * @return The parent-relative orientation of the joint
   */

  QuaternionReadable4FType orientation4F();

  /**
   * @return The parent-relative scale of the joint
   */

  VectorReadable3FType scale3F();
}
