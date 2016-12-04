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

package com.io7m.jcalcium.format.protobuf3.v1;

import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.loader.api.CaLoaderException;
import com.io7m.jcalcium.loader.api.CaLoaderType;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerType;
import javaslang.collection.SortedSet;
import javaslang.collection.TreeSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * An implementation of a protobuf3 parser for version 1 types.
 */

public final class CaV1Protobuf3Format
  implements CaLoaderType, CaCompiledSerializerType
{
  /**
   * Construct a parser
   */

  public CaV1Protobuf3Format()
  {

  }

  /**
   * @return The parserSupportedVersions supported by this parser
   */

  public static SortedSet<CaFormatVersion> supported()
  {
    return TreeSet.of(CaFormatVersion.of(1, 0));
  }

  @Override
  public void serializeCompiledSkeletonToStream(
    final CaSkeleton skeleton,
    final OutputStream out)
    throws IOException
  {
    new CaV1Serializer().serializeCompiledSkeletonToStream(skeleton, out);
  }

  @Override
  public CaSkeleton loadCompiledSkeletonFromStream(
    final InputStream is,
    final URI uri)
    throws CaLoaderException
  {
    return new CaV1Loader(uri, is).run();
  }
}
