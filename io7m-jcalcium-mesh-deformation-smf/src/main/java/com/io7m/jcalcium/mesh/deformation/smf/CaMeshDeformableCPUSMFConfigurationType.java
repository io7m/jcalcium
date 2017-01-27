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

package com.io7m.jcalcium.mesh.deformation.smf;

import com.io7m.jcalcium.core.CaImmutableStyleType;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeSourceSelection;
import com.io7m.smfj.core.SMFAttributeName;
import javaslang.collection.Seq;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
import org.immutables.javaslang.encodings.JavaslangEncodingEnabled;
import org.immutables.value.Value;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Configuration values for loading SMF-backed meshes.
 */

@Value.Immutable
@JavaslangEncodingEnabled
@CaImmutableStyleType
public interface CaMeshDeformableCPUSMFConfigurationType
{
  /**
   * <p>A sequence of attributes that will be used as the source data for
   * deformation of the mesh. It an error to provide two or more attributes with
   * the same name.</p>
   *
   * @return The set of attributes that will be subject to deformation
   */

  @Value.Parameter
  Seq<CaMeshDeformableAttributeSourceSelection> sourceAttributes();

  /**
   * <p>A predicate that will be evaluated for each auxiliary attribute that
   * may be loaded. If the predicate returns {@code true}, the attribute will
   * be included. Otherwise, the attribute will not be included.</p>
   *
   * <p>By default, a predicate that will accept all attributes is provided.</p>
   *
   * @return A predicate evaluated for each auxiliary attribute
   */

  @Value.Parameter
  @Value.Default
  default Predicate<SMFAttributeName> auxiliarySelector()
  {
    return name -> true;
  }

  /**
   * @return The set of attributes that will be subject to deformation, by name
   */

  @Value.Derived
  default SortedMap<String, CaMeshDeformableAttributeSourceSelection> sourceAttributesByName()
  {
    return TreeMap.ofAll(this.sourceAttributes().toJavaStream().collect(
      Collectors.toMap(
        CaMeshDeformableAttributeSourceSelection::name,
        Function.identity(),
        (a0, a1) -> {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Duplicate attribute name.");
          sb.append(System.lineSeparator());
          sb.append("  Name: ");
          sb.append(a0.name());
          sb.append(System.lineSeparator());
          throw new IllegalArgumentException(sb.toString());
        })));
  }

}
