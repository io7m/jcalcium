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

import com.io7m.jcalcium.core.spaces.CaSpaceObjectDeformedType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointReadableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedSkeletonReadableDType;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.VectorM4D;
import com.io7m.jtensors.VectorM4L;
import com.io7m.jtensors.parameterized.PMatrix4x4DType;
import com.io7m.jtensors.parameterized.PMatrixHeapArrayM4x4D;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;
import javaslang.collection.Map;
import javaslang.collection.SortedMap;

/**
 * Functions for deforming meshes on the CPU.
 */

public final class CaMeshDeformerCPU implements CaMeshDeformerCPUType
{
  private final CaMeshDeformationMatricesDType matrices_d;
  private final PMatrix4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> matrix_deform_d;
  private final MatrixM4x4D.ContextMM4D matrix_context_4x4d;
  private final VectorM4D temporary_4d;

  private CaMeshDeformerCPU(
    final CaMeshDeformationMatricesDType in_matrices_d)
  {
    this.matrices_d = NullCheck.notNull(in_matrices_d, "Matrices D");
    this.matrix_deform_d = PMatrixHeapArrayM4x4D.newMatrix();
    this.matrix_context_4x4d = new MatrixM4x4D.ContextMM4D();
    this.temporary_4d = new VectorM4D();
  }

  /**
   * Create a mesh deformer.
   *
   * @param in_matrices_d A deformation matrix provider
   *
   * @return A new mesh deformer
   */

  public static CaMeshDeformerCPUType create(
    final CaMeshDeformationMatricesDType in_matrices_d)
  {
    return new CaMeshDeformerCPU(in_matrices_d);
  }

  @Override
  public void deformD(
    final CaEvaluatedSkeletonReadableDType skeleton,
    final CaMeshDeformableCPUType mesh)
  {
    NullCheck.notNull(skeleton, "Skeleton");
    NullCheck.notNull(mesh, "Mesh");

    final VectorM4L indices = new VectorM4L();
    final VectorM4D weights = new VectorM4D();
    final Int2ReferenceSortedMap<CaEvaluatedJointReadableDType> joints =
      skeleton.jointsByID();

    final SortedMap<String, CaMeshDeformableAttributeCursorType> target_cursors =
      mesh.meshTargetCursors();
    final SortedMap<String, CaMeshDeformableAttributeCursorReadableType> source_cursors =
      mesh.meshSourceCursors();

    for (long vertex = 0L;
         Long.compareUnsigned(vertex, mesh.vertexCount()) < 0;
         ++vertex) {

      mesh.jointIndicesForVertex(vertex, indices);
      mesh.jointWeightsForVertex(vertex, weights);

      final CaEvaluatedJointReadableDType joint_0 =
        joints.get(Math.toIntExact(indices.getXL()));
      final CaEvaluatedJointReadableDType joint_1 =
        joints.get(Math.toIntExact(indices.getYL()));
      final CaEvaluatedJointReadableDType joint_2 =
        joints.get(Math.toIntExact(indices.getZL()));
      final CaEvaluatedJointReadableDType joint_3 =
        joints.get(Math.toIntExact(indices.getWL()));

      final double weight_0 = weights.getXD();
      final double weight_1 = weights.getYD();
      final double weight_2 = weights.getZD();
      final double weight_3 = weights.getWD();

      this.matrices_d.weightedDeformationMatrixExplicitD(
        joint_0.transformDeform4x4D(),
        weight_0,
        joint_1.transformDeform4x4D(),
        weight_1,
        joint_2.transformDeform4x4D(),
        weight_2,
        joint_3.transformDeform4x4D(),
        weight_3,
        this.matrix_deform_d);

      this.writeDeformedData(target_cursors, source_cursors, vertex);
    }
  }

  private void writeDeformedData(
    final Map<String, CaMeshDeformableAttributeCursorType> target_cursors,
    final Map<String, CaMeshDeformableAttributeCursorReadableType> source_cursors,
    final long vertex)
  {
    for (final String name : source_cursors.keySet()) {
      final CaMeshDeformableAttributeCursorReadableType source =
        source_cursors.get(name).get();
      final CaMeshDeformableAttributeCursorType target =
        target_cursors.get(name).get();

      source.setVertex(vertex);
      target.setVertex(vertex);

      switch (source.kind()) {
        case CURSOR_FLOAT_3: {
          final CaMeshDeformableAttributeCursorReadable3Type source3 =
            (CaMeshDeformableAttributeCursorReadable3Type) source;
          final CaMeshDeformableAttributeCursor3Type target3 =
            (CaMeshDeformableAttributeCursor3Type) target;

          source3.get3D(this.temporary_4d);
          switch (source3.semantic()) {
            case POSITION: {
              this.temporary_4d.setWD(1.0);
              break;
            }
            case DIRECTION: {
              this.temporary_4d.setWD(0.0);
              break;
            }
          }

          MatrixM4x4D.multiplyVector4D(
            this.matrix_context_4x4d,
            this.matrix_deform_d,
            this.temporary_4d,
            this.temporary_4d);

          target3.set3D(
            this.temporary_4d.getXD(),
            this.temporary_4d.getYD(),
            this.temporary_4d.getZD());
          break;
        }

        case CURSOR_FLOAT_4: {
          final CaMeshDeformableAttributeCursorReadable4Type source4 =
            (CaMeshDeformableAttributeCursorReadable4Type) source;
          final CaMeshDeformableAttributeCursor4Type target4 =
            (CaMeshDeformableAttributeCursor4Type) target;

          source4.get4D(this.temporary_4d);
          final double saved_w = this.temporary_4d.getWD();
          switch (source4.semantic()) {
            case POSITION: {
              this.temporary_4d.setWD(1.0);
              break;
            }
            case DIRECTION: {
              this.temporary_4d.setWD(0.0);
              break;
            }
          }

          MatrixM4x4D.multiplyVector4D(
            this.matrix_context_4x4d,
            this.matrix_deform_d,
            this.temporary_4d,
            this.temporary_4d);

          target4.set4D(
            this.temporary_4d.getXD(),
            this.temporary_4d.getYD(),
            this.temporary_4d.getZD(),
            saved_w);
          break;
        }
      }
    }
  }
}
