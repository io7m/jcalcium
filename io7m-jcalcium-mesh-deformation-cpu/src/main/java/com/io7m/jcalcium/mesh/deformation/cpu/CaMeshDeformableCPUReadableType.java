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

import com.io7m.jtensors.VectorWritable4DType;
import com.io7m.jtensors.VectorWritable4LType;
import javaslang.collection.SortedMap;

/**
 * <p>The type of readable meshes that can be deformed by the CPU.</p>
 *
 * <p>A CPU-deformable mesh provides access to several buffers via a cursor
 * interface. Specifically, the mesh uses a <i>source</i> buffer to contain data
 * that will read-only data that will be used as the source for deformations.
 * Deformed source data will be written to a <i>target</i> buffer on each
 * deformation, the entire contents being replaced each time. Any additional
 * read-only mesh data that will not otherwise be processed is stored in an
 * <i>auxiliary</i> buffer. The read-only triangle indices for the mesh are
 * stored in a <i>triangle</i> buffer.</p>
 */

public interface CaMeshDeformableCPUReadableType
{
  /**
   * <p>A map of cursors that can read from source attributes within the
   * mesh.</p>
   *
   * @return The available source cursors
   */

  SortedMap<String, CaMeshDeformableAttributeCursorReadableType> meshSourceCursors();

  /**
   * @return The number of vertices in the mesh
   */

  long vertexCount();

  /**
   * Access the joint indices for the given vertex.
   *
   * @param vertex The vertex index
   * @param out    The output vector that will contain joint indices
   */

  void jointIndicesForVertex(
    long vertex,
    VectorWritable4LType out);

  /**
   * Access the joint weights for the given vertex.
   *
   * @param vertex The vertex index
   * @param out    The output vector that will contain joint weights
   */

  void jointWeightsForVertex(
    long vertex,
    VectorWritable4DType out);
}
