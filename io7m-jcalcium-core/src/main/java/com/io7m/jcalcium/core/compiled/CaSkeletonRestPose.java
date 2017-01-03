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

import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
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

public final class CaSkeletonRestPose
{
  private CaSkeletonRestPose()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Calculate a set of rest pose transforms for the given skeleton.
   *
   * @param c        Preallocated storage for processing matrices without extra
   *                 allocations
   * @param skeleton The skeleton
   *
   * @return A set of rest pose transforms
   */

  public static CaSkeletonRestPoseDType createD(
    final MatrixM4x4D.ContextMM4D c,
    final CaSkeleton skeleton)
  {
    NullCheck.notNull(skeleton, "Skeleton");
    return new BuildD(skeleton).build(c);
  }

  /**
   * Calculate a set of rest pose transforms for the given skeleton.
   *
   * @param c        Preallocated storage for processing matrices without extra
   *                 allocations
   * @param skeleton The skeleton
   *
   * @return A set of rest pose transforms
   */

  public static CaSkeletonRestPoseFType createF(
    final MatrixM4x4F.ContextMM4F c,
    final CaSkeleton skeleton)
  {
    NullCheck.notNull(skeleton, "Skeleton");
    return new BuildF(skeleton).build(c);
  }

  private static final class BuildD
  {
    private final Matrix4x4DType m_translation;
    private final Matrix4x4DType m_orientation;
    private final Matrix4x4DType m_scale;
    private final Matrix4x4DType m_accumulated;
    private final Int2ReferenceOpenHashMap<PMatrix4x4DType<CaSpaceObjectType, CaSpaceJointType>> transforms;
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
      final @Nullable PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceJointType> joint_parent_transform,
      final CaJointType joint)
    {
      MatrixM4x4D.makeTranslation3D(
        joint.translation(), this.m_translation);

      QuaternionM4D.makeRotationMatrix4x4(
        joint.orientation(), this.m_orientation);

      MatrixM4x4D.setIdentity(this.m_scale);
      this.m_scale.setR0C0D(joint.scale().getXD());
      this.m_scale.setR1C1D(joint.scale().getYD());
      this.m_scale.setR2C2D(joint.scale().getZD());

      MatrixM4x4D.multiply(
        this.m_translation, this.m_orientation, this.m_accumulated);
      MatrixM4x4D.multiply(
        this.m_accumulated, this.m_scale, this.m_accumulated);

      final PMatrix4x4DType<CaSpaceObjectType, CaSpaceJointType> transform =
        PMatrixHeapArrayM4x4D.newMatrix();
      if (joint_parent_transform != null) {
        MatrixM4x4D.multiply(
          joint_parent_transform, this.m_accumulated, transform);
      } else {
        MatrixM4x4D.copy(this.m_accumulated, transform);
      }

      this.transforms.put(joint.id(), transform);
    }

    CaSkeletonRestPoseDType build(
      final MatrixM4x4D.ContextMM4D c)
    {
      this.skeleton.joints().forEachBreadthFirst(this, (t, depth, node) -> {
        final CaJoint joint = node.value();

        final Optional<JOTreeNodeReadableType<CaJoint>> parent_opt =
          node.parentReadable();
        final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceJointType> parent_transform;
        if (parent_opt.isPresent()) {
          final CaJoint parent = parent_opt.get().value();
          parent_transform = t.transforms.get(parent.id());
        } else {
          parent_transform = null;
        }

        t.makeTransform(parent_transform, joint);
      });

      /*
       * Invert all transform matrices.
       */

      for (final PMatrix4x4DType<CaSpaceObjectType, CaSpaceJointType> v : this.transforms.values()) {
        MatrixM4x4D.invertInPlace(c, v);
      }

      return new BuiltD(this.skeleton, this.transforms);
    }
  }

  private static final class BuiltD implements CaSkeletonRestPoseDType
  {
    private final Int2ReferenceOpenHashMap<PMatrix4x4DType<CaSpaceObjectType, CaSpaceJointType>> transforms;
    private final CaSkeleton skeleton;

    private BuiltD(
      final CaSkeleton in_skeleton,
      final Int2ReferenceOpenHashMap<PMatrix4x4DType<CaSpaceObjectType, CaSpaceJointType>> in_transforms)
    {
      this.skeleton = NullCheck.notNull(in_skeleton, "Skeleton");
      this.transforms = NullCheck.notNull(in_transforms, "transforms");
    }

    @Override
    public PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceJointType>
    transformInverseRest4x4D(
      final int joint_id)
      throws NoSuchElementException
    {
      if (this.transforms.containsKey(joint_id)) {
        return this.transforms.get(joint_id);
      }
      throw new NoSuchElementException("No such joint: " + joint_id);
    }

    @Override
    public CaSkeleton skeleton()
    {
      return this.skeleton;
    }
  }

  private static final class BuildF
  {
    private final Matrix4x4FType m_translation;
    private final Matrix4x4FType m_orientation;
    private final Matrix4x4FType m_scale;
    private final Matrix4x4FType m_accumulated;
    private final Int2ReferenceOpenHashMap<PMatrix4x4FType<CaSpaceObjectType, CaSpaceJointType>> transforms;
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
      final @Nullable MatrixReadable4x4FType joint_parent_transform,
      final CaJointType joint)
    {
      final PVectorI3D<CaSpaceJointType> translation =
        joint.translation();

      final VectorI3F translation_f = new VectorI3F(
        (float) translation.getXD(),
        (float) translation.getYD(),
        (float) translation.getZD());

      MatrixM4x4F.makeTranslation3F(
        translation_f, this.m_translation);

      final QuaternionI4D orientation_d =
        joint.orientation();

      final QuaternionI4F orientation_f =
        new QuaternionI4F(
          (float) orientation_d.getXD(),
          (float) orientation_d.getYD(),
          (float) orientation_d.getZD(),
          (float) orientation_d.getWD());

      QuaternionM4F.makeRotationMatrix4x4(
        orientation_f, this.m_orientation);

      MatrixM4x4F.setIdentity(this.m_scale);
      this.m_scale.setR0C0F((float) joint.scale().getXD());
      this.m_scale.setR1C1F((float) joint.scale().getYD());
      this.m_scale.setR2C2F((float) joint.scale().getZD());

      MatrixM4x4F.multiply(
        this.m_translation, this.m_orientation, this.m_accumulated);
      MatrixM4x4F.multiply(
        this.m_accumulated, this.m_scale, this.m_accumulated);

      final PMatrix4x4FType<CaSpaceObjectType, CaSpaceJointType> transform =
        PMatrixHeapArrayM4x4F.newMatrix();

      if (joint_parent_transform != null) {
        MatrixM4x4F.multiply(
          joint_parent_transform, this.m_accumulated, transform);
      } else {
        MatrixM4x4F.copy(this.m_accumulated, transform);
      }

      this.transforms.put(joint.id(), transform);
    }

    CaSkeletonRestPoseFType build(
      final MatrixM4x4F.ContextMM4F c)
    {
      this.skeleton.joints().forEachBreadthFirst(this, (t, depth, node) -> {
        final CaJoint joint = node.value();

        final Optional<JOTreeNodeReadableType<CaJoint>> parent_opt =
          node.parentReadable();
        final PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceJointType> parent_transform;
        if (parent_opt.isPresent()) {
          final CaJoint parent = parent_opt.get().value();
          parent_transform = t.transforms.get(parent.id());
        } else {
          parent_transform = null;
        }

        t.makeTransform(parent_transform, joint);
      });

      /*
       * Invert all transform matrices.
       */

      for (final PMatrix4x4FType<CaSpaceObjectType, CaSpaceJointType> v : this.transforms.values()) {
        MatrixM4x4F.invertInPlace(c, v);
      }

      return new BuiltF(this.skeleton, this.transforms);
    }
  }

  private static final class BuiltF implements CaSkeletonRestPoseFType
  {
    private final Int2ReferenceOpenHashMap<PMatrix4x4FType<CaSpaceObjectType, CaSpaceJointType>> transforms;
    private final CaSkeleton skeleton;

    private BuiltF(
      final CaSkeleton in_skeleton,
      final Int2ReferenceOpenHashMap<PMatrix4x4FType<CaSpaceObjectType, CaSpaceJointType>> in_transforms)
    {
      this.skeleton = NullCheck.notNull(in_skeleton, "Skeleton");
      this.transforms = NullCheck.notNull(in_transforms, "transforms");
    }

    @Override
    public PMatrixReadable4x4FType<CaSpaceObjectType, CaSpaceJointType>
    transformInverseRest4x4F(
      final int joint_id)
      throws NoSuchElementException
    {
      if (this.transforms.containsKey(joint_id)) {
        return this.transforms.get(joint_id);
      }
      throw new NoSuchElementException("No such joint: " + joint_id);
    }

    @Override
    public CaSkeleton skeleton()
    {
      return this.skeleton;
    }
  }
}
