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

import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPoseDType;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMaps;

/**
 * The default implementation of the {@link CaEvaluatedSkeletonMutableDType}
 * interface.
 */

public final class CaEvaluatedSkeletonD implements
  CaEvaluatedSkeletonMutableDType
{
  private final CaSkeletonRestPoseDType rest_pose;
  private final JOTreeNodeType<CaEvaluatedJointMutableDType> joints;
  private final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> joints_view;
  private final Int2ReferenceSortedMap<CaEvaluatedJointMutableDType> joints_by_id;
  private final Int2ReferenceSortedMap<CaEvaluatedJointReadableDType> joints_by_id_view;
  private final CaEvaluationContextType context;

  private CaEvaluatedSkeletonD(
    final CaEvaluationContextType in_context,
    final CaSkeletonRestPoseDType in_rest_pose)
  {
    this.context =
      NullCheck.notNull(in_context, "Context");
    this.rest_pose =
      NullCheck.notNull(in_rest_pose, "Rest pose");

    this.joints_by_id =
      new Int2ReferenceRBTreeMap<>();
    this.joints_by_id_view =
      Int2ReferenceSortedMaps.unmodifiable(castMap(this.joints_by_id));

    final CaSkeleton skeleton = in_rest_pose.skeleton();
    this.joints = skeleton.joints().mapBreadthFirst(
      this, (t, depth, node) -> {
        final CaJoint c_joint = node.value();

        final CaEvaluatedJointMutableDType c_state =
          CaEvaluatedJointMutableD.create(t.context, t, c_joint.id());

        this.joints_by_id.put(c_joint.id(), c_state);
        return c_state;
      });

    @SuppressWarnings("unchecked")
    final JOTreeNodeReadableType<CaEvaluatedJointReadableDType> view_typed =
      (JOTreeNodeReadableType<CaEvaluatedJointReadableDType>) (Object) this.joints;
    this.joints_view = view_typed;
  }

  @SuppressWarnings("unchecked")
  private static <A, B extends A> Int2ReferenceSortedMap<A> castMap(
    final Int2ReferenceSortedMap<B> m)
  {
    return (Int2ReferenceSortedMap<A>) m;
  }

  /**
   * Create a new mutable skeleton.
   *
   * @param in_context   An evaluation context
   * @param in_rest_pose The skeleton's rest pose
   *
   * @return A new skeleton
   */

  public static CaEvaluatedSkeletonMutableDType create(
    final CaEvaluationContextType in_context,
    final CaSkeletonRestPoseDType in_rest_pose)
  {
    return new CaEvaluatedSkeletonD(in_context, in_rest_pose);
  }

  @Override
  public JOTreeNodeReadableType<CaEvaluatedJointReadableDType> joints()
  {
    return this.joints_view;
  }

  @Override
  public Int2ReferenceSortedMap<CaEvaluatedJointReadableDType> jointsByID()
  {
    return this.joints_by_id_view;
  }

  @Override
  public CaSkeletonRestPoseDType restPose()
  {
    return this.rest_pose;
  }

  @Override
  public JOTreeNodeReadableType<CaEvaluatedJointMutableDType> jointsMutable()
  {
    return this.joints;
  }

  @Override
  public Int2ReferenceSortedMap<CaEvaluatedJointMutableDType> jointsMutableByID()
  {
    return this.joints_by_id;
  }
}
