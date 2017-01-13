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
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointReadableFType;
import com.io7m.jtensors.VectorI4D;
import com.io7m.jtensors.VectorI4L;
import com.io7m.jtensors.parameterized.PMatrixWritable4x4FType;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;

/**
 * Methods to construct single-precision deformation matrices.
 */

public interface CaMeshDeformationsFType
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

  void weightedDeformationMatrixF(
    final Int2ReferenceSortedMap<CaEvaluatedJointReadableFType> joints,
    final VectorI4L joint_indices,
    final VectorI4D joint_weights,
    final PMatrixWritable4x4FType<CaSpaceObjectType, CaSpaceObjectDeformedType> output);
}
