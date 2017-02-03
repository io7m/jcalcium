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
import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.VectorM3D;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * The default implementation of the {@link CaEvaluationContextType} interface.
 */

public final class CaEvaluationContext implements CaEvaluationContextType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaEvaluationContext.class);
  }

  private final ReferencePool<Matrices> matrices;
  private final ReferencePool<Vectors> vectors;

  private interface PooledType
  {
    void open();
  }

  private static final class ReferencePool<T extends PooledType>
  {
    private final ReferenceOpenHashSet<T> free;
    private final ReferenceOpenHashSet<T> used;
    private final int free_max;
    private final Function<ReferencePool<T>, T> supplier;

    ReferencePool(
      final Function<ReferencePool<T>, T> in_supplier,
      final int in_free_max)
    {
      this.supplier = in_supplier;
      this.free = new ReferenceOpenHashSet<>();
      this.used = new ReferenceOpenHashSet<>();
      this.free_max = in_free_max;
    }

    private void untake(final T v)
    {
      LOG.trace("return {}", v);
      this.used.remove(v);
      if (this.free.size() < this.free_max) {
        this.free.add(v);
      }
    }

    private T take()
    {
      final T m;
      if (this.free.isEmpty()) {
        m = this.supplier.apply(this);
        LOG.trace("new {}", m);
      } else {
        final ObjectIterator<T> i = this.free.iterator();
        m = i.next();
        i.remove();
        LOG.trace("reuse {}", m);
      }

      m.open();
      this.used.add(m);
      return m;
    }
  }

  private CaEvaluationContext(
    final int in_matrices_free_max,
    final int in_vectors_free_max)
  {
    this.matrices = new ReferencePool<>(Matrices::new, in_matrices_free_max);
    this.vectors = new ReferencePool<>(Vectors::new, in_vectors_free_max);
  }

  /**
   * Create a new evaluation context.
   *
   * @return A new evaluation context
   */

  public static CaEvaluationContextType create()
  {
    return new CaEvaluationContext(8, 8);
  }

  @Override
  public CaEvaluationContextMatricesType newMatrices()
  {
    return this.matrices.take();
  }

  @Override
  public CaEvaluationContextVectorsType newVectors()
  {
    return this.vectors.take();
  }

  private static final class Vectors extends AbstractPooled<Vectors>
    implements CaEvaluationContextVectorsType
  {
    private final QuaternionM4D.ContextQM4D quaternion_m4d;
    private final VectorM3D.ContextVM3D vector_m3d;

    private Vectors(
      final ReferencePool<Vectors> in_pool)
    {
      super(in_pool);
      this.quaternion_m4d = new QuaternionM4D.ContextQM4D();
      this.vector_m3d = new VectorM3D.ContextVM3D();
    }

    @Override
    public QuaternionM4D.ContextQM4D quaternionContext4D()
    {
      super.checkOpen();
      return this.quaternion_m4d;
    }

    @Override
    public VectorM3D.ContextVM3D vectorContext3D()
    {
      super.checkOpen();
      return this.vector_m3d;
    }
  }

  private static final class Matrices extends AbstractPooled<Matrices>
    implements CaEvaluationContextMatricesType
  {
    private final Matrix4x4DType m_accumulated4x4d;
    private final Matrix4x4DType m_translation4x4d;
    private final Matrix4x4DType m_orientation4x4d;
    private final Matrix4x4DType m_scale4x4d;

    private Matrices(final ReferencePool<Matrices> in_pool)
    {
      super(in_pool);
      this.m_accumulated4x4d = MatrixHeapArrayM4x4D.newMatrix();
      this.m_orientation4x4d = MatrixHeapArrayM4x4D.newMatrix();
      this.m_translation4x4d = MatrixHeapArrayM4x4D.newMatrix();
      this.m_scale4x4d = MatrixHeapArrayM4x4D.newMatrix();
    }

    @Override
    public Matrix4x4DType accumulated4x4D()
    {
      super.checkOpen();
      return this.m_accumulated4x4d;
    }

    @Override
    public Matrix4x4DType translation4x4D()
    {
      super.checkOpen();
      return this.m_translation4x4d;
    }

    @Override
    public Matrix4x4DType orientation4x4D()
    {
      super.checkOpen();
      return this.m_orientation4x4d;
    }

    @Override
    public Matrix4x4DType scale4x4D()
    {
      super.checkOpen();
      return this.m_scale4x4d;
    }
  }

  private static abstract class AbstractPooled<T extends PooledType> implements
    PooledType, AutoCloseable
  {
    private final ReferencePool<T> pool;
    private boolean open;

    AbstractPooled(
      final ReferencePool<T> in_pool)
    {
      this.pool = in_pool;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void close()
      throws IllegalStateException
    {
      this.checkOpen();
      this.open = false;
      this.pool.untake((T) this);
    }

    final void checkOpen()
    {
      if (!this.open) {
        throw new IllegalStateException("This context has already been returned");
      }
    }

    @Override
    public final void open()
    {
      this.open = true;
    }
  }
}
