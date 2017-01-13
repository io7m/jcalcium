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

package com.io7m.jcalcium.evaluator.api;

import com.io7m.jcalcium.core.compiled.CaSkeletonRestPoseDType;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;

/**
 * The type of readable evaluated skeletons with double-precision components.
 */

public interface CaEvaluatedSkeletonReadableDType extends
  CaEvaluatedSkeletonReadableType
{
  /**
   * @return The tree of evaluated joints
   */

  JOTreeNodeReadableType<CaEvaluatedJointReadableDType> joints();

  /**
   * A read-only view of the evaluated joints organized by ID.
   *
   * @return The set of evaluated joints by ID
   */

  Int2ReferenceSortedMap<CaEvaluatedJointReadableDType> jointsByID();

  /**
   * @return The skeleton's rest pose
   */

  CaSkeletonRestPoseDType restPose();
}
