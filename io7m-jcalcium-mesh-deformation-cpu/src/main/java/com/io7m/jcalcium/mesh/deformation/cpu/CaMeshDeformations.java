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
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointReadableFType;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.MatrixM4x4F;
import com.io7m.jtensors.VectorI4D;
import com.io7m.jtensors.VectorI4L;
import com.io7m.jtensors.parameterized.PMatrix4x4DType;
import com.io7m.jtensors.parameterized.PMatrix4x4FType;
import com.io7m.jtensors.parameterized.PMatrixHeapArrayM4x4D;
import com.io7m.jtensors.parameterized.PMatrixHeapArrayM4x4F;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4DType;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4FType;
import com.io7m.jtensors.parameterized.PMatrixWritable4x4DType;
import com.io7m.jtensors.parameterized.PMatrixWritable4x4FType;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;

/**
 * Functions to construct deformation matrices.
 */

public final class CaMeshDeformations implements CaMeshDeformationsType
{
  private final PMatrix4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> m4d;
  private final PMatrix4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> m4d_temp;
  private final PMatrix4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> m4f;
  private final PMatrix4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> m4f_temp;

  private CaMeshDeformations()
  {
    this.m4d = PMatrixHeapArrayM4x4D.newMatrix();
    this.m4d_temp = PMatrixHeapArrayM4x4D.newMatrix();
    this.m4f = PMatrixHeapArrayM4x4F.newMatrix();
    this.m4f_temp = PMatrixHeapArrayM4x4F.newMatrix();
  }

  /**
   * Create a new matrix provider.
   *
   * @return A new matrix provider
   */

  public static CaMeshDeformationsType create()
  {
    return new CaMeshDeformations();
  }

  @Override
  public void weightedDeformationMatrixD(
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

    MatrixM4x4D.setZero(this.m4d);
    MatrixM4x4D.scale(deform_joint_0, joint_weight_0, this.m4d_temp);
    MatrixM4x4D.addInPlace(this.m4d, this.m4d_temp);
    MatrixM4x4D.scale(deform_joint_1, joint_weight_1, this.m4d_temp);
    MatrixM4x4D.addInPlace(this.m4d, this.m4d_temp);
    MatrixM4x4D.scale(deform_joint_2, joint_weight_2, this.m4d_temp);
    MatrixM4x4D.addInPlace(this.m4d, this.m4d_temp);
    MatrixM4x4D.scale(deform_joint_3, joint_weight_3, this.m4d_temp);
    MatrixM4x4D.add(this.m4d, this.m4d_temp, output);
  }

  @Override
  public void weightedDeformationMatrixF(
    final Int2ReferenceSortedMap<CaEvaluatedJointReadableFType> joints,
    final VectorI4L joint_indices,
    final VectorI4D joint_weights,
    final PMatrixWritable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> output)
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

    final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_0 =
      joints.get(joint_index_0).transformDeform4x4F();
    final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_1 =
      joints.get(joint_index_1).transformDeform4x4F();
    final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_2 =
      joints.get(joint_index_2).transformDeform4x4F();
    final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_3 =
      joints.get(joint_index_3).transformDeform4x4F();

    MatrixM4x4F.setZero(this.m4f);
    MatrixM4x4F.scale(deform_joint_0, joint_weight_0, this.m4f_temp);
    MatrixM4x4F.addInPlace(this.m4f, this.m4f_temp);
    MatrixM4x4F.scale(deform_joint_1, joint_weight_1, this.m4f_temp);
    MatrixM4x4F.addInPlace(this.m4f, this.m4f_temp);
    MatrixM4x4F.scale(deform_joint_2, joint_weight_2, this.m4f_temp);
    MatrixM4x4F.addInPlace(this.m4f, this.m4f_temp);
    MatrixM4x4F.scale(deform_joint_3, joint_weight_3, this.m4f_temp);
    MatrixM4x4F.add(this.m4f, this.m4f_temp, output);
  }
}
