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
import com.io7m.jcalcium.core.compiled.CaSkeletonType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedBoneType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorWeightedAction;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.QuaternionReadable4DType;
import com.io7m.jtensors.VectorM3D;
import com.io7m.jtensors.VectorReadable3DType;
import com.io7m.jtensors.parameterized.PVectorM3D;
import com.io7m.jtensors.parameterized.PVectorReadable3DType;
import com.io7m.junreachable.UnimplementedCodeException;
import javaslang.collection.List;

import static com.io7m.jfunctional.Unit.unit;

/**
 * The default implementation of {@link CaEvaluatorType}.
 */

public final class CaEvaluator implements CaEvaluatorType
{
  private final CaSkeletonType skeleton;
  private final JOTreeNodeType<BoneState> state;
  private final JOTreeNodeReadableType<CaEvaluatedBoneType> state_view;

  private CaEvaluator(
    final CaSkeletonType in_skeleton)
  {
    this.skeleton = NullCheck.notNull(in_skeleton, "Skeleton");
    this.state = in_skeleton.bones().mapBreadthFirst(
      unit(), (input, depth, node) -> {
        final CaBone c_bone = node.value();
        return new BoneState(c_bone.name(), c_bone.id());
      });

    @SuppressWarnings("unchecked")
    final JOTreeNodeReadableType<CaEvaluatedBoneType> view_typed =
      (JOTreeNodeReadableType<CaEvaluatedBoneType>) (Object) this.state;
    this.state_view = view_typed;
  }

  /**
   * Construct a new evaluator for the skeleton.
   *
   * @param skeleton The skeleton
   *
   * @return A new evaluator
   */

  public static CaEvaluatorType create(
    final CaSkeletonType skeleton)
  {
    return new CaEvaluator(skeleton);
  }

  @Override
  public JOTreeNodeReadableType<CaEvaluatedBoneType> evaluatedBones()
  {
    return this.state_view;
  }

  @Override
  public void evaluateTransitionTo(
    final List<CaEvaluatorWeightedAction> actions,
    final double time)
  {
    NullCheck.notNull(actions, "actions");

    // TODO: Generated method stub
    throw new UnimplementedCodeException();
  }

  @Override
  public void evaluate(
    final double time)
  {
    // TODO: Generated method stub
    throw new UnimplementedCodeException();
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
