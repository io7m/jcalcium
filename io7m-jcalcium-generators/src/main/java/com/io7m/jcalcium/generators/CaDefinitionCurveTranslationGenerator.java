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
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeTranslation;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveKeyframeTranslationType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveTranslation;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.generators.parameterized.PVectorI3DGenerator;
import javaslang.collection.List;
import net.java.quickcheck.Generator;

import java.util.Collections;

/**
 * A generator for {@link CaDefinitionCurveTranslation}.
 */

public final class CaDefinitionCurveTranslationGenerator implements Generator<CaDefinitionCurveTranslation>
{
  private final BoneTree tree;
  private final PVectorI3DGenerator<CaSpaceBoneParentRelativeType> pvec_gen;
  private final Generator<CaCurveInterpolation> interp_gen;
  private final Generator<CaCurveEasing> easing_gen;

  /**
   * Construct a generator.
   *
   * @param in_interp_gen An interpolation generator
   * @param in_easing_gen An easing generator
   * @param in_pvec_gen   A vector generator
   * @param in_tree       A tree
   */

  public CaDefinitionCurveTranslationGenerator(
    final Generator<CaCurveInterpolation> in_interp_gen,
    final Generator<CaCurveEasing> in_easing_gen,
    final PVectorI3DGenerator<CaSpaceBoneParentRelativeType> in_pvec_gen,
    final BoneTree in_tree)
  {
    this.tree = NullCheck.notNull(in_tree, "Tree");
    this.pvec_gen = NullCheck.notNull(in_pvec_gen, "PVectorGenerator");
    this.interp_gen = NullCheck.notNull(in_interp_gen, "Interpolation");
    this.easing_gen = NullCheck.notNull(in_easing_gen, "Easing");
  }

  @Override
  public CaDefinitionCurveTranslation next()
  {
    List<CaDefinitionCurveKeyframeTranslationType> keyframes = List.empty();

    int index = 0;
    for (int count = 0; count < 10; ++count) {
      final CaDefinitionCurveKeyframeTranslation.Builder kf_b =
        CaDefinitionCurveKeyframeTranslation.builder();
      kf_b.setIndex(index);
      kf_b.setTranslation(this.pvec_gen.next());
      kf_b.setEasing(this.easing_gen.next());
      kf_b.setInterpolation(this.interp_gen.next());
      keyframes = keyframes.append(kf_b.build());
      index = index + (int) (Math.random() * 50.0 + 1.0);
    }

    final CaDefinitionCurveTranslation.Builder b =
      CaDefinitionCurveTranslation.builder();

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
