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

package com.io7m.jcalcium.core.compiled.actions;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaImmutableStyleType;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jfunctional.PartialBiFunctionType;
import com.io7m.jnull.NullCheck;
import javaslang.collection.IndexedSeq;
import javaslang.collection.SortedMap;
import org.immutables.javaslang.encodings.JavaslangEncodingEnabled;
import org.immutables.value.Value;

import java.util.Objects;

/**
 * An action that is constructed from a set of keyframe curves.
 */

@CaImmutableStyleType
@JavaslangEncodingEnabled
@Value.Immutable
public interface CaActionCurvesType extends CaActionType
{
  @Override
  default <A, B, E extends Exception> B matchAction(
    final A context,
    final PartialBiFunctionType<A, CaActionCurvesType, B, E> on_curves)
    throws E
  {
    return NullCheck.notNull(on_curves, "on_curves").call(context, this);
  }

  @Value.Parameter
  @Override
  CaActionName name();

  @Value.Parameter
  @Override
  int framesPerSecond();

  /**
   * @return The curves for the action
   */

  @Value.Parameter
  SortedMap<CaJointName, IndexedSeq<CaCurveType>> curves();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    Preconditions.checkPreconditionI(
      this.framesPerSecond(),
      this.framesPerSecond() > 0,
      f -> "Frames per second must be > 0");

    this.curves().forEach(
      (joint_name, sequence) ->
        sequence.forEach(curve -> {
          Preconditions.checkPrecondition(
            Objects.equals(curve.action(), this.name()),
            () -> "Action names must match (" + curve.action() + " == " + this.name() + ")");
          Preconditions.checkPrecondition(
            Objects.equals(curve.joint(), joint_name),
            () -> "Joint names must match (" + curve.joint() + " == " + joint_name + ")");
        }));
  }
}
