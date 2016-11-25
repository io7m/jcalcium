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

import com.io7m.jcalcium.core.CaBoneName;
import com.io7m.jcalcium.core.ImmutableStyleType;
import com.io7m.jcalcium.core.spaces.CaSpaceBoneParentRelativeType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import org.immutables.value.Value;

/**
 * The type of compiled bones.
 */

@Value.Immutable
@ImmutableStyleType
public interface CaCompiledBoneType
{
  /**
   * @return The bone name
   */

  @Value.Parameter
  CaBoneName name();

  /**
   * @return The bone ID
   */

  @Value.Parameter
  int id();

  /**
   * @return The parent-relative offset for the bone
   */

  @Value.Parameter
  PVectorI3D<CaSpaceBoneParentRelativeType> translation();

  /**
   * @return The parent-relative orientation of the bone
   */

  @Value.Parameter
  QuaternionI4D orientation();

  /**
   * @return The parent-relative scale of the bone
   */

  @Value.Parameter
  VectorI3D scale();
}
