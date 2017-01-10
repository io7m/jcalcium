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

package com.io7m.jcalcium.mesh.processing.core;

import com.io7m.jcalcium.core.CaImmutableStyleType;
import com.io7m.jtensors.VectorI4D;
import com.io7m.jtensors.VectorI4L;
import javaslang.collection.Vector;
import org.immutables.javaslang.encodings.JavaslangEncodingEnabled;
import org.immutables.value.Value;

/**
 * <p>A set of aggregated weights for a mesh.</p>
 *
 * <p>The <i>aggregated weights</i> for a vertex are the weights of the four
 * bones that affect a given vertex the most out of all bones in a given
 * skeleton. The weights and indices of the bones are stored such that for a
 * given vertex index {@code i}, {@code weights[i].x} is the amount by which the
 * bone with index {@code indices[i].x} affects the vertex. {@code weights[i].y}
 * is the amount by which the bone with index {@code indices[i].y} affects the
 * vertex, and so on. The weights in {@code weights[i]} are normalized such that
 * {@code VectorI4D.magnitude(weights[i]) == 1.0}.</p>
 */

@Value.Immutable
@CaImmutableStyleType
@JavaslangEncodingEnabled
public interface CaMeshWeightsAggregatedType
{
  /**
   * @return The per-vertex weights
   */

  @Value.Parameter
  Vector<VectorI4D> vertexWeights();

  /**
   * @return The per-vertex bone indices
   */

  @Value.Parameter
  Vector<VectorI4L> vertexBoneIndices();

  /**
   * @return The required size in bits to store bone indices (will be {@code
   * {8|16|32|64}})
   */

  @Value.Derived
  default int indexBitsRequired()
  {
    final Long max = this.vertexBoneIndices().foldLeft(
      Long.valueOf(0L), (Long m, VectorI4L v) -> {
        final long mm = m.longValue();
        long k = Math.max(mm, v.getXL());
        k = Math.max(mm, v.getYL());
        k = Math.max(mm, v.getZL());
        k = Math.max(mm, v.getWL());
        return Long.valueOf(k);
      });

    int required = 8;
    if (max.longValue() >= 255L) {
      required = 16;
    }
    if (max.longValue() >= 65535L) {
      required = 32;
    }
    if (max.longValue() >= 4294967295L) {
      required = 64;
    }
    return required;
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    if (this.vertexWeights().size() != this.vertexBoneIndices().size()) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Weights and indices arrays must be of the same size.");
      sb.append(System.lineSeparator());
      sb.append("  Weights: ");
      sb.append(this.vertexWeights().size());
      sb.append(System.lineSeparator());
      sb.append("  Indices: ");
      sb.append(this.vertexBoneIndices().size());
      sb.append(System.lineSeparator());
      throw new IllegalArgumentException(sb.toString());
    }
  }
}
