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
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jorchard.generators.JOTreeNodeGenerator;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;

import java.util.HashMap;

/**
 * A generator for joint trees.
 */

public final class JointNameTreeGenerator implements Generator<JointNameTree>
{
  private final CaJointNameGenerator names;
  private final IntegerGenerator sizes;
  private final Generator<JOTreeNodeType<CaJointName>> tree_gen;

  /**
   * Construct a joint tree generator.
   */

  public JointNameTreeGenerator()
  {
    this.names = new CaJointNameGenerator();
    this.sizes = new IntegerGenerator(1, 32);
    this.tree_gen = JOTreeNodeGenerator.create(this.sizes, this.names, 0.2);
  }

  @Override
  public JointNameTree next()
  {
    final JOTreeNodeType<CaJointName> tree = this.tree_gen.next();
    final HashMap<CaJointName, JOTreeNodeReadableType<CaJointName>> map =
      new HashMap<>();
    tree.forEachDepthFirst(
      map, (input, depth, node) -> map.put(node.value(), node));
    return new JointNameTree(tree, javaslang.collection.HashMap.ofAll(map));
  }
}
