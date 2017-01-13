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

package com.io7m.jcalcium.format.protobuf3;

import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.definitions.CaFormatDescription;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.format.protobuf3.v1.CaV1Protobuf3Format;
import com.io7m.jcalcium.loader.api.CaLoaderException;
import com.io7m.jcalcium.loader.api.CaLoaderFormatProviderType;
import com.io7m.jcalcium.loader.api.CaLoaderType;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerFormatProviderType;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerType;
import com.io7m.jnull.NullCheck;
import javaslang.collection.SortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A provider for the protobuf3 format.
 */

public final class CaProtobuf3FormatProvider implements
  CaLoaderFormatProviderType,
  CaCompiledSerializerFormatProviderType
{
  private static final Logger LOG;
  private static final CaFormatDescription FORMAT;
  private static final int[] MAGIC_NUMBER;

  static {
    LOG = LoggerFactory.getLogger(CaProtobuf3FormatProvider.class);

    {
      final CaFormatDescription.Builder b = CaFormatDescription.builder();
      b.setMimeType("application/vnd.io7m.calcium-protobuf");
      b.setDescription("Protobuf 3 encoded compiled skeleton format");
      b.setName("ccp");
      b.setSuffix("ccp");
      FORMAT = b.build();
    }

    {
      MAGIC_NUMBER = new int[8];
      MAGIC_NUMBER[0] = 0x89;
      MAGIC_NUMBER[1] = 'C';
      MAGIC_NUMBER[2] = 'C';
      MAGIC_NUMBER[3] = 'P';
      MAGIC_NUMBER[4] = 0x0D;
      MAGIC_NUMBER[5] = 0x0A;
      MAGIC_NUMBER[6] = 0x1A;
      MAGIC_NUMBER[7] = 0x0A;
    }
  }

  /**
   * Construct a provider.
   */

  public CaProtobuf3FormatProvider()
  {

  }

  /**
   * @return The eight-octet magic number
   */

  public static int[] magicNumber()
  {
    return MAGIC_NUMBER;
  }

  @Override
  public CaFormatDescription loaderFormat()
  {
    return FORMAT;
  }

  @Override
  public SortedSet<CaFormatVersion> loaderSupportedVersions()
  {
    return CaV1Protobuf3Format.supported();
  }

  @Override
  public CaLoaderType loaderCreate()
  {
    return new DetectingLoader();
  }

  @Override
  public CaFormatDescription serializerFormat()
  {
    return FORMAT;
  }

  @Override
  public SortedSet<CaFormatVersion> serializerSupportedVersions()
  {
    return CaV1Protobuf3Format.supported();
  }

  @Override
  public CaCompiledSerializerType serializerCreate(
    final CaFormatVersion v)
    throws UnsupportedOperationException
  {
    for (final CaFormatVersion supported : CaV1Protobuf3Format.supported()) {
      if (supported.major() == v.major() && supported.minor() == v.minor()) {
        return new PrefixingSerializer(v);
      }
    }

    throw new UnsupportedOperationException(
      "The given version is not supported");
  }

  private static final class PrefixingSerializer implements
    CaCompiledSerializerType
  {
    private final CaFormatVersion version;

    PrefixingSerializer(
      final CaFormatVersion v)
    {
      this.version = NullCheck.notNull(v, "Version");
    }

    @Override
    public void serializeCompiledSkeletonToStream(
      final CaSkeleton skeleton,
      final OutputStream out)
      throws IOException
    {
      final byte[] buffer = new byte[4];
      final ByteBuffer wrapper = ByteBuffer.wrap(buffer);
      wrapper.order(ByteOrder.BIG_ENDIAN);

      for (int index = 0; index < MAGIC_NUMBER.length; ++index) {
        out.write(MAGIC_NUMBER[index]);
      }

      wrapper.putInt(0, this.version.major());
      out.write(buffer);
      wrapper.putInt(0, this.version.minor());
      out.write(buffer);

      new CaV1Protobuf3Format().serializeCompiledSkeletonToStream(
        skeleton,
        out);
    }
  }

  private static final class DetectingLoader implements CaLoaderType
  {
    private DetectingLoader()
    {

    }

    private static CaFormatVersion parseVersion(
      final InputStream is)
      throws IOException
    {
      final byte[] buffer = new byte[4];
      final ByteBuffer wrapper = ByteBuffer.wrap(buffer);
      wrapper.order(ByteOrder.BIG_ENDIAN);

      final int major;

      {
        final int r = is.read(buffer);
        if (r != 4) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Failed to read major version number.");
          sb.append(System.lineSeparator());
          sb.append("  Expected: 4 octets");
          sb.append(System.lineSeparator());
          sb.append("  Received: ");
          sb.append(r);
          sb.append(" octets");
          sb.append(System.lineSeparator());
          throw new IOException(sb.toString());
        }

        major = wrapper.getInt(0);
      }

      final int minor;

      {
        final int r = is.read(buffer);
        if (r != 4) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Failed to read minor version number.");
          sb.append(System.lineSeparator());
          sb.append("  Expected: 4 octets");
          sb.append(System.lineSeparator());
          sb.append("  Received: ");
          sb.append(r);
          sb.append(" octets");
          sb.append(System.lineSeparator());
          throw new IOException(sb.toString());
        }

        minor = wrapper.getInt(0);
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug(
          "parsed version {} {}",
          Integer.valueOf(major),
          Integer.valueOf(minor));
      }

      return CaFormatVersion.of(major, minor);
    }

    private static void parseMagicNumber(
      final InputStream is,
      final URI uri)
      throws CaLoaderException, IOException
    {
      LOG.debug("attempting to parse magic number");

      final int[] input = new int[8];
      for (int index = 0; index < MAGIC_NUMBER.length; ++index) {
        input[index] = is.read();
      }

      for (int index = 0; index < MAGIC_NUMBER.length; ++index) {
        final int received = input[index];
        final int expected = MAGIC_NUMBER[index];

        if (received != expected) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Magic number incorrect.");
          sb.append(System.lineSeparator());

          sb.append("  Expected: ");
          for (int x = 0; x < MAGIC_NUMBER.length; ++x) {
            sb.append("0x");
            sb.append(Integer.toHexString(MAGIC_NUMBER[x]));
            sb.append(" ");
          }

          sb.append(System.lineSeparator());
          sb.append("  Received: ");

          for (int x = 0; x < MAGIC_NUMBER.length; ++x) {
            sb.append("0x");
            sb.append(Integer.toHexString(input[x]));
            sb.append(" ");
          }

          sb.append(System.lineSeparator());
          throw new CaLoaderBadMagicNumber(uri, sb.toString());
        }
      }
    }

    private static CaLoaderType loaderForVersion(
      final CaFormatVersion version,
      final URI uri)
      throws CaLoaderUnsupportedVersion
    {
      for (final CaFormatVersion supported : CaV1Protobuf3Format.supported()) {
        if (supported.equals(version)) {
          return new CaV1Protobuf3Format();
        }
      }

      final StringBuilder sb = new StringBuilder(128);
      sb.append("Unsupported format version.");
      sb.append(System.lineSeparator());
      sb.append("  Requested: ");
      sb.append(version.major());
      sb.append("");
      sb.append(version.minor());
      sb.append(System.lineSeparator());
      sb.append("  Supported: ");
      sb.append(System.lineSeparator());

      for (final CaFormatVersion supported : CaV1Protobuf3Format.supported()) {
        sb.append("    ");
        sb.append(supported.major());
        sb.append("");
        sb.append(supported.minor());
        sb.append(System.lineSeparator());
      }

      throw new CaLoaderUnsupportedVersion(uri, sb.toString());
    }

    @Override
    public CaSkeleton loadCompiledSkeletonFromStream(
      final InputStream is,
      final URI uri)
      throws CaLoaderException
    {
      NullCheck.notNull(is, "Input");
      NullCheck.notNull(uri, "URI");

      try {
        parseMagicNumber(is, uri);
        final CaFormatVersion v = parseVersion(is);
        final CaLoaderType loader = loaderForVersion(v, uri);
        return loader.loadCompiledSkeletonFromStream(is, uri);
      } catch (final IOException e) {
        throw new CaLoaderIOException(uri, e);
      }
    }
  }
}
