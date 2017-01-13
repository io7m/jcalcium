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
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.MatrixM4x4F;
import com.io7m.jtensors.parameterized.PMatrix4x4DType;
import com.io7m.jtensors.parameterized.PMatrix4x4FType;
import com.io7m.jtensors.parameterized.PMatrixHeapArrayM4x4D;
import com.io7m.jtensors.parameterized.PMatrixHeapArrayM4x4F;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4DType;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4FType;
import com.io7m.jtensors.parameterized.PMatrixWritable4x4DType;
import com.io7m.jtensors.parameterized.PMatrixWritable4x4FType;

/**
 * Functions to construct deformation matrices.
 */

public final class CaMeshDeformationMatrices implements
  CaMeshDeformationMatricesType
{
  private final PMatrix4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> m4d;
  private final PMatrix4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> m4d_temp;
  private final PMatrix4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> m4f;
  private final PMatrix4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> m4f_temp;

  private CaMeshDeformationMatrices()
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

  public static CaMeshDeformationMatricesType create()
  {
    return new CaMeshDeformationMatrices();
  }

  @Override
  public void weightedDeformationMatrixExplicitD(
    final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_0,
    final double joint_weight_0,
    final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_1,
    final double joint_weight_1,
    final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_2,
    final double joint_weight_2,
    final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_3,
    final double joint_weight_3,
    final PMatrixWritable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> output)
  {
    MatrixM4x4D.scale(deform_joint_0, joint_weight_0, this.m4d);
    MatrixM4x4D.scale(deform_joint_1, joint_weight_1, this.m4d_temp);
    MatrixM4x4D.addInPlace(this.m4d, this.m4d_temp);
    MatrixM4x4D.scale(deform_joint_2, joint_weight_2, this.m4d_temp);
    MatrixM4x4D.addInPlace(this.m4d, this.m4d_temp);
    MatrixM4x4D.scale(deform_joint_3, joint_weight_3, this.m4d_temp);
    MatrixM4x4D.add(this.m4d, this.m4d_temp, output);
  }

  @Override
  public void weightedDeformationMatrixExplicitF(
    final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_0,
    final double joint_weight_0,
    final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_1,
    final double joint_weight_1,
    final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_2,
    final double joint_weight_2,
    final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> deform_joint_3,
    final double joint_weight_3,
    final PMatrixWritable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> output)
  {
    MatrixM4x4F.scale(deform_joint_0, joint_weight_0, this.m4f);
    MatrixM4x4F.scale(deform_joint_1, joint_weight_1, this.m4f_temp);
    MatrixM4x4F.addInPlace(this.m4f, this.m4f_temp);
    MatrixM4x4F.scale(deform_joint_2, joint_weight_2, this.m4f_temp);
    MatrixM4x4F.addInPlace(this.m4f, this.m4f_temp);
    MatrixM4x4F.scale(deform_joint_3, joint_weight_3, this.m4f_temp);
    MatrixM4x4F.add(this.m4f, this.m4f_temp, output);
  }
}
