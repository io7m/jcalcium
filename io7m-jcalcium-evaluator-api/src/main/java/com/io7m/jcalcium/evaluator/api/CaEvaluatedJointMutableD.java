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

package com.io7m.jcalcium.evaluator.api;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPoseDType;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectDeformedType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jtensors.Matrix4x4DType;
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.QuaternionReadable4DType;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.VectorM3D;
import com.io7m.jtensors.VectorReadable3DType;
import com.io7m.jtensors.parameterized.PMatrix4x4DType;
import com.io7m.jtensors.parameterized.PMatrixHeapArrayM4x4D;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4DType;
import com.io7m.jtensors.parameterized.PVectorI3D;
import com.io7m.jtensors.parameterized.PVectorM3D;
import com.io7m.jtensors.parameterized.PVectorReadable3DType;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;
import javaslang.collection.SortedMap;

import java.util.Optional;

/**
 * A mutable evaluated joint with double-precision components.
 */

public final class CaEvaluatedJointMutableD implements
  CaEvaluatedJointMutableDType
{
  private final CaJointName joint_name;
  private final VectorM3D scale;
  private final PVectorM3D<CaSpaceJointType> translation;
  private final QuaternionM4D orientation;
  private final PMatrix4x4DType<CaSpaceJointType, CaSpaceObjectType> transform_joint_object;
  private final PMatrix4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> transform_deform;
  private final Optional<CaEvaluatedJointMutableDType> joint_parent;
  private final CaSkeletonRestPoseDType rest_pose;
  private final CaEvaluationContextType context;
  private final int joint_id;
  private boolean transform_current;

  private CaEvaluatedJointMutableD(
    final CaJointName in_joint_name,
    final VectorM3D in_scale,
    final PVectorM3D<CaSpaceJointType> in_translation,
    final QuaternionM4D in_orientation,
    final PMatrix4x4DType<CaSpaceJointType, CaSpaceObjectType> in_transform_joint_object,
    final PMatrix4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> in_transform_deform,
    final Optional<CaEvaluatedJointMutableDType> in_joint_parent,
    final CaSkeletonRestPoseDType in_rest_pose,
    final CaEvaluationContextType in_context,
    final int in_joint_id)
  {
    this.joint_name =
      NullCheck.notNull(in_joint_name, "Joint name");
    this.scale =
      NullCheck.notNull(in_scale, "Scale");
    this.translation =
      NullCheck.notNull(in_translation, "Translation");
    this.orientation =
      NullCheck.notNull(in_orientation, "Orientation");
    this.transform_joint_object =
      NullCheck.notNull(in_transform_joint_object, "Transform joint object");
    this.transform_deform =
      NullCheck.notNull(in_transform_deform, "Transform deform");
    this.joint_parent =
      NullCheck.notNull(in_joint_parent, "Joint parent");
    this.rest_pose =
      NullCheck.notNull(in_rest_pose, "Rest pose");
    this.context =
      NullCheck.notNull(in_context, "Context");
    this.joint_id = in_joint_id;
  }

  /**
   * Create a new joint.
   *
   * @param in_context  An evaluation context
   * @param in_skeleton The owning skeleton
   * @param in_joint_id The joint ID
   *
   * @return A new joint
   */

  public static CaEvaluatedJointMutableDType create(
    final CaEvaluationContextType in_context,
    final CaEvaluatedSkeletonMutableDType in_skeleton,
    final int in_joint_id)
  {
    final CaSkeletonRestPoseDType rest_pose =
      in_skeleton.restPose();
    final SortedMap<Integer, JOTreeNodeReadableType<CaJoint>> nodes_by_id =
      rest_pose.skeleton().jointsByID();

    Preconditions.checkPreconditionI(
      in_joint_id,
      nodes_by_id.containsKey(Integer.valueOf(in_joint_id)),
      jn -> "Joint " + jn + " must exist in the skeleton");

    final JOTreeNodeReadableType<CaJoint> rest_node =
      nodes_by_id.get(Integer.valueOf(in_joint_id)).get();

    final CaJoint rest_joint =
      rest_node.value();

    final Optional<CaEvaluatedJointMutableDType> in_joint_parent =
      rest_node.parentReadable().flatMap(parent -> {
        final CaJoint parent_joint = parent.value();
        final Int2ReferenceSortedMap<CaEvaluatedJointMutableDType> joints =
          in_skeleton.jointsMutableByID();
        return Optional.ofNullable(joints.get(parent_joint.id()));
      });

    return new CaEvaluatedJointMutableD(
      rest_joint.name(),
      new VectorM3D(rest_joint.scale()),
      new PVectorM3D<>(rest_joint.translation()),
      new QuaternionM4D(rest_joint.orientation()),
      PMatrixHeapArrayM4x4D.newMatrix(),
      PMatrixHeapArrayM4x4D.newMatrix(),
      in_joint_parent,
      rest_pose,
      in_context,
      in_joint_id);
  }

  @Override
  public CaJointName name()
  {
    return this.joint_name;
  }

  @Override
  public int id()
  {
    return this.joint_id;
  }

  @Override
  public PMatrixReadable4x4DType<CaSpaceJointType, CaSpaceObjectType>
  transformJointObject4x4D()
  {
    if (!this.transform_current) {
      this.makeTransform();
    }

    return this.transform_joint_object;
  }

  private void makeTransform()
  {
    try (final CaEvaluationContextMatricesType m = this.context.newMatrices()) {
      final Matrix4x4DType m_accumulated = m.accumulated4x4D();
      final Matrix4x4DType m_translation = m.translation4x4D();
      final Matrix4x4DType m_orientation = m.orientation4x4D();
      final Matrix4x4DType m_scale = m.scale4x4D();

      MatrixM4x4D.makeTranslation3D(
        this.translation, m_translation);

      QuaternionM4D.makeRotationMatrix4x4(
        this.orientation, m_orientation);

      MatrixM4x4D.setIdentity(m_scale);
      m_scale.setR0C0D(this.scale.getXD());
      m_scale.setR1C1D(this.scale.getYD());
      m_scale.setR2C2D(this.scale.getZD());

      MatrixM4x4D.multiply(
        m_translation, m_orientation, m_accumulated);
      MatrixM4x4D.multiply(
        m_accumulated, m_scale, m_accumulated);

      if (this.joint_parent.isPresent()) {
        MatrixM4x4D.multiply(
          this.joint_parent.get().transformJointObject4x4D(),
          m_accumulated,
          this.transform_joint_object);
      } else {
        MatrixM4x4D.copy(m_accumulated, this.transform_joint_object);
      }

      MatrixM4x4D.multiply(
        this.transform_joint_object,
        this.rest_pose.transformInverseRest4x4D(this.joint_id),
        this.transform_deform);

      this.transform_current = true;
    }
  }

  @Override
  public PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType>
  transformDeform4x4D()
  {
    if (!this.transform_current) {
      this.makeTransform();
    }

    return this.transform_deform;
  }

  @Override
  public PVectorReadable3DType<CaSpaceJointType> translation3D()
  {
    return this.translation;
  }

  @Override
  public QuaternionReadable4DType orientation4D()
  {
    return this.orientation;
  }

  @Override
  public VectorReadable3DType scale3D()
  {
    return this.scale;
  }

  @Override
  public Optional<CaEvaluatedJointReadableDType> parent()
  {
    return this.joint_parent.map(x -> x);
  }

  @Override
  public void setTranslation3D(
    final PVectorI3D<CaSpaceJointType> p)
  {
    this.translation.set3D(p.getXD(), p.getYD(), p.getZD());
    this.transform_current = false;
  }

  @Override
  public void setOrientation(
    final QuaternionI4D q)
  {
    this.orientation.set4D(q.getXD(), q.getYD(), q.getZD(), q.getWD());
    this.transform_current = false;
  }

  @Override
  public void setScale(
    final VectorI3D s)
  {
    this.scale.set3D(s.getXD(), s.getYD(), s.getZD());
    this.transform_current = false;
  }
}
