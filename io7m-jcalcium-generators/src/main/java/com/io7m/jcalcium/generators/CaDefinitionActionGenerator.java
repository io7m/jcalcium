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

import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionCurves;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionType;
import com.io7m.junreachable.UnreachableCodeException;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;

/**
 * A generator for {@link CaDefinitionActionType}.
 */

public final class CaDefinitionActionGenerator implements Generator<CaDefinitionActionType>
{
  private final Generator<CaDefinitionActionCurves> curve_gen;
  private final IntegerGenerator which_gen;

  /**
   * Construct a new generator.
   *
   * @param bone_tree A bone tree
   */

  public CaDefinitionActionGenerator(final BoneTree bone_tree)
  {
    this.curve_gen = new CaDefinitionActionCurvesGenerator(bone_tree);
    this.which_gen = new IntegerGenerator(0, 0);
  }

  @Override
  public CaDefinitionActionType next()
  {
    switch (this.which_gen.nextInt()) {
      case 0: {
        return this.curve_gen.next();
      }
      default: {
        throw new UnreachableCodeException();
      }
    }
  }
}
