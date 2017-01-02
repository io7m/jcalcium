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

import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.compiled.CaBone;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneAbsoluteType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jcalcium.evaluator.api.CaActionEvaluatorCurvesDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedBoneDType;
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
  private final JOTreeNodeType<BoneStateD> bone_states;
  private final JOTreeNodeReadableType<CaEvaluatedBoneDType> bone_states_view;
  private final Matrix4x4DType m_translation;
  private final Matrix4x4DType m_orientation;
  private final Matrix4x4DType m_scale;
  private final Matrix4x4DType m_accumulated;
  private final Int2ReferenceSortedMap<BoneStateD> bone_states_by_id;
  private final Int2ReferenceSortedMap<CaEvaluatedBoneDType> bone_states_by_id_view;
  private ActionKind kind;
  private CaActionEvaluatorCurvesDType eval_curves;
  private long frame_start;
  private long frame_current;
  private double time_scale;

  private CaEvaluatorSingleD(
    final CaSkeleton in_skeleton,
    final CaActionType in_action,
    final int global_fps)
  {
    NullCheck.notNull(in_skeleton, "Skeleton");
    NullCheck.notNull(in_action, "Action");

    this.bone_states_by_id =
      new Int2ReferenceRBTreeMap<>();
    this.bone_states_by_id_view =
      Int2ReferenceSortedMaps.unmodifiable(castMap(this.bone_states_by_id));

    this.bone_states = in_skeleton.bones().mapBreadthFirst(
      unit(), (input, depth, node) -> {
        final CaBone c_bone = node.value();

        final Optional<JOTreeNodeReadableType<CaBone>> parent_opt =
          node.parentReadable();

        final OptionalInt c_bone_parent;
        if (parent_opt.isPresent()) {
          c_bone_parent = OptionalInt.of(parent_opt.get().value().id());
        } else {
          c_bone_parent = OptionalInt.empty();
        }

        final BoneStateD c_bone_state =
          new BoneStateD(c_bone.name(), c_bone.id(), c_bone_parent);
        this.bone_states_by_id.put(node.value().id(), c_bone_state);
        return c_bone_state;
      });

    @SuppressWarnings("unchecked")
    final JOTreeNodeReadableType<CaEvaluatedBoneDType> view_typed =
      (JOTreeNodeReadableType<CaEvaluatedBoneDType>) (Object) this.bone_states;
    this.bone_states_view = view_typed;

    in_action.matchAction(this, (t, curves) -> {
      t.kind = ActionKind.ACTION_CURVES;
      t.eval_curves =
        CaActionEvaluatorCurves.createD(in_skeleton, curves, global_fps);
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
   * @param in_skeleton The skeleton
   * @param in_action   The action
   * @param global_fps  The global FPS rate
   *
   * @return An evaluator
   */

  public static CaEvaluatorSingleDType create(
    final CaSkeleton in_skeleton,
    final CaActionType in_action,
    final int global_fps)
  {
    return new CaEvaluatorSingleD(in_skeleton, in_action, global_fps);
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
  public JOTreeNodeReadableType<CaEvaluatedBoneDType> evaluatedBonesD()
  {
    return this.bone_states_view;
  }

  @Override
  public Int2ReferenceSortedMap<CaEvaluatedBoneDType> evaluatedBonesDByID()
  {
    return this.bone_states_by_id_view;
  }

  private Unit evaluateCurves(
    final long in_frame_start,
    final long in_frame_current,
    final double in_time_scale)
  {
    this.frame_start = in_frame_start;
    this.frame_current = in_frame_current;
    this.time_scale = in_time_scale;
    this.bone_states.forEachBreadthFirst(this, (t, depth, node) -> {
      final BoneStateD bone = node.value();

      t.eval_curves.evaluateOrientation4DForGlobalFrame(
        bone.bone_id,
        t.frame_start,
        t.frame_current,
        t.time_scale,
        bone.orientation);

      t.eval_curves.evaluateTranslation3DForGlobalFrame(
        bone.bone_id,
        t.frame_start,
        t.frame_current,
        t.time_scale,
        bone.translation);

      t.eval_curves.evaluateScale3DForGlobalFrame(
        bone.bone_id,
        t.frame_start,
        t.frame_current,
        t.time_scale,
        bone.scale);

      final BoneStateD bone_parent = node.parentReadable().map(
        JOTreeNodeReadableType::value).orElse(null);

      t.makeTransform(bone_parent, bone);
    });
    return unit();
  }

  private void makeTransform(
    final @Nullable BoneStateD bone_parent,
    final BoneStateD bone)
  {
    MatrixM4x4D.makeTranslation3D(
      bone.translation, this.m_translation);

    QuaternionM4D.makeRotationMatrix4x4(
      bone.orientation, this.m_orientation);

    MatrixM4x4D.setIdentity(this.m_scale);
    this.m_scale.setR0C0D(bone.scale.getXD());
    this.m_scale.setR1C1D(bone.scale.getYD());
    this.m_scale.setR2C2D(bone.scale.getZD());

    MatrixM4x4D.multiply(
      this.m_translation, this.m_orientation, this.m_accumulated);
    MatrixM4x4D.multiply(
      this.m_accumulated, this.m_scale, this.m_accumulated);

    if (bone_parent != null) {
      MatrixM4x4D.multiply(
        bone_parent.absolute_transform,
        this.m_accumulated,
        bone.absolute_transform);
    } else {
      MatrixM4x4D.copy(this.m_accumulated, bone.absolute_transform);
    }
  }

  private enum ActionKind
  {
    ACTION_CURVES
  }

  private static final class BoneStateD implements CaEvaluatedBoneDType
  {
    private final CaBoneName bone_name;
    private final int bone_id;
    private final VectorM3D scale;
    private final PVectorM3D<CaSpaceBoneParentRelativeType> translation;
    private final QuaternionM4D orientation;
    private final PMatrix4x4DType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> absolute_transform;
    private final OptionalInt bone_parent;

    BoneStateD(
      final CaBoneName in_bone_name,
      final int in_bone_id,
      final OptionalInt in_bone_parent)
    {
      this.bone_name = NullCheck.notNull(in_bone_name, "Bone name");
      this.bone_parent = NullCheck.notNull(in_bone_parent, "Parent");
      this.bone_id = in_bone_id;
      this.translation = new PVectorM3D<>();
      this.orientation = new QuaternionM4D();
      this.scale = new VectorM3D();
      this.absolute_transform = PMatrixHeapArrayM4x4D.newMatrix();
    }

    @Override
    public CaBoneName name()
    {
      return this.bone_name;
    }

    @Override
    public int id()
    {
      return this.bone_id;
    }

    @Override
    public OptionalInt parent()
    {
      return this.bone_parent;
    }

    @Override
    public PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> transformAbsolute4x4D()
    {
      return this.absolute_transform;
    }

    @Override
    public PVectorReadable3DType<CaSpaceBoneParentRelativeType> translation3D()
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
