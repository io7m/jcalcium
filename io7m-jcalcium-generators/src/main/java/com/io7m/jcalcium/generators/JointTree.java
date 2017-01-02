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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.definitions.CaDefinitionJoint;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import javaslang.collection.Map;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A generated joint tree.
 */

public final class JointTree
{
  private final JOTreeNodeType<CaDefinitionJoint> tree;
  private final Map<CaJointName, JOTreeNodeReadableType<CaDefinitionJoint>> nodes;

  /**
   * Construct a joint tree.
   *
   * @param in_tree  The tree
   * @param in_nodes The nodes
   */

  public JointTree(
    final JOTreeNodeType<CaDefinitionJoint> in_tree,
    final Map<CaJointName, JOTreeNodeReadableType<CaDefinitionJoint>> in_nodes)
  {
    this.tree = NullCheck.notNull(in_tree, "in_tree");
    this.nodes = NullCheck.notNull(in_nodes, "in_nodes");

    final AtomicInteger count = new AtomicInteger();
    in_tree.forEachDepthFirst(
      count, (input, depth, node) -> input.incrementAndGet());

    Preconditions.checkPrecondition(
      in_nodes.size() == count.get(),
      String.format(
        "Tree size %d must match node map size %d",
        Integer.valueOf(count.get()),
        Integer.valueOf(in_nodes.size())));
  }

  /**
   * @return The node tree
   */

  public JOTreeNodeType<CaDefinitionJoint> tree()
  {
    return this.tree;
  }

  /**
   * @return The nodes by name
   */

  public Map<CaJointName, JOTreeNodeReadableType<CaDefinitionJoint>> nodes()
  {
    return this.nodes;
  }
}
