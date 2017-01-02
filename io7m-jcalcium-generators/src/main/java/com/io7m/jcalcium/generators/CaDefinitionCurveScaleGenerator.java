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

package com.io7m.jcalcium.generators;

import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeScale;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeScaleType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveScale;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.generators.VectorI3DGenerator;
import javaslang.collection.List;
import net.java.quickcheck.Generator;

import java.util.Collections;

/**
 * A generator for {@link CaDefinitionCurveScale}.
 */

public final class CaDefinitionCurveScaleGenerator implements Generator<CaDefinitionCurveScale>
{
  private final JointTree tree;
  private final Generator<CaCurveInterpolation> interp_gen;
  private final Generator<CaCurveEasing> easing_gen;
  private final VectorI3DGenerator vec_gen;

  /**
   * Construct a generator.
   *
   * @param in_interp_gen An interpolation generator
   * @param in_easing_gen An easing generator
   * @param in_vec_gen    A quaternion generator
   * @param in_tree       A tree
   */

  public CaDefinitionCurveScaleGenerator(
    final Generator<CaCurveInterpolation> in_interp_gen,
    final Generator<CaCurveEasing> in_easing_gen,
    final VectorI3DGenerator in_vec_gen,
    final JointTree in_tree)
  {
    this.tree = NullCheck.notNull(in_tree, "Tree");
    this.vec_gen = NullCheck.notNull(in_vec_gen, "VectorI3DGenerator");
    this.interp_gen = NullCheck.notNull(in_interp_gen, "Interpolation");
    this.easing_gen = NullCheck.notNull(in_easing_gen, "Easing");
  }

  @Override
  public CaDefinitionCurveScale next()
  {
    List<CaDefinitionCurveKeyframeScaleType> keyframes = List.empty();

    int index = 0;
    for (int count = 0; count < 10; ++count) {
      final CaDefinitionCurveKeyframeScale.Builder kf_b =
        CaDefinitionCurveKeyframeScale.builder();
      kf_b.setIndex(index);
      kf_b.setScale(this.vec_gen.next());
      kf_b.setEasing(this.easing_gen.next());
      kf_b.setInterpolation(this.interp_gen.next());
      keyframes = keyframes.append(kf_b.build());
      index = index + (int) (Math.random() * 50.0 + 1.0);
    }

    final CaDefinitionCurveScale.Builder b =
      CaDefinitionCurveScale.builder();

    final java.util.List<CaJointName> names_mut =
      List.ofAll(this.tree.nodes().keySet()).toJavaList();
    Collections.shuffle(names_mut);
    final List<CaJointName> names = List.ofAll(names_mut);

    final CaJointName joint_name = names.head();
    b.setJoint(joint_name);
    b.setKeyframes(keyframes);
    return b.build();
  }
}
