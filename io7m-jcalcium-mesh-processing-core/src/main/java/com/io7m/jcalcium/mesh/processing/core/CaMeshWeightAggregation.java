/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
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

package com.io7m.jcalcium.mesh.processing.core;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.VectorI4D;
import com.io7m.jtensors.VectorI4L;
import com.io7m.junreachable.UnimplementedCodeException;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Seq;
import javaslang.collection.Set;
import javaslang.collection.SortedMap;
import javaslang.collection.Vector;

import java.util.Comparator;

/**
 * Functions to aggregate weights in a mesh.
 */

public final class CaMeshWeightAggregation
{
  private CaMeshWeightAggregation()
  {

  }

  /**
   * Given a list of vertex weights for each joint, pick the four largest
   * weights for each vertex and record the joint IDs for each weight.
   *
   * @param skeleton The skeleton
   * @param weights  The per-joint weights
   *
   * @return A list of packed weights
   */

  public static CaMeshWeightsAggregated aggregateWeights(
    final CaSkeleton skeleton,
    final SortedMap<CaJointName, Vector<Double>> weights)
  {
    NullCheck.notNull(skeleton, "Skeleton");
    NullCheck.notNull(weights, "Weights");

    Preconditions.checkPreconditionI(
      weights.size(),
      weights.size() > 0,
      x -> "Must provide at least one weight array");

    weights.forEach(
      p -> Preconditions.checkPrecondition(
        p._1,
        skeleton.jointsByName().containsKey(p._1),
        j -> "Skeleton must contain the named joint"));

    final Set<Integer> vertex_counts =
      weights.values().map(vs -> Integer.valueOf(vs.size())).toSet();

    Preconditions.checkPrecondition(
      vertex_counts.size() == 1, "Weight arrays must all be the same length");

    final int vertex_count = vertex_counts.iterator().get().intValue();

    final SortedMap<CaJointName, Integer> joint_ids =
      skeleton.jointsByName().mapValues(
        node -> Integer.valueOf(node.value().id()));

    Vector<VectorI4L> result_indices = Vector.empty();
    Vector<VectorI4D> result_weights = Vector.empty();

    for (int vertex_index = 0; vertex_index < vertex_count; ++vertex_index) {
      final int vi = vertex_index;

      final List<Tuple2<Integer, Double>> weighted_indices =
        weights
          .map((joint, array) -> jointLookup(joint, array, joint_ids, vi))
          .toList()
          .sorted(compareWeightedIndex())
          .take(4);

      final Tuple2<VectorI4L, VectorI4D> packed = packWeights(weighted_indices);
      result_indices = result_indices.append(packed._1);
      result_weights = result_weights.append(packed._2);
    }

    return CaMeshWeightsAggregated.of(result_weights, result_indices);
  }

  private static Tuple2<Integer, Double> jointLookup(
    final CaJointName joint_name,
    final Seq<Double> array,
    final Map<CaJointName, Integer> names_to_ids,
    final int f_index)
  {
    return Tuple.of(names_to_ids.get(joint_name).get(), array.get(f_index));
  }

  private static Tuple2<VectorI4L, VectorI4D> packWeights(
    final Seq<Tuple2<Integer, Double>> weighted_indices)
  {
    final VectorI4L index4;
    final VectorI4D weight4;

    switch (weighted_indices.size()) {
      case 1: {
        index4 = new VectorI4L(
          weighted_indices.get(0)._1.longValue(),
          0L,
          0L,
          0L);
        weight4 = new VectorI4D(
          weighted_indices.get(0)._2.doubleValue(),
          0.0,
          0.0,
          0.0);
        return Tuple.of(index4, VectorI4D.normalize(weight4));
      }

      case 2: {
        index4 = new VectorI4L(
          weighted_indices.get(0)._1.longValue(),
          weighted_indices.get(1)._1.longValue(),
          0L,
          0L);
        weight4 = new VectorI4D(
          weighted_indices.get(0)._2.doubleValue(),
          weighted_indices.get(1)._2.doubleValue(),
          0.0,
          0.0);
        return Tuple.of(index4, VectorI4D.normalize(weight4));
      }

      case 3: {
        index4 = new VectorI4L(
          weighted_indices.get(0)._1.longValue(),
          weighted_indices.get(1)._1.longValue(),
          weighted_indices.get(2)._1.longValue(),
          0L);
        weight4 = new VectorI4D(
          weighted_indices.get(0)._2.doubleValue(),
          weighted_indices.get(1)._2.doubleValue(),
          weighted_indices.get(2)._2.doubleValue(),
          0.0);
        return Tuple.of(index4, VectorI4D.normalize(weight4));
      }

      case 4: {
        index4 = new VectorI4L(
          weighted_indices.get(0)._1.longValue(),
          weighted_indices.get(1)._1.longValue(),
          weighted_indices.get(2)._1.longValue(),
          weighted_indices.get(3)._1.longValue());
        weight4 = new VectorI4D(
          weighted_indices.get(0)._2.doubleValue(),
          weighted_indices.get(1)._2.doubleValue(),
          weighted_indices.get(2)._2.doubleValue(),
          weighted_indices.get(3)._2.doubleValue());
        return Tuple.of(index4, VectorI4D.normalize(weight4));
      }

      default: {
        throw new UnimplementedCodeException();
      }
    }
  }

  private static Comparator<Tuple2<Integer, Double>> compareWeightedIndex()
  {
    return (x, y) -> Double.compare(y._2.doubleValue(), x._2.doubleValue());
  }
}
