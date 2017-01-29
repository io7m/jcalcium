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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaImmutableStyleType;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
import org.immutables.javaslang.encodings.JavaslangEncodingEnabled;
import org.immutables.value.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.io7m.jfunctional.Unit.unit;

/**
 * The type of compiled skeletons.
 */

@CaImmutableStyleType
@JavaslangEncodingEnabled
@Value.Immutable
public interface CaSkeletonType
{
  /**
   * @return The skeleton metadata
   */

  @Value.Parameter
  CaSkeletonMetadata meta();

  /**
   * @return The tree of joints for the skeleton
   */

  @Value.Parameter
  JOTreeNodeReadableType<CaJoint> joints();

  /**
   * @return The actions by name
   */

  @Value.Parameter
  SortedMap<CaActionName, CaActionType> actionsByName();

  /**
   * @return A map of joint nodes by name
   */

  @Value.Derived
  default SortedMap<CaJointName, JOTreeNodeReadableType<CaJoint>> jointsByName()
  {
    final Map<CaJointName, JOTreeNodeReadableType<CaJoint>> hm = new HashMap<>();
    this.joints().forEachBreadthFirst(
      hm, (t, depth, node) -> {
        final CaJoint joint = node.value();
        Preconditions.checkPrecondition(
          joint.name(),
          !t.containsKey(joint.name()),
          name -> "Joint name must be unique");
        t.put(joint.name(), node);
      });
    return TreeMap.ofAll(hm);
  }

  /**
   * @return A map of joint nodes by ID
   */

  @Value.Derived
  default SortedMap<Integer, JOTreeNodeReadableType<CaJoint>> jointsByID()
  {
    final Map<Integer, JOTreeNodeReadableType<CaJoint>> hm = new HashMap<>();
    this.joints().forEachBreadthFirst(
      hm, (t, depth, node) -> {
        final CaJoint joint = node.value();
        Preconditions.checkPreconditionI(
          joint.id(),
          !t.containsKey(Integer.valueOf(joint.id())),
          id -> "Joint ID must be unique");
        t.put(Integer.valueOf(joint.id()), node);
      });
    return TreeMap.ofAll(hm);
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    Preconditions.checkPrecondition(
      this.jointsByID().size() == this.jointsByName().size(),
      "Joint maps must be the same size");

    this.joints().forEachBreadthFirst(this, (t, depth, node) -> {
      final CaJoint joint = node.value();
      final Integer joint_id = Integer.valueOf(joint.id());

      Preconditions.checkPrecondition(
        joint_id,
        t.jointsByID().containsKey(joint_id),
        id -> "Joint " + id + " must exist in ID map");
      Preconditions.checkPrecondition(
        joint.name(),
        t.jointsByName().containsKey(joint.name()),
        name -> "Joint " + name + " must exist in name map");

      final JOTreeNodeReadableType<CaJoint> joint_by_name =
        t.jointsByName().get(joint.name()).get();
      final JOTreeNodeReadableType<CaJoint> joint_by_id =
        t.jointsByID().get(joint_id).get();

      Preconditions.checkPrecondition(
        joint.name(),
        Objects.equals(joint_by_name.value().name(), joint.name()),
        name -> "Joint name " + name + " must match name in map");
      Preconditions.checkPrecondition(
        joint_id,
        joint_by_id.value().id() == joint.id(),
        id -> "Joint ID " + id + " must match id in map");
    });

    this.actionsByName().forEach(pair -> {
      final CaActionName act_name = pair._1;
      final CaActionType act = pair._2;

      Preconditions.checkPrecondition(
        act_name,
        Objects.equals(act_name, act.name()),
        name -> "Action name " + name + " must match name in map");

      act.matchAction(this, (t, act_curves) -> {
        act_curves.curves().forEach(curve_pair -> {
          final CaJointName act_joint = curve_pair._1;

          Preconditions.checkPrecondition(
            act_joint,
            t.jointsByName().containsKey(act_joint),
            name -> "Action must not refer to nonexistent joint " + name);

          curve_pair._2.forEach(curve -> {
            Preconditions.checkPrecondition(
              Objects.equals(curve.joint(), act_joint),
              "Curve must refer to correct joint");
            Preconditions.checkPrecondition(
              Objects.equals(curve.action(), act_name),
              "Curve must refer to correct action");
          });
        });
        return unit();
      });
    });
  }
}
