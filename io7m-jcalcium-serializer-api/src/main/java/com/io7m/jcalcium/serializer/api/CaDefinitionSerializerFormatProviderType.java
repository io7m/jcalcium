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

package com.io7m.jcalcium.serializer.api;

import com.io7m.jcalcium.core.definitions.CaFormatDescription;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import javaslang.collection.SortedSet;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A provider of definition serializers for a given format.
 */

@ProviderType
public interface CaDefinitionSerializerFormatProviderType
{
  /**
   * @return The format that this provider supports
   */

  CaFormatDescription serializerFormat();

  /**
   * @return The supported versions of the format
   */

  SortedSet<CaFormatVersion> serializerSupportedVersions();

  /**
   * Create a new serializer for the given format version.
   *
   * @param v The version
   *
   * @return A new serializer for the format
   *
   * @throws UnsupportedOperationException If the given version is not one of
   *                                       the versions returned by {@link
   *                                       #serializerSupportedVersions()}
   */

  CaDefinitionSerializerType serializerCreate(
    CaFormatVersion v)
    throws UnsupportedOperationException;
}
