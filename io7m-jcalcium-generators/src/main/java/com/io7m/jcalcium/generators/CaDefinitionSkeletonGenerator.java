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
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.definitions.CaDefinitionJoint;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionActionType;
import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveType;
import com.io7m.jnull.NullCheck;
import javaslang.Tuple;
import javaslang.collection.List;
import javaslang.collection.Map;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static com.io7m.jfunctional.Unit.unit;

/**
 * A generator for {@link CaDefinitionSkeleton}.
 */

public final class CaDefinitionSkeletonGenerator
  implements Generator<CaDefinitionSkeleton>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaDefinitionSkeletonGenerator.class);
  }

  private final CaSkeletonNameGenerator name_gen;
  private final JointTree tree;
  private final CaDefinitionActionGenerator act_gen;

  /**
   * Construct a generator.
   *
   * @param in_tree A joint tree
   */

  public CaDefinitionSkeletonGenerator(
    final JointTree in_tree)
  {
    this.name_gen = new CaSkeletonNameGenerator();
    this.act_gen = new CaDefinitionActionGenerator(in_tree);
    this.tree = NullCheck.notNull(in_tree, "Tree");
  }

  @Override
  public CaDefinitionSkeleton next()
  {
    final Map<CaJointName, CaDefinitionJoint> joints = this.tree.nodes().map(
      (joint_name, joint_node) -> Tuple.of(joint_name, joint_node.value()));

    final Map<CaActionName, CaDefinitionActionType> actions =
      this.tree.nodes().map((joint_name, joint_node) -> {
        final CaDefinitionActionType act = this.act_gen.next();
        return Tuple.of(act.name(), act);
      });

    final CaSkeletonName skeleton_name = this.name_gen.next();
    LOG.trace("generated skeleton: {}", skeleton_name.value());
    LOG.trace("joints:   {}", Integer.valueOf(joints.size()));

    final AtomicInteger curve_count = new AtomicInteger(0);
    for (final CaActionName name : actions.keySet()) {
      LOG.trace("action: {}", name.value());

      final CaDefinitionActionType act = actions.get(name).get();
      act.matchAction(unit(), (x, y) -> {
        y.curves().forEach(curves_pair -> {
          final List<CaDefinitionCurveType> curves = curves_pair._2;
          curve_count.addAndGet(curves.size());
          curves.forEach(curve -> {
            LOG.trace(
              "action: {} joint {} curve type {}",
              name.value(),
              curve.joint().value(),
              curve.getClass().getSimpleName());
          });
        });
        return unit();
      });
    }

    LOG.trace("actions: {}", Integer.valueOf(actions.size()));
    LOG.trace("curves:  {}", Integer.valueOf(curve_count.get()));

    final CaDefinitionSkeleton.Builder sb = CaDefinitionSkeleton.builder();
    sb.setName(skeleton_name);
    sb.setJoints(joints);
    sb.setActions(actions);
    return sb.build();
  }
}
