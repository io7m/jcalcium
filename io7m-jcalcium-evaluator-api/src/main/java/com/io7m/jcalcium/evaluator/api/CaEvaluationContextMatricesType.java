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

package com.io7m.jcalcium.evaluator.api;

import com.io7m.jtensors.Matrix4x4DType;

/**
 * A set of temporary matrices.
 */

public interface CaEvaluationContextMatricesType extends AutoCloseable
{
  /**
   * Close the instance.
   *
   * @throws IllegalStateException Iff the instance has already been closed
   */

  @Override
  void close()
    throws IllegalStateException;

  /**
   * @return A 4x4 matrix
   */

  Matrix4x4DType accumulated4x4D();

  /**
   * @return A 4x4 matrix
   */

  Matrix4x4DType translation4x4D();

  /**
   * @return A 4x4 matrix
   */

  Matrix4x4DType orientation4x4D();

  /**
   * @return A 4x4 matrix
   */

  Matrix4x4DType scale4x4D();
}
