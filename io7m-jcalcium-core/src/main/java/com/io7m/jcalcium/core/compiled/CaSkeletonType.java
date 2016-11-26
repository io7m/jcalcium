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

package com.io7m.jcalcium.core.compiled;

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import javaslang.collection.SortedMap;

/**
 * The type of compiled skeletons.
 */

public interface CaSkeletonType
{
  /**
   * @return The tree of bones for the skeleton
   */

  JOTreeNodeReadableType<CaBone> bones();

  /**
   * @return The actions by name
   */

  SortedMap<CaActionName, CaActionType> actionsByName();

  /**
   * @return A map of bone nodes by name
   */

  SortedMap<CaBoneName, JOTreeNodeReadableType<CaBone>> bonesByName();

  /**
   * @return A map of bone nodes by ID
   */

  SortedMap<Integer, JOTreeNodeReadableType<CaBone>> bonesByID();
}
