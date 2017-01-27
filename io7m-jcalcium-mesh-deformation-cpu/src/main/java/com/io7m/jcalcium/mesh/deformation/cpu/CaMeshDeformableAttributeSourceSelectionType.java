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

package com.io7m.jcalcium.mesh.deformation.cpu;

import com.io7m.jcalcium.core.CaImmutableStyleType;
import org.immutables.value.Value;

/**
 * A selection of an attribute that should appear in the source buffer
 * of a given mesh. That is, an attribute that will be subject to deformation.
 */

@Value.Immutable
@CaImmutableStyleType
public interface CaMeshDeformableAttributeSourceSelectionType
{
  /**
   * @return The attribute name
   */

  @Value.Parameter
  String name();

  /**
   * @return The attribute semantic
   */

  @Value.Parameter
  CaMeshDeformableAttributeSemantic semantic();

  /**
   * @return The attribute kind
   */

  @Value.Parameter
  CaMeshDeformableAttributeCursorKind kind();
}
