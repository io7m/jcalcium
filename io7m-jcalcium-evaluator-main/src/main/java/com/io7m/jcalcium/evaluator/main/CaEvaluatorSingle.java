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
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.evaluator.api.CaActionEvaluatorCurvesType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedBoneType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorSingleType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.QuaternionReadable4DType;
import com.io7m.jtensors.VectorM3D;
import com.io7m.jtensors.VectorReadable3DType;
import com.io7m.jtensors.parameterized.PVectorM3D;
import com.io7m.jtensors.parameterized.PVectorReadable3DType;

import static com.io7m.jfunctional.Unit.unit;

/**
 * The default implementation of the {@link CaEvaluatorSingleType} type.
 */

public final class CaEvaluatorSingle implements CaEvaluatorSingleType
{
  private final CaSkeleton skeleton;
  private final CaActionType action;
  private final JOTreeNodeType<BoneState> bone_states;
  private final JOTreeNodeReadableType<CaEvaluatedBoneType> bone_states_view;
  private CaActionEvaluatorCurvesType eval_curves;

  private CaEvaluatorSingle(
    final CaSkeleton in_skeleton,
    final CaActionType in_action)
  {
    this.skeleton = NullCheck.notNull(in_skeleton, "Skeleton");
    this.action = NullCheck.notNull(in_action, "Action");

    this.bone_states = in_skeleton.bones().mapBreadthFirst(
      unit(), (input, depth, node) -> {
        final CaBone c_bone = node.value();
        return new BoneState(c_bone.name(), c_bone.id());
      });

    @SuppressWarnings("unchecked")
    final JOTreeNodeReadableType<CaEvaluatedBoneType> view_typed =
      (JOTreeNodeReadableType<CaEvaluatedBoneType>) (Object) this.bone_states;
    this.bone_states_view = view_typed;

    this.action.matchAction(this, (t, curves) -> {
      t.eval_curves = CaActionEvaluatorCurves.create(t.skeleton, curves);
      return unit();
    });
  }

  /**
   * Create a new single-action evaluator.
   *
   * @param in_skeleton The skeleton
   * @param in_action   The action
   *
   * @return An evaluator
   */

  public static CaEvaluatorSingleType create(
    final CaSkeleton in_skeleton,
    final CaActionType in_action)
  {
    return new CaEvaluatorSingle(in_skeleton, in_action);
  }

  @Override
  public CaSkeleton skeleton()
  {
    return this.skeleton;
  }

  @Override
  public CaActionType action()
  {
    return this.action;
  }

  @Override
  public JOTreeNodeReadableType<CaEvaluatedBoneType> evaluatedBones()
  {
    return this.bone_states_view;
  }

  @Override
  public void evaluate(
    final double time)
  {
    this.action.matchAction(this, (t, curves) -> t.evaluateCurves(time));
  }

  private Unit evaluateCurves(
    final double time)
  {
    this.bone_states.forEachBreadthFirst(this, (t, depth, node) -> {
      final BoneState bone = node.value();
      t.eval_curves.evaluateOrientation(bone.bone_id, time, bone.orientation);
      t.eval_curves.evaluateTranslation(bone.bone_id, time, bone.translation);
      t.eval_curves.evaluateScale(bone.bone_id, time, bone.scale);
    });
    return unit();
  }

  private static final class BoneState implements CaEvaluatedBoneType
  {
    private final CaBoneName bone_name;
    private final int bone_id;
    private final VectorM3D scale;
    private PVectorM3D<CaSpaceBoneParentRelativeType> translation;
    private QuaternionM4D orientation;

    BoneState(
      final CaBoneName in_bone_name,
      final int in_bone_id)
    {
      this.bone_name = NullCheck.notNull(in_bone_name, "Bone name");
      this.bone_id = in_bone_id;
      this.translation = new PVectorM3D<>();
      this.orientation = new QuaternionM4D();
      this.scale = new VectorM3D();
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
    public PVectorReadable3DType<CaSpaceBoneParentRelativeType> translation()
    {
      return this.translation;
    }

    @Override
    public QuaternionReadable4DType orientation()
    {
      return this.orientation;
    }

    @Override
    public VectorReadable3DType scale()
    {
      return this.scale;
    }
  }
}
