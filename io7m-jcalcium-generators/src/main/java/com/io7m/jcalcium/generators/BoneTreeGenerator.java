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
import com.io7m.jcalcium.core.definitions.CaDefinitionBone;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jorchard.generators.JOTreeNodeGenerator;
import com.io7m.jtensors.generators.QuaternionI4DGenerator;
import com.io7m.jtensors.generators.VectorI3DGenerator;
import com.io7m.jtensors.generators.parameterized.PVectorI3DGenerator;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;

import java.util.HashMap;
import java.util.Optional;

/**
 * A generator for bone trees.
 */

public final class BoneTreeGenerator implements Generator<BoneTree>
{
  private final CaBoneNameGenerator names;
  private final IntegerGenerator sizes;
  private final Generator<JOTreeNodeType<CaBoneName>> tree_gen;
  private final VectorI3DGenerator vec_gen;
  private final PVectorI3DGenerator<CaSpaceBoneParentRelativeType> pvec_gen;
  private final QuaternionI4DGenerator quat_gen;

  /**
   * Construct a bone tree generator.
   */

  public BoneTreeGenerator()
  {
    this.names = new CaBoneNameGenerator();
    this.sizes = new IntegerGenerator(1, 32);
    this.tree_gen = JOTreeNodeGenerator.create(this.sizes, this.names, 0.2);
    this.vec_gen = new VectorI3DGenerator();
    this.pvec_gen = new PVectorI3DGenerator<>();
    this.quat_gen = new QuaternionI4DGenerator();
  }

  @Override
  public BoneTree next()
  {
    final JOTreeNodeType<CaBoneName> tree =
      this.tree_gen.next();
    final HashMap<CaBoneName, JOTreeNodeReadableType<CaDefinitionBone>> map =
      new HashMap<>();

    final JOTreeNodeType<CaDefinitionBone> out =
      tree.mapDepthFirst(map, (input, depth, node) -> {

        final CaDefinitionBone.Builder b = CaDefinitionBone.builder();
        b.setName(node.value());
        b.setScale(this.vec_gen.next());
        b.setTranslation(this.pvec_gen.next());
        b.setOrientation(this.quat_gen.next());

        final Optional<JOTreeNodeReadableType<CaBoneName>> parent_opt =
          node.parentReadable();
        if (parent_opt.isPresent()) {
          final JOTreeNodeReadableType<CaBoneName> parent_name = parent_opt.get();
          b.setParent(parent_name.value());
        }

        return b.build();
      });

    out.forEachDepthFirst(map, (input, depth, node) -> {
      map.put(node.value().name(), node);
    });

    return new BoneTree(out, javaslang.collection.HashMap.ofAll(map));
  }
}
