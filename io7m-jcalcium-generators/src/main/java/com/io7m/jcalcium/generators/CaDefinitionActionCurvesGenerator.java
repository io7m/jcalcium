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

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionCurves;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveType;
import com.io7m.jnull.NullCheck;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;

import java.util.Objects;

/**
 * A generator for {@link CaDefinitionActionCurves}.
 */

public final class CaDefinitionActionCurvesGenerator
  implements Generator<CaDefinitionActionCurves>
{
  private final Generator<CaActionName> act_name_gen;
  private final IntegerGenerator fps_gen;
  private final BoneTree tree;
  private final CaDefinitionCurveGenerator curve_gen;

  /**
   * Construct a generator.
   *
   * @param in_tree A bone tree
   */

  public CaDefinitionActionCurvesGenerator(
    final BoneTree in_tree)
  {
    this.act_name_gen = new CaActionNameGenerator();
    this.fps_gen = new IntegerGenerator(1, 300);
    this.curve_gen = new CaDefinitionCurveGenerator(in_tree);
    this.tree = NullCheck.notNull(in_tree, "tree");
  }

  @Override
  public CaDefinitionActionCurves next()
  {
    List<CaDefinitionCurveType> curves = List.empty();
    for (int index = 0; index < this.tree.nodes().size(); ++index) {
      curves = curves.append(this.curve_gen.next());
    }

    Map<CaBoneName, List<CaDefinitionCurveType>> m = HashMap.empty();
    for (final CaDefinitionCurveType curve : curves) {
      final CaBoneName bone_name = curve.bone();
      if (m.containsKey(bone_name)) {
        final List<CaDefinitionCurveType> xs = m.get(bone_name).get();
        if (xs.filter(c -> Objects.equals(c.getClass(), curve.getClass())).isEmpty()) {
          m = m.put(bone_name, xs.append(curve));
        }
      } else {
        m = m.put(bone_name, List.of(curve));
      }
    }

    final CaDefinitionActionCurves.Builder ab =
      CaDefinitionActionCurves.builder();
    ab.setFramesPerSecond(this.fps_gen.nextInt());
    ab.setName(this.act_name_gen.next());
    ab.setCurves(m);
    return ab.build();
  }
}
