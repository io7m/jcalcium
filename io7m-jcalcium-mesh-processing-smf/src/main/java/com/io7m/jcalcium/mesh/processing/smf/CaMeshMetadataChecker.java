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

package com.io7m.jcalcium.mesh.processing.smf;

import com.io7m.jcalcium.core.compiled.CaSkeletonMetadata;
import com.io7m.jcalcium.mesh.meta.CaMeshMetas;
import com.io7m.jnull.NullCheck;
import com.io7m.smfj.parser.api.SMFParserEventsMetaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A metadata checker that rejects meshes that do not have the expected name and
 * hash.
 */

public final class CaMeshMetadataChecker implements SMFParserEventsMetaType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaMeshMetadataChecker.class);
  }

  private final CaSkeletonMetadata expected;

  private CaMeshMetadataChecker(
    final CaSkeletonMetadata in_expected)
  {
    this.expected = NullCheck.notNull(in_expected, "Expected");
  }

  /**
   * Create a new metadata checker that rejects meshes that do not have the
   * expected name and hash.
   *
   * @param in_expected The expected metadata
   *
   * @return A new metadata checker
   */

  public static SMFParserEventsMetaType create(
    final CaSkeletonMetadata in_expected)
  {
    return new CaMeshMetadataChecker(in_expected);
  }

  @Override
  public boolean onMeta(
    final long vendor,
    final long schema,
    final long length)
  {
    return vendor == Integer.toUnsignedLong(CaMeshMetas.VENDOR_ID)
      && schema == Integer.toUnsignedLong(CaMeshMetas.PRODUCT_ID);
  }

  @Override
  public void onMetaData(
    final long vendor,
    final long schema,
    final byte[] data)
  {
    final CaSkeletonMetadata received = CaMeshMetas.deserialize(data);

    if (LOG.isDebugEnabled()) {
      LOG.debug(
        "mesh skeleton name: {}",
        received.name().value());
      LOG.debug(
        "mesh skeleton hash: {} {}",
        received.hash().algorithm(),
        received.hash().value());
    }

    if (!Objects.equals(received, this.expected)) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Mesh is incompatible with skeleton.");
      sb.append(System.lineSeparator());
      sb.append("  Skeleton name: ");
      sb.append(this.expected.name().value());
      sb.append(System.lineSeparator());
      sb.append("  Skeleton hash: ");
      sb.append(this.expected.hash().algorithm());
      sb.append(" ");
      sb.append(this.expected.hash().value());
      sb.append(System.lineSeparator());
      sb.append("  Mesh skeleton name: ");
      sb.append(received.name().value());
      sb.append(System.lineSeparator());
      sb.append("  Mesh skeleton hash: ");
      sb.append(received.hash().algorithm());
      sb.append(" ");
      sb.append(received.hash().value());
      sb.append(System.lineSeparator());
      throw new IllegalArgumentException(sb.toString());
    }
  }
}
