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

package com.io7m.jcalcium.core.definitions;

import com.io7m.jcalcium.core.CaImmutableStyleType;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * A joint definition for a skeleton.
 */

@CaImmutableStyleType
@Value.Immutable
public interface CaDefinitionJointType
{
  /**
   * @return The joint name
   */

  @Value.Parameter
  CaJointName name();

  /**
   * @return The name of the joint's parent
   */

  @Value.Parameter
  Optional<CaJointName> parent();

  /**
   * @return The parent-relative offset for the joint
   */

  @Value.Parameter
  PVectorI3D<CaSpaceJointType> translation();

  /**
   * @return The parent-relative orientation of the joint
   */

  @Value.Parameter
  QuaternionI4D orientation();

  /**
   * @return The parent-relative scale of the joint
   */

  @Value.Parameter
  VectorI3D scale();
}
