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
import com.io7m.smfj.bytebuffer.SMFByteBufferPackedTriangles;
import com.io7m.smfj.core.SMFHeader;
import com.io7m.smfj.core.SMFTriangles;
import javaslang.collection.SortedMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The event receiver interface for producing SMF-backed CPU-side deformable
 * meshes.
 *
 * @param <T> The actual type of CPU meshes produced
 */

public interface CaMeshDeformableCPUSMFEventsType<T extends CaMeshDeformableCPUType>
{
  /**
   * Called unconditionally when mesh creation begins.
   */

  default void onStart()
  {

  }

  /**
   * Called when a new buffer is required to hold {@code size_octets} octets of
   * joint data.
   *
   * @param size_octets The required size
   *
   * @return An allocated buffer
   */

  default ByteBuffer onAllocateJointBuffer(
    final long size_octets)
  {
    final int isize = Math.toIntExact(size_octets);
    return ByteBuffer.allocate(isize).order(ByteOrder.nativeOrder());
  }

  /**
   * Called when a new buffer is required to hold {@code size_octets} octets of
   * source data.
   *
   * @param size_octets The required size
   *
   * @return An allocated buffer
   */

  default ByteBuffer onAllocateSourceBuffer(
    final long size_octets)
  {
    final int isize = Math.toIntExact(size_octets);
    return ByteBuffer.allocate(isize).order(ByteOrder.nativeOrder());
  }

  /**
   * Called when a new buffer is required to hold {@code size_octets} octets of
   * target data.
   *
   * @param size_octets The required size
   *
   * @return An allocated buffer
   */

  default ByteBuffer onAllocateTargetBuffer(
    final long size_octets)
  {
    final int isize = Math.toIntExact(size_octets);
    return ByteBuffer.allocate(isize).order(ByteOrder.nativeOrder());
  }

  /**
   * Called when a new buffer is required to hold {@code size_octets} octets of
   * auxiliary data.
   *
   * @param size_octets The required size
   *
   * @return An allocated buffer
   */

  default ByteBuffer onAllocateAuxiliaryBuffer(
    final long size_octets)
  {
    final int isize = Math.toIntExact(size_octets);
    return ByteBuffer.allocate(isize).order(ByteOrder.nativeOrder());
  }

  /**
   * Called when a new buffer is required to hold {@code size_octets} octets of
   * triangle data.
   *
   * @param triangles   Information about the triangles that will be written
   * @param size_octets The required size
   *
   * @return An allocated buffer
   */

  default ByteBuffer onAllocateTriangleBuffer(
    final SMFTriangles triangles,
    final long size_octets)
  {
    final int isize = Math.toIntExact(size_octets);
    return ByteBuffer.allocate(isize).order(ByteOrder.nativeOrder());
  }

  /**
   * Called when all data has been parsed and processed.
   *
   * @param header              The SMF mesh header
   * @param joint_data          Packed joint data
   * @param cursor_joint_index  A cursor into the joint data for joint indices
   * @param cursor_joint_weight A cursor into the joint data for joint weights
   * @param source_data         Packed source data
   * @param source_cursors      The set of source cursors
   * @param target_data         Packed target data
   * @param target_cursors      The set of target cursors
   * @param aux_data            Packed auxiliary data
   * @param triangle_data       Packed triangle data
   *
   * @return A newly created mesh based on the given parameters
   */

  T onCreated(
    SMFHeader header,
    CaMeshDeformableCPUSMFPackedAttributeSet<CaSetJointType> joint_data,
    JPRACursor1DType<SMFByteBufferIntegerUnsigned4Type> cursor_joint_index,
    JPRACursor1DType<SMFByteBufferFloat4Type> cursor_joint_weight,
    CaMeshDeformableCPUSMFPackedAttributeSet<CaSetSourceType> source_data,
    SortedMap<String, CaMeshDeformableAttributeCursorReadableType> source_cursors,
    CaMeshDeformableCPUSMFPackedAttributeSet<CaSetTargetType> target_data,
    SortedMap<String, CaMeshDeformableAttributeCursorType> target_cursors,
    CaMeshDeformableCPUSMFPackedAttributeSet<CaSetAuxiliaryType> aux_data,
    SMFByteBufferPackedTriangles triangle_data);

  /**
   * Called unconditionally when mesh creation completes, even if a previous
   * step has failed.
   */

  default void onFinish()
  {

  }
}
