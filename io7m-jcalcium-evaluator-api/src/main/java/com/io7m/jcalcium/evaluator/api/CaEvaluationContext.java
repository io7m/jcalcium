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
import com.io7m.jtensors.MatrixHeapArrayM4x4D;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of the {@link CaEvaluationContextType} interface.
 */

public final class CaEvaluationContext implements CaEvaluationContextType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaEvaluationContext.class);
  }

  private final ReferenceOpenHashSet<Matrices> matrices_free;
  private final ReferenceOpenHashSet<Matrices> matrices_used;
  private final int matrices_free_max;

  private CaEvaluationContext(
    final int in_matrices_free_max)
  {
    this.matrices_free = new ReferenceOpenHashSet<>();
    this.matrices_used = new ReferenceOpenHashSet<>();
    this.matrices_free_max = in_matrices_free_max;
  }

  /**
   * Create a new evaluation context.
   *
   * @return A new evaluation context
   */

  public static CaEvaluationContextType create()
  {
    return new CaEvaluationContext(8);
  }

  @Override
  public CaEvaluationContextMatricesType newMatrices()
  {
    return this.matricesTake();
  }

  private CaEvaluationContextMatricesType matricesTake()
  {
    final Matrices m;
    if (this.matrices_free.isEmpty()) {
      m = new Matrices();
      LOG.trace("new matrices");
    } else {
      final ObjectIterator<Matrices> i = this.matrices_free.iterator();
      m = i.next();
      i.remove();
      LOG.trace("reuse matrices {}", m);
    }

    m.open = true;
    this.matrices_used.add(m);
    return m;
  }

  private void matricesReturn(
    final Matrices m)
  {
    LOG.trace("return matrices {}", m);
    this.matrices_used.remove(m);
    if (this.matrices_free.size() < this.matrices_free_max) {
      this.matrices_free.add(m);
    }
  }

  private final class Matrices implements CaEvaluationContextMatricesType
  {
    private final Matrix4x4DType m_accumulated4x4d;
    private final Matrix4x4DType m_translation4x4d;
    private final Matrix4x4DType m_orientation4x4d;
    private final Matrix4x4DType m_scale4x4d;
    private boolean open;

    private Matrices()
    {
      this.m_accumulated4x4d = MatrixHeapArrayM4x4D.newMatrix();
      this.m_orientation4x4d = MatrixHeapArrayM4x4D.newMatrix();
      this.m_translation4x4d = MatrixHeapArrayM4x4D.newMatrix();
      this.m_scale4x4d = MatrixHeapArrayM4x4D.newMatrix();
    }

    @Override
    public void close()
      throws IllegalStateException
    {
      this.checkOpen();
      this.open = false;
      CaEvaluationContext.this.matricesReturn(this);
    }

    private void checkOpen()
    {
      if (!this.open) {
        throw new IllegalStateException("Matrices have already been returned");
      }
    }

    @Override
    public Matrix4x4DType accumulated4x4D()
    {
      this.checkOpen();
      return this.m_accumulated4x4d;
    }

    @Override
    public Matrix4x4DType translation4x4D()
    {
      this.checkOpen();
      return this.m_translation4x4d;
    }

    @Override
    public Matrix4x4DType orientation4x4D()
    {
      this.checkOpen();
      return this.m_orientation4x4d;
    }

    @Override
    public Matrix4x4DType scale4x4D()
    {
      this.checkOpen();
      return this.m_scale4x4d;
    }
  }
}
