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

import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeOrientation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeOrientationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveOrientation;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.generators.QuaternionI4DGenerator;
import javaslang.collection.List;
import net.java.quickcheck.Generator;

import java.util.Collections;

/**
 * A generator for {@link CaDefinitionCurveOrientation}.
 */

public final class CaDefinitionCurveOrientationGenerator implements Generator<CaDefinitionCurveOrientation>
{
  private final BoneTree tree;
  private final Generator<CaCurveInterpolation> interp_gen;
  private final Generator<CaCurveEasing> easing_gen;
  private final QuaternionI4DGenerator quat_gen;

  /**
   * Construct a generator.
   *
   * @param in_interp_gen An interpolation generator
   * @param in_easing_gen An easing generator
   * @param in_quat_gen   A quaternion generator
   * @param in_tree       A tree
   */

  public CaDefinitionCurveOrientationGenerator(
    final Generator<CaCurveInterpolation> in_interp_gen,
    final Generator<CaCurveEasing> in_easing_gen,
    final QuaternionI4DGenerator in_quat_gen,
    final BoneTree in_tree)
  {
    this.tree = NullCheck.notNull(in_tree, "Tree");
    this.quat_gen = NullCheck.notNull(in_quat_gen, "QuaternionI4DGenerator");
    this.interp_gen = NullCheck.notNull(in_interp_gen, "Interpolation");
    this.easing_gen = NullCheck.notNull(in_easing_gen, "Easing");
  }

  @Override
  public CaDefinitionCurveOrientation next()
  {
    List<CaDefinitionCurveKeyframeOrientationType> keyframes = List.empty();

    int index = 0;
    for (int count = 0; count < 10; ++count) {
      final CaDefinitionCurveKeyframeOrientation.Builder kf_b =
        CaDefinitionCurveKeyframeOrientation.builder();
      kf_b.setIndex(index);
      kf_b.setOrientation(this.quat_gen.next());
      kf_b.setEasing(this.easing_gen.next());
      kf_b.setInterpolation(this.interp_gen.next());
      keyframes = keyframes.append(kf_b.build());
      index = index + (int) (Math.random() * 50.0 + 1.0);
    }

    final CaDefinitionCurveOrientation.Builder b =
      CaDefinitionCurveOrientation.builder();

    final java.util.List<CaBoneName> names_mut =
      List.ofAll(this.tree.nodes().keySet()).toJavaList();
    Collections.shuffle(names_mut);
    final List<CaBoneName> names = List.ofAll(names_mut);

    final CaBoneName bone_name = names.head();
    b.setBone(bone_name);
    b.setKeyframes(keyframes);
    return b.build();
  }
}
