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

package com.io7m.jcalcium.mesh.meta;

import com.google.protobuf.ByteString;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaSkeletonHash;
import com.io7m.jcalcium.core.compiled.CaSkeletonMetadata;
import com.io7m.jcalcium.mesh.meta.v1.Meta;
import com.io7m.jintegers.Unsigned32;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Functions over mesh metadata.
 */

public final class CaMeshMetas
{
  /**
   * The vendor ID used by the <tt>jcalcium</tt> package.
   */

  public static final int VENDOR_ID = 0x494F374D;

  /**
   * The product ID used by the <tt>jcalcium</tt> package.
   */

  public static final int PRODUCT_ID = 0x63610000;

  private CaMeshMetas()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Serialize the given metadata using the data format for major version {@code
   * version_major} and minor version {@code version_minor}.
   *
   * @param meta          The metadata
   * @param version_major The major version
   * @param version_minor The minor version
   *
   * @return Serialized metadata
   */

  public static byte[] serialize(
    final CaSkeletonMetadata meta,
    final int version_major,
    final int version_minor)
  {
    NullCheck.notNull(meta, "Metadata");

    switch (version_major) {
      case 1: {
        return serializeV1(meta, version_major, version_minor);
      }
      default: {
        throw new UnsupportedOperationException(
          "Unsupported major version: " + version_major);
      }
    }
  }

  private static byte[] serializeV1(
    final CaSkeletonMetadata meta,
    final long version_major,
    final long version_minor)
  {
    try {
      final byte[] byte4 = new byte[4];
      final ByteBuffer buffer4 = ByteBuffer.wrap(byte4);
      buffer4.order(ByteOrder.BIG_ENDIAN);

      try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Unsigned32.packToBuffer(version_major, buffer4, 0);
        bos.write(byte4);
        Unsigned32.packToBuffer(version_minor, buffer4, 0);
        bos.write(byte4);

        final Meta.V1SkeletonMeta.Builder b = Meta.V1SkeletonMeta.newBuilder();
        final CaSkeletonName name = meta.name();
        b.setName(name.value());
        final CaSkeletonHash hash = meta.hash();
        b.setHashAlgorithm(hash.algorithm());
        b.setHashValue(ByteString.copyFrom(Hex.decodeHex(hash.value().toCharArray())));
        final byte[] serialized = b.build().toByteArray();

        Unsigned32.packToBuffer((long) serialized.length, buffer4, 0);
        bos.write(byte4);
        bos.write(serialized);
        return bos.toByteArray();
      }
    } catch (final IOException | DecoderException e) {
      // Byte array streams do not actually do I/O
      // Preconditions on the skeleton hash type make this impossible
      throw new UnreachableCodeException(e);
    }
  }

  /**
   * Deserialize metadata from given bytes.
   *
   * @param data Serialized metadata
   *
   * @return Skeleton metadata
   */

  public static CaSkeletonMetadata deserialize(
    final byte[] data)
  {
    final ByteBuffer wrap = ByteBuffer.wrap(data);
    wrap.order(ByteOrder.BIG_ENDIAN);

    if (data.length >= 12) {
      final int version_major =
        (int) Unsigned32.unpackFromBuffer(wrap, 0);
      final int version_minor =
        (int) Unsigned32.unpackFromBuffer(wrap, 4);
      final int size =
        (int) Unsigned32.unpackFromBuffer(wrap, 8);

      switch (version_major) {
        case 1: {
          return deserializeV1(data, size);
        }
        default: {
          throw new UnsupportedOperationException(
            "Unsupported major version: " + version_major);
        }
      }
    }

    final StringBuffer sb = new StringBuffer(128);
    sb.append("Could not deserialize skeleton metadata.");
    sb.append(System.lineSeparator());
    sb.append("  Expected: at least 12 octets.");
    sb.append(System.lineSeparator());
    sb.append("  Received: ");
    sb.append(data.length);
    sb.append(" octets");
    sb.append(System.lineSeparator());
    throw new IllegalArgumentException(sb.toString());
  }

  private static CaSkeletonMetadata deserializeV1(
    final byte[] data,
    final int size)
  {
    try (final InputStream stream = new ByteArrayInputStream(data, 12, size)) {
      final Meta.V1SkeletonMeta v1 =
        Meta.V1SkeletonMeta.parseFrom(stream);

      final CaSkeletonName name =
        CaSkeletonName.of(v1.getName());


      final String hash_value =
        Hex.encodeHexString(v1.getHashValue().toByteArray());
      final String hash_algo =
        v1.getHashAlgorithm();

      return CaSkeletonMetadata.of(
        name, CaSkeletonHash.of(hash_algo, hash_value));

    } catch (final IOException e) {
      // Byte array streams do not actually do I/O
      throw new UnreachableCodeException(e);
    }
  }
}
