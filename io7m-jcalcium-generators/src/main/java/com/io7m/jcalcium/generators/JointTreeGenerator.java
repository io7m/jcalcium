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
import com.io7m.jcalcium.core.definitions.CaDefinitionJoint;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
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
 * A generator for joint trees.
 */

public final class JointTreeGenerator implements Generator<JointTree>
{
  private final Generator<JOTreeNodeType<CaJointName>> tree_gen;
  private final VectorI3DGenerator vec_gen;
  private final PVectorI3DGenerator<CaSpaceJointType> pvec_gen;
  private final QuaternionI4DGenerator quat_gen;

  /**
   * Construct a joint tree generator.
   */

  public JointTreeGenerator()
  {
    final CaJointNameGenerator names = new CaJointNameGenerator();
    final IntegerGenerator sizes = new IntegerGenerator(1, 32);
    this.tree_gen = JOTreeNodeGenerator.create(sizes, names, 0.2);
    this.vec_gen = new VectorI3DGenerator();
    this.pvec_gen = new PVectorI3DGenerator<>();
    this.quat_gen = new QuaternionI4DGenerator();
  }

  @Override
  public JointTree next()
  {
    final JOTreeNodeType<CaJointName> tree =
      this.tree_gen.next();
    final HashMap<CaJointName, JOTreeNodeReadableType<CaDefinitionJoint>> map =
      new HashMap<>();

    final JOTreeNodeType<CaDefinitionJoint> out =
      tree.mapDepthFirst(map, (input, depth, node) -> {

        final CaDefinitionJoint.Builder b = CaDefinitionJoint.builder();
        b.setName(node.value());
        b.setScale(this.vec_gen.next());
        b.setTranslation(this.pvec_gen.next());
        b.setOrientation(this.quat_gen.next());

        final Optional<JOTreeNodeReadableType<CaJointName>> parent_opt =
          node.parentReadable();
        if (parent_opt.isPresent()) {
          final JOTreeNodeReadableType<CaJointName> parent_name = parent_opt.get();
          b.setParent(parent_name.value());
        }

        return b.build();
      });

    out.forEachDepthFirst(map, (input, depth, node) -> map.put(node.value().name(), node));

    return new JointTree(out, javaslang.collection.HashMap.ofAll(map));
  }
}
