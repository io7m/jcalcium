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

package com.io7m.jcalcium.core.compiled;

import com.io7m.jcalcium.core.spaces.CaSpaceBoneAbsoluteType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jtensors.Matrix4x4DType;
import com.io7m.jtensors.Matrix4x4FType;
import com.io7m.jtensors.MatrixHeapArrayM4x4D;
import com.io7m.jtensors.MatrixHeapArrayM4x4F;
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.MatrixM4x4F;
import com.io7m.jtensors.MatrixReadable4x4FType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.QuaternionI4F;
import com.io7m.jtensors.QuaternionM4D;
import com.io7m.jtensors.QuaternionM4F;
import com.io7m.jtensors.VectorI3F;
import com.io7m.jtensors.parameterized.PMatrix4x4DType;
import com.io7m.jtensors.parameterized.PMatrix4x4FType;
import com.io7m.jtensors.parameterized.PMatrixHeapArrayM4x4D;
import com.io7m.jtensors.parameterized.PMatrixHeapArrayM4x4F;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4DType;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4FType;
import com.io7m.jtensors.parameterized.PVectorI3D;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Functions for calculating rest pose transforms.
 */

public final class CaSkeletonTransforms
{
  private CaSkeletonTransforms()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Calculate a set of rest pose transforms for the given skeleton.
   *
   * @param skeleton The skeleton
   *
   * @return A set of transforms
   */

  public static CaSkeletonTransformsDType createD(
    final CaSkeleton skeleton)
  {
    NullCheck.notNull(skeleton, "Skeleton");
    return new BuildD(skeleton).build();
  }

  /**
   * Calculate a set of rest pose transforms for the given skeleton.
   *
   * @param skeleton The skeleton
   *
   * @return A set of transforms
   */

  public static CaSkeletonTransformsFType createF(
    final CaSkeleton skeleton)
  {
    NullCheck.notNull(skeleton, "Skeleton");
    return new BuildF(skeleton).build();
  }

  private static final class BuildD
  {
    private final Matrix4x4DType m_translation;
    private final Matrix4x4DType m_orientation;
    private final Matrix4x4DType m_scale;
    private final Matrix4x4DType m_accumulated;
    private final Int2ReferenceOpenHashMap<PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceBoneAbsoluteType>> transforms;
    private final CaSkeleton skeleton;

    BuildD(
      final CaSkeleton in_skeleton)
    {
      this.skeleton = NullCheck.notNull(in_skeleton, "Skeleton");
      this.m_translation = MatrixHeapArrayM4x4D.newMatrix();
      this.m_orientation = MatrixHeapArrayM4x4D.newMatrix();
      this.m_scale = MatrixHeapArrayM4x4D.newMatrix();
      this.m_accumulated = MatrixHeapArrayM4x4D.newMatrix();
      this.transforms = new Int2ReferenceOpenHashMap<>();
    }

    private void makeTransform(
      final @Nullable PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> bone_parent_transform,
      final CaBoneType bone)
    {
      MatrixM4x4D.makeTranslation3D(
        bone.translation(), this.m_translation);

      QuaternionM4D.makeRotationMatrix4x4(
        bone.orientation(), this.m_orientation);

      MatrixM4x4D.setIdentity(this.m_scale);
      this.m_scale.setR0C0D(bone.scale().getXD());
      this.m_scale.setR1C1D(bone.scale().getYD());
      this.m_scale.setR2C2D(bone.scale().getZD());

      MatrixM4x4D.multiply(
        this.m_translation, this.m_orientation, this.m_accumulated);
      MatrixM4x4D.multiply(
        this.m_accumulated, this.m_scale, this.m_accumulated);

      final PMatrix4x4DType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> transform = PMatrixHeapArrayM4x4D.newMatrix();
      if (bone_parent_transform != null) {
        MatrixM4x4D.multiply(
          bone_parent_transform,
          this.m_accumulated,
          transform);
      } else {
        MatrixM4x4D.copy(this.m_accumulated, transform);
      }

      this.transforms.put(bone.id(), transform);
    }

    CaSkeletonTransformsDType build()
    {
      this.skeleton.bones().forEachBreadthFirst(this, (t, depth, node) -> {
        final CaBone bone = node.value();

        final Optional<JOTreeNodeReadableType<CaBone>> parent_opt =
          node.parentReadable();
        final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> parent_transform;
        if (parent_opt.isPresent()) {
          final CaBone parent = parent_opt.get().value();
          parent_transform = t.transforms.get(parent.id());
        } else {
          parent_transform = null;
        }

        t.makeTransform(parent_transform, bone);
      });

      return new BuiltD(this.transforms);
    }
  }

  private static final class BuiltD implements CaSkeletonTransformsDType
  {
    private final Int2ReferenceOpenHashMap<PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceBoneAbsoluteType>> transforms;

    private BuiltD(
      final Int2ReferenceOpenHashMap<PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceBoneAbsoluteType>> in_transforms)
    {
      this.transforms = NullCheck.notNull(in_transforms, "transforms");
    }

    @Override
    public PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> transformAbsolute4x4D(
      final int bone_id)
      throws NoSuchElementException
    {
      if (this.transforms.containsKey(bone_id)) {
        return this.transforms.get(bone_id);
      }
      throw new NoSuchElementException("No such bone: " + bone_id);
    }
  }

  private static final class BuildF
  {
    private final Matrix4x4FType m_translation;
    private final Matrix4x4FType m_orientation;
    private final Matrix4x4FType m_scale;
    private final Matrix4x4FType m_accumulated;
    private final Int2ReferenceOpenHashMap<PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceBoneAbsoluteType>> transforms;
    private final CaSkeleton skeleton;

    BuildF(
      final CaSkeleton in_skeleton)
    {
      this.skeleton = NullCheck.notNull(in_skeleton, "Skeleton");
      this.m_translation = MatrixHeapArrayM4x4F.newMatrix();
      this.m_orientation = MatrixHeapArrayM4x4F.newMatrix();
      this.m_scale = MatrixHeapArrayM4x4F.newMatrix();
      this.m_accumulated = MatrixHeapArrayM4x4F.newMatrix();
      this.transforms = new Int2ReferenceOpenHashMap<>();
    }

    private void makeTransform(
      final @Nullable MatrixReadable4x4FType bone_parent_transform,
      final CaBoneType bone)
    {
      final PVectorI3D<CaSpaceBoneParentRelativeType> translation =
        bone.translation();

      final VectorI3F translation_f = new VectorI3F(
        (float) translation.getXD(),
        (float) translation.getYD(),
        (float) translation.getZD());

      MatrixM4x4F.makeTranslation3F(
        translation_f, this.m_translation);

      final QuaternionI4D orientation_d =
        bone.orientation();

      final QuaternionI4F orientation_f =
        new QuaternionI4F(
          (float) orientation_d.getXD(),
          (float) orientation_d.getYD(),
          (float) orientation_d.getZD(),
          (float) orientation_d.getWD());

      QuaternionM4F.makeRotationMatrix4x4(
        orientation_f, this.m_orientation);

      MatrixM4x4F.setIdentity(this.m_scale);
      this.m_scale.setR0C0F((float) bone.scale().getXD());
      this.m_scale.setR1C1F((float) bone.scale().getYD());
      this.m_scale.setR2C2F((float) bone.scale().getZD());

      MatrixM4x4F.multiply(
        this.m_translation, this.m_orientation, this.m_accumulated);
      MatrixM4x4F.multiply(
        this.m_accumulated, this.m_scale, this.m_accumulated);

      final PMatrix4x4FType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> transform =
        PMatrixHeapArrayM4x4F.newMatrix();

      if (bone_parent_transform != null) {
        MatrixM4x4F.multiply(
          bone_parent_transform,
          this.m_accumulated,
          transform);
      } else {
        MatrixM4x4F.copy(this.m_accumulated, transform);
      }

      this.transforms.put(bone.id(), transform);
    }

    CaSkeletonTransformsFType build()
    {
      this.skeleton.bones().forEachBreadthFirst(this, (t, depth, node) -> {
        final CaBone bone = node.value();

        final Optional<JOTreeNodeReadableType<CaBone>> parent_opt =
          node.parentReadable();
        final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> parent_transform;
        if (parent_opt.isPresent()) {
          final CaBone parent = parent_opt.get().value();
          parent_transform = t.transforms.get(parent.id());
        } else {
          parent_transform = null;
        }

        t.makeTransform(parent_transform, bone);
      });

      return new BuiltF(this.transforms);
    }
  }

  private static final class BuiltF implements CaSkeletonTransformsFType
  {
    private final Int2ReferenceOpenHashMap<PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceBoneAbsoluteType>> transforms;

    private BuiltF(
      final Int2ReferenceOpenHashMap<PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceBoneAbsoluteType>> in_transforms)
    {
      this.transforms = NullCheck.notNull(in_transforms, "transforms");
    }

    @Override
    public PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceBoneAbsoluteType> transformAbsolute4x4F(
      final int bone_id)
      throws NoSuchElementException
    {
      if (this.transforms.containsKey(bone_id)) {
        return this.transforms.get(bone_id);
      }
      throw new NoSuchElementException("No such bone: " + bone_id);
    }
  }
}
