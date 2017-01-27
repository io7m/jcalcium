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

import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorReadableType;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorType;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableCPUType;
import com.io7m.jpra.runtime.java.JPRACursor1DType;
import com.io7m.smfj.bytebuffer.SMFByteBufferFloat4Type;
import com.io7m.smfj.bytebuffer.SMFByteBufferIntegerUnsigned4Type;
import javaslang.collection.SortedMap;

/**
 * The default SMF implementation of the {@link CaMeshDeformableCPUType} type.
 */

public final class CaMeshDeformableCPUSMF
  extends CaMeshDeformableCPUSMFAbstract
{
  private CaMeshDeformableCPUSMF(
    final SortedMap<String, CaMeshDeformableAttributeCursorType> in_target_cursors,
    final SortedMap<String, CaMeshDeformableAttributeCursorReadableType> in_source_cursors,
    final JPRACursor1DType<SMFByteBufferIntegerUnsigned4Type> in_joint_indices_cursor,
    final JPRACursor1DType<SMFByteBufferFloat4Type> in_joint_weights_cursor,
    final long in_vertex_count)
  {
    super(
      in_target_cursors,
      in_source_cursors,
      in_joint_indices_cursor,
      in_joint_weights_cursor,
      in_vertex_count);
  }

  /**
   * Create a mesh.
   *
   * @param in_target_cursors       The set of target cursors
   * @param in_source_cursors       The set of source cursors
   * @param in_joint_indices_cursor The joint index cursor
   * @param in_joint_weights_cursor The joint weight cursor
   * @param in_vertex_count         The number of vertices
   *
   * @return A new mesh
   */

  public static CaMeshDeformableCPUType create(
    final SortedMap<String, CaMeshDeformableAttributeCursorType> in_target_cursors,
    final SortedMap<String, CaMeshDeformableAttributeCursorReadableType> in_source_cursors,
    final JPRACursor1DType<SMFByteBufferIntegerUnsigned4Type> in_joint_indices_cursor,
    final JPRACursor1DType<SMFByteBufferFloat4Type> in_joint_weights_cursor,
    final long in_vertex_count)
  {
    return new CaMeshDeformableCPUSMF(
      in_target_cursors,
      in_source_cursors,
      in_joint_indices_cursor,
      in_joint_weights_cursor,
      in_vertex_count);
  }
}
