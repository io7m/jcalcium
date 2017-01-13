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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectDeformedType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointReadableDType;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.VectorI4D;
import com.io7m.jtensors.VectorI4L;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4DType;
import com.io7m.jtensors.parameterized.PMatrixWritable4x4DType;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;

/**
 * Methods to construct double-precision deformation matrices.
 */

public interface CaMeshDeformationMatricesDType
{
  /**
   * Construct a weighted deformation matrix. The matrix is a weighted blend of
   * the deformation matrices for the joints given in {@code joint_indices}. The
   * weights are taken from the corresponding components of {@code
   * joint_weights}.
   *
   * @param joints        The set of available joints
   * @param joint_indices The four joints that affect the given position the
   *                      most
   * @param joint_weights The corresponding weights for each joint
   * @param output        The output matrix
   */

  default void weightedDeformationMatrixD(
    final Int2ReferenceSortedMap<CaEvaluatedJointReadableDType> joints,
    final VectorI4L joint_indices,
    final VectorI4D joint_weights,
    final PMatrixWritable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> output)
  {
    NullCheck.notNull(joints, "Joints");
    NullCheck.notNull(joint_indices, "Joint indices");
    NullCheck.notNull(joint_weights, "Joint weights");
    NullCheck.notNull(output, "Output");

    final int joint_index_0 = Math.toIntExact(joint_indices.getXL());
    final int joint_index_1 = Math.toIntExact(joint_indices.getYL());
    final int joint_index_2 = Math.toIntExact(joint_indices.getZL());
    final int joint_index_3 = Math.toIntExact(joint_indices.getWL());

    final double joint_weight_0 = joint_weights.getXD();
    final double joint_weight_1 = joint_weights.getYD();
    final double joint_weight_2 = joint_weights.getZD();
    final double joint_weight_3 = joint_weights.getWD();

    Preconditions.checkPreconditionI(
      joint_index_0,
      joints.containsKey(joint_index_0),
      joint -> "Joint " + joint + " must exist");
    Preconditions.checkPreconditionI(
      joint_index_1,
      joints.containsKey(joint_index_1),
      joint -> "Joint " + joint + " must exist");
    Preconditions.checkPreconditionI(
      joint_index_2,
      joints.containsKey(joint_index_2),
      joint -> "Joint " + joint + " must exist");
    Preconditions.checkPreconditionI(
      joint_index_3,
      joints.containsKey(joint_index_3),
      joint -> "Joint " + joint + " must exist");

    final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_0 =
      joints.get(joint_index_0).transformDeform4x4D();
    final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_1 =
      joints.get(joint_index_1).transformDeform4x4D();
    final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_2 =
      joints.get(joint_index_2).transformDeform4x4D();
    final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_3 =
      joints.get(joint_index_3).transformDeform4x4D();

    this.weightedDeformationMatrixExplicitD(
      deform_joint_0,
      joint_weight_0,
      deform_joint_1,
      joint_weight_1,
      deform_joint_2,
      joint_weight_2,
      deform_joint_3,
      joint_weight_3,
      output);
  }

  /**
   * Construct a weighted deformation matrix. The matrix is a weighted blend of
   * the given joint deformation matrices. For best results, {@code
   * joint_weight_0 + joint_weight_1 + joint_weight_2 + joint_weight_3 == 1.0}.
   *
   * @param deform_joint_0 The first joint deformation matrix
   * @param joint_weight_0 The weight for {@code deform_joint_0}
   * @param deform_joint_1 The first joint deformation matrix
   * @param joint_weight_1 The weight for {@code deform_joint_1}
   * @param deform_joint_2 The first joint deformation matrix
   * @param joint_weight_2 The weight for {@code deform_joint_2}
   * @param deform_joint_3 The first joint deformation matrix
   * @param joint_weight_3 The weight for {@code deform_joint_3}
   * @param output         The output matrix
   */

  void weightedDeformationMatrixExplicitD(
    PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_0,
    double joint_weight_0,
    PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_1,
    double joint_weight_1,
    PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_2,
    double joint_weight_2,
    PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_3,
    double joint_weight_3,
    PMatrixWritable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> output);
}
