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
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.CaImmutableStyleType;
import com.io7m.jcalcium.core.CaSkeletonName;
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
   * @return The name of the skeleton
   */

  @Value.Parameter
  CaSkeletonName name();

  /**
   * @return The tree of bones for the skeleton
   */

  @Value.Parameter
  JOTreeNodeReadableType<CaBone> bones();

  /**
   * @return The actions by name
   */

  @Value.Parameter
  SortedMap<CaActionName, CaActionType> actionsByName();

  /**
   * @return A map of bone nodes by name
   */

  @Value.Derived
  default SortedMap<CaBoneName, JOTreeNodeReadableType<CaBone>> bonesByName()
  {
    final Map<CaBoneName, JOTreeNodeReadableType<CaBone>> hm = new HashMap<>();
    this.bones().forEachBreadthFirst(
      hm, (t, depth, node) -> {
        final CaBone bone = node.value();
        Preconditions.checkPrecondition(
          bone.name(),
          !t.containsKey(bone.name()),
          name -> "Bone name must be unique");
        t.put(bone.name(), node);
      });
    return TreeMap.ofAll(hm);
  }

  /**
   * @return A map of bone nodes by ID
   */

  @Value.Derived
  default SortedMap<Integer, JOTreeNodeReadableType<CaBone>> bonesByID()
  {
    final Map<Integer, JOTreeNodeReadableType<CaBone>> hm = new HashMap<>();
    this.bones().forEachBreadthFirst(
      hm, (t, depth, node) -> {
        final CaBone bone = node.value();
        Preconditions.checkPreconditionI(
          bone.id(),
          !t.containsKey(Integer.valueOf(bone.id())),
          id -> "Bone ID must be unique");
        t.put(Integer.valueOf(bone.id()), node);
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
      this.bonesByID().size() == this.bonesByName().size(),
      "Bone maps must be the same size");

    this.bones().forEachBreadthFirst(this, (t, depth, node) -> {
      final CaBone bone = node.value();
      final Integer bone_id = Integer.valueOf(bone.id());

      Preconditions.checkPrecondition(
        bone_id,
        t.bonesByID().containsKey(bone_id),
        id -> "Bone " + id + " must exist in ID map");
      Preconditions.checkPrecondition(
        bone.name(),
        t.bonesByName().containsKey(bone.name()),
        name -> "Bone " + name + " must exist in name map");

      final JOTreeNodeReadableType<CaBone> bone_by_name =
        t.bonesByName().get(bone.name()).get();
      final JOTreeNodeReadableType<CaBone> bone_by_id =
        t.bonesByID().get(bone_id).get();

      Preconditions.checkPrecondition(
        bone.name(),
        Objects.equals(bone_by_name.value().name(), bone.name()),
        name -> "Bone name " + name + " must match name in map");
      Preconditions.checkPrecondition(
        bone_id,
        bone_by_id.value().id() == bone.id(),
        id -> "Bone ID " + id + " must match id in map");
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
          final CaBoneName act_bone = curve_pair._1;

          Preconditions.checkPrecondition(
            act_bone,
            t.bonesByName().containsKey(act_bone),
            name -> "Action must not refer to nonexistent bone " + name);

          curve_pair._2.forEach(curve -> {
            Preconditions.checkPrecondition(
              Objects.equals(curve.bone(), act_bone),
              "Curve must refer to correct bone");
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
