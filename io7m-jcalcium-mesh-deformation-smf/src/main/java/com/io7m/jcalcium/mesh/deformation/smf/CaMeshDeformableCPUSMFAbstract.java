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

package com.io7m.jcalcium.mesh.deformation.smf;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorReadableType;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorType;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableCPUType;
import com.io7m.jnull.NullCheck;
import com.io7m.jpra.runtime.java.JPRACursor1DType;
import com.io7m.jtensors.VectorWritable4DType;
import com.io7m.jtensors.VectorWritable4LType;
import com.io7m.smfj.bytebuffer.SMFByteBufferFloat4Type;
import com.io7m.smfj.bytebuffer.SMFByteBufferIntegerUnsigned4Type;
import javaslang.collection.SortedMap;

/**
 * An abstract SMF implementation of the {@link CaMeshDeformableCPUType} type.
 */

public abstract class CaMeshDeformableCPUSMFAbstract implements
  CaMeshDeformableCPUType
{
  private final SortedMap<String, CaMeshDeformableAttributeCursorType> target_cursors;
  private final SortedMap<String, CaMeshDeformableAttributeCursorReadableType> source_cursors;
  private final JPRACursor1DType<SMFByteBufferIntegerUnsigned4Type> joint_indices_cursor;
  private final JPRACursor1DType<SMFByteBufferFloat4Type> joint_weights_cursor;
  private final long vertex_count;

  protected CaMeshDeformableCPUSMFAbstract(
    final SortedMap<String, CaMeshDeformableAttributeCursorType> in_target_cursors,
    final SortedMap<String, CaMeshDeformableAttributeCursorReadableType> in_source_cursors,
    final JPRACursor1DType<SMFByteBufferIntegerUnsigned4Type> in_joint_indices_cursor,
    final JPRACursor1DType<SMFByteBufferFloat4Type> in_joint_weights_cursor,
    final long in_vertex_count)
  {
    this.target_cursors =
      NullCheck.notNull(in_target_cursors, "Target cursors");
    this.source_cursors =
      NullCheck.notNull(in_source_cursors, "Source cursors");
    this.joint_indices_cursor =
      NullCheck.notNull(in_joint_indices_cursor, "Indices cursor");
    this.joint_weights_cursor =
      NullCheck.notNull(in_joint_weights_cursor, "Weights cursor");
    this.vertex_count = in_vertex_count;
  }

  @Override
  public final SortedMap<String, CaMeshDeformableAttributeCursorType> meshTargetCursors()
  {
    return this.target_cursors;
  }

  @Override
  public final SortedMap<String, CaMeshDeformableAttributeCursorReadableType> meshSourceCursors()
  {
    return this.source_cursors;
  }

  @Override
  public final long vertexCount()
  {
    return this.vertex_count;
  }

  @Override
  public final void jointIndicesForVertex(
    final long vertex,
    final VectorWritable4LType out)
  {
    Preconditions.checkPreconditionL(
      vertex,
      Long.compareUnsigned(vertex, this.vertexCount()) < 0,
      x -> "Vertex must exist");

    final JPRACursor1DType<SMFByteBufferIntegerUnsigned4Type> c =
      this.joint_indices_cursor;
    c.setElementIndex(Math.toIntExact(vertex));
    c.getElementView().get4UL(out);
  }

  @Override
  public final void jointWeightsForVertex(
    final long vertex,
    final VectorWritable4DType out)
  {
    Preconditions.checkPreconditionL(
      vertex,
      Long.compareUnsigned(vertex, this.vertexCount()) < 0,
      x -> "Vertex must exist");

    final JPRACursor1DType<SMFByteBufferFloat4Type> c =
      this.joint_weights_cursor;
    c.setElementIndex(Math.toIntExact(vertex));
    c.getElementView().get4D(out);
  }
}
