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
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jorchard.generators.JOTreeNodeGenerator;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;

import java.util.HashMap;

/**
 * A generator for bone trees.
 */

public final class BoneNameTreeGenerator implements Generator<BoneNameTree>
{
  private final CaBoneNameGenerator names;
  private final IntegerGenerator sizes;
  private final Generator<JOTreeNodeType<CaBoneName>> tree_gen;

  /**
   * Construct a bone tree generator.
   */

  public BoneNameTreeGenerator()
  {
    this.names = new CaBoneNameGenerator();
    this.sizes = new IntegerGenerator(1, 32);
    this.tree_gen = JOTreeNodeGenerator.create(this.sizes, this.names, 0.2);
  }

  @Override
  public BoneNameTree next()
  {
    final JOTreeNodeType<CaBoneName> tree = this.tree_gen.next();
    final HashMap<CaBoneName, JOTreeNodeReadableType<CaBoneName>> map =
      new HashMap<>();
    tree.forEachDepthFirst(
      map, (input, depth, node) -> map.put(node.value(), node));
    return new BoneNameTree(tree, javaslang.collection.HashMap.ofAll(map));
  }
}
