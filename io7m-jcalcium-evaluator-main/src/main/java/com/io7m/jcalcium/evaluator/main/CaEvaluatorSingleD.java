/*
 * Copyright © 2016 <code@io7m.com> http://io7m.com
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

package com.io7m.jcalcium.evaluator.main;

import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPoseDType;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectDeformedType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jcalcium.evaluator.api.CaActionEvaluatorCurvesDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorSingleDType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jtensors.Matrix4x4DType;
import com.io7m.jtensors.MatrixHeapArrayM4x4D;
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.QuaternionReadable4DType;
import com.io7m.jtensors.VectorM3D;
import com.io7m.jtensors.VectorReadable3DType;
import com.io7m.jtensors.parameterized.PMatrix4x4DType;
import com.io7m.jtensors.parameterized.PMatrixHeapArrayM4x4D;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4DType;
import com.io7m.jtensors.parameterized.PVectorM3D;
import com.io7m.jtensors.parameterized.PVectorReadable3DType;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMaps;

import java.util.Optional;
import java.util.OptionalInt;

import static com.io7m.jfunctional.Unit.unit;

/**
 * The default implementation of the {@link CaEvaluatorSingleDType} type.
 */

public final class CaEvaluatorSingleD implements CaEvaluatorSingleDType
{
  private final JOTreeNodeType<JointStateD> joint_states;
  private final JOTreeNodeReadableType<CaEvaluatedJointDType> joint_states_view;
  private final Matrix4x4DType m_translation;
  private final Matrix4x4DType m_orientation;
  private final Matrix4x4DType m_scale;
  private final Matrix4x4DType m_accumulated;
  private final Int2ReferenceSortedMap<JointStateD> joint_states_by_id;
  private final Int2ReferenceSortedMap<CaEvaluatedJointDType> joint_states_by_id_view;
  private final CaSkeletonRestPoseDType rest_pose;
  private ActionKind kind;
  private CaActionEvaluatorCurvesDType eval_curves;
  private long frame_start;
  private long frame_current;
  private double time_scale;

  private CaEvaluatorSingleD(
    final CaSkeletonRestPoseDType in_rest_pose,
    final CaActionType in_action,
    final int global_fps)
  {
    this.rest_pose = NullCheck.notNull(in_rest_pose, "Rest pose");
    NullCheck.notNull(in_action, "Action");

    this.joint_states_by_id =
      new Int2ReferenceRBTreeMap<>();
    this.joint_states_by_id_view =
      Int2ReferenceSortedMaps.unmodifiable(castMap(this.joint_states_by_id));

    final CaSkeleton skeleton = in_rest_pose.skeleton();
    this.joint_states = skeleton.joints().mapBreadthFirst(
      unit(), (input, depth, node) -> {
        final CaJoint c_joint = node.value();

        final Optional<JOTreeNodeReadableType<CaJoint>> parent_opt =
          node.parentReadable();

        final OptionalInt c_joint_parent;
        if (parent_opt.isPresent()) {
          c_joint_parent = OptionalInt.of(parent_opt.get().value().id());
        } else {
          c_joint_parent = OptionalInt.empty();
        }

        final JointStateD c_joint_state =
          new JointStateD(c_joint.name(), c_joint.id(), c_joint_parent);
        this.joint_states_by_id.put(node.value().id(), c_joint_state);
        return c_joint_state;
      });

    @SuppressWarnings("unchecked")
    final JOTreeNodeReadableType<CaEvaluatedJointDType> view_typed =
      (JOTreeNodeReadableType<CaEvaluatedJointDType>) (Object) this.joint_states;
    this.joint_states_view = view_typed;

    in_action.matchAction(this, (t, curves) -> {
      t.kind = ActionKind.ACTION_CURVES;
      t.eval_curves =
        CaActionEvaluatorCurves.createD(skeleton, curves, global_fps);
      return unit();
    });

    this.m_translation = MatrixHeapArrayM4x4D.newMatrix();
    this.m_orientation = MatrixHeapArrayM4x4D.newMatrix();
    this.m_scale = MatrixHeapArrayM4x4D.newMatrix();
    this.m_accumulated = MatrixHeapArrayM4x4D.newMatrix();
  }

  @SuppressWarnings("unchecked")
  private static <A, B extends A> Int2ReferenceSortedMap<A> castMap(
    final Int2ReferenceSortedMap<B> m)
  {
    return (Int2ReferenceSortedMap<A>) m;
  }

  /**
   * Create a new single-action evaluator.
   *
   * @param in_rest_pose The skeleton rest pose transforms
   * @param in_action    The action
   * @param global_fps   The global FPS rate
   *
   * @return An evaluator
   */

  public static CaEvaluatorSingleDType create(
    final CaSkeletonRestPoseDType in_rest_pose,
    final CaActionType in_action,
    final int global_fps)
  {
    return new CaEvaluatorSingleD(in_rest_pose, in_action, global_fps);
  }

  @Override
  public void evaluateForGlobalFrame(
    final long in_frame_start,
    final long in_frame_current,
    final double in_time_scale)
  {
    switch (this.kind) {
      case ACTION_CURVES: {
        this.evaluateCurves(in_frame_start, in_frame_current, in_time_scale);
        return;
      }
    }

    throw new UnreachableCodeException();
  }

  @Override
  public JOTreeNodeReadableType<CaEvaluatedJointDType> evaluatedJointsD()
  {
    return this.joint_states_view;
  }

  @Override
  public Int2ReferenceSortedMap<CaEvaluatedJointDType> evaluatedJointsDByID()
  {
    return this.joint_states_by_id_view;
  }

  private Unit evaluateCurves(
    final long in_frame_start,
    final long in_frame_current,
    final double in_time_scale)
  {
    this.frame_start = in_frame_start;
    this.frame_current = in_frame_current;
    this.time_scale = in_time_scale;
    this.joint_states.forEachBreadthFirst(this, (t, depth, node) -> {
      final JointStateD joint = node.value();

      t.eval_curves.evaluateOrientation4DForGlobalFrame(
        joint.joint_id,
        t.frame_start,
        t.frame_current,
        t.time_scale,
        joint.orientation);

      t.eval_curves.evaluateTranslation3DForGlobalFrame(
        joint.joint_id,
        t.frame_start,
        t.frame_current,
        t.time_scale,
        joint.translation);

      t.eval_curves.evaluateScale3DForGlobalFrame(
        joint.joint_id,
        t.frame_start,
        t.frame_current,
        t.time_scale,
        joint.scale);

      final JointStateD joint_parent = node.parentReadable().map(
        JOTreeNodeReadableType::value).orElse(null);

      t.makeTransform(joint_parent, joint);
    });
    return unit();
  }

  private void makeTransform(
    final @Nullable JointStateD joint_parent,
    final JointStateD joint)
  {
    MatrixM4x4D.makeTranslation3D(
      joint.translation, this.m_translation);

    QuaternionM4D.makeRotationMatrix4x4(
      joint.orientation, this.m_orientation);

    MatrixM4x4D.setIdentity(this.m_scale);
    this.m_scale.setR0C0D(joint.scale.getXD());
    this.m_scale.setR1C1D(joint.scale.getYD());
    this.m_scale.setR2C2D(joint.scale.getZD());

    MatrixM4x4D.multiply(
      this.m_translation, this.m_orientation, this.m_accumulated);
    MatrixM4x4D.multiply(
      this.m_accumulated, this.m_scale, this.m_accumulated);

    if (joint_parent != null) {
      MatrixM4x4D.multiply(
        joint_parent.transform_joint_object,
        this.m_accumulated,
        joint.transform_joint_object);
    } else {
      MatrixM4x4D.copy(this.m_accumulated, joint.transform_joint_object);
    }

    MatrixM4x4D.multiply(
      joint.transform_joint_object,
      this.rest_pose.transformInverseRest4x4D(joint.joint_id),
      joint.transform_deform);
  }

  private enum ActionKind
  {
    ACTION_CURVES
  }

  private static final class JointStateD implements CaEvaluatedJointDType
  {
    private final CaJointName joint_name;
    private final int joint_id;
    private final VectorM3D scale;
    private final PVectorM3D<CaSpaceJointType> translation;
    private final QuaternionM4D orientation;
    private final PMatrix4x4DType<CaSpaceJointType, CaSpaceObjectType> transform_joint_object;
    private final PMatrix4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> transform_deform;
    private final OptionalInt joint_parent;

    JointStateD(
      final CaJointName in_joint_name,
      final int in_joint_id,
      final OptionalInt in_joint_parent)
    {
      this.joint_name = NullCheck.notNull(in_joint_name, "Bone name");
      this.joint_parent = NullCheck.notNull(in_joint_parent, "Parent");
      this.joint_id = in_joint_id;
      this.translation = new PVectorM3D<>();
      this.orientation = new QuaternionM4D();
      this.scale = new VectorM3D();
      this.transform_joint_object = PMatrixHeapArrayM4x4D.newMatrix();
      this.transform_deform = PMatrixHeapArrayM4x4D.newMatrix();
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
    public OptionalInt parent()
    {
      return this.joint_parent;
    }

    @Override
    public PMatrixReadable4x4DType<CaSpaceJointType, CaSpaceObjectType> transformJointObject4x4D()
    {
      return this.transform_joint_object;
    }

    @Override
    public PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceObjectDeformedType> transformDeform4x4D()
    {
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
  }
}
