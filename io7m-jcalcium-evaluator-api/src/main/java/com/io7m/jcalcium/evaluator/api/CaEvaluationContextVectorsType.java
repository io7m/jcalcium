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

import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.VectorM3D;

/**
 * A set of temporary matrices.
 */

public interface CaEvaluationContextVectorsType extends AutoCloseable
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
   * @return A 4D quaternion context
   */

  QuaternionM4D.ContextQM4D quaternionContext4D();

  /**
   * @return A 3D vector context
   */

  VectorM3D.ContextVM3D vectorContext3D();
}
