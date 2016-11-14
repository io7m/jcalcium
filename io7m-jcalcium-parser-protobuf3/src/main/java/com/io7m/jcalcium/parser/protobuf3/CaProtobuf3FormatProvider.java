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

package com.io7m.jcalcium.parser.protobuf3;

import com.io7m.jcalcium.core.definitions.CaDefinitionSkeletonType;
import com.io7m.jcalcium.core.definitions.CaFormatDescription;
import com.io7m.jcalcium.core.definitions.CaFormatDescriptionType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserFormatProviderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jcalcium.parser.api.CaParseErrorType;
import com.io7m.jcalcium.parser.api.CaParserVersion;
import com.io7m.jcalcium.parser.api.CaParserVersionType;
import com.io7m.jcalcium.parser.protobuf3.v1.CaV1Protobuf3Parsing;
import com.io7m.jfunctional.Unit;
import com.io7m.jlexing.core.ImmutableLexicalPosition;
import com.io7m.jlexing.core.ImmutableLexicalPositionType;
import javaslang.collection.List;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;

import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.valid;

/**
 * A provider for the protobuf3 format.
 */

public final class CaProtobuf3FormatProvider implements
  CaDefinitionParserFormatProviderType
{
  private static final Logger LOG;
  private static final CaFormatDescriptionType FORMAT;

  static {
    LOG = LoggerFactory.getLogger(CaProtobuf3FormatProvider.class);

    {
      final CaFormatDescription.Builder b = CaFormatDescription.builder();
      b.setMimeType("application/vnd.io7m.calcium-protobuf");
      b.setDescription("Protobuf 3 encoded binary skeleton format");
      b.setName("CaP");
      b.setSuffix("cap");
      FORMAT = b.build();
    }
  }

  /**
   * Construct a provider.
   */

  public CaProtobuf3FormatProvider()
  {

  }

  @Override
  public CaFormatDescriptionType format()
  {
    return FORMAT;
  }

  @Override
  public List<CaParserVersionType> versions()
  {
    return CaV1Protobuf3Parsing.supported();
  }

  @Override
  public CaDefinitionParserType create()
  {
    return new DetectingParser();
  }

  private static final class DetectingParser implements CaDefinitionParserType
  {
    private DetectingParser()
    {

    }

    private static Validation<List<CaParseErrorType>, CaParserVersionType> parseVersion(
      final InputStream is,
      final URI uri)
    {
      final byte[] buffer = new byte[2];
      final ByteBuffer wrapper = ByteBuffer.wrap(buffer);
      wrapper.order(ByteOrder.BIG_ENDIAN);

      try {
        final int major;

        {
          final int r = is.read(buffer);
          if (r != 2) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("Failed to read major version number.");
            sb.append(System.lineSeparator());
            sb.append("  Expected: 2 octets");
            sb.append(System.lineSeparator());
            sb.append("  Received: ");
            sb.append(r);
            sb.append(" octets");
            sb.append(System.lineSeparator());
            return invalid(
              List.of(CaParseError.of(position(uri), sb.toString())));
          }

          major = (int) wrapper.getShort(0) & 0xffff;
        }

        final int minor;

        {
          final int r = is.read(buffer);
          if (r != 2) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("Failed to read minor version number.");
            sb.append(System.lineSeparator());
            sb.append("  Expected: 2 octets");
            sb.append(System.lineSeparator());
            sb.append("  Received: ");
            sb.append(r);
            sb.append(" octets");
            sb.append(System.lineSeparator());
            return invalid(
              List.of(CaParseError.of(position(uri), sb.toString())));
          }

          minor = (int) wrapper.getShort(0) & 0xffff;
        }

        if (LOG.isDebugEnabled()) {
          LOG.debug(
            "parsed version {} {}",
            Integer.valueOf(major),
            Integer.valueOf(minor));
        }

        return valid(CaParserVersion.of(major, minor));
      } catch (final IOException e) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Failed to read version number.");
        sb.append(System.lineSeparator());
        sb.append("  I/O error: ");
        sb.append(e.getMessage());
        sb.append(System.lineSeparator());
        return invalid(List.of(CaParseError.of(position(uri), sb.toString())));
      }
    }

    private static ImmutableLexicalPositionType<Path> position(final URI uri)
    {
      return ImmutableLexicalPosition.newPositionWithFile(0, 0, Paths.get(uri));
    }

    private static Validation<List<CaParseErrorType>, Unit> parseMagicNumber(
      final InputStream is,
      final URI uri)
    {
      LOG.debug("attempting to parse magic number");

      try {
        final int x0 = is.read();
        final int x1 = is.read();
        final int x2 = is.read();
        final int x3 = is.read();

        boolean ok = true;
        ok &= x0 == (int) 'C';
        ok &= x1 == (int) 'a';
        ok &= x2 == (int) '\r';
        ok &= x3 == (int) '\n';

        if (!ok) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Magic number incorrect.");
          sb.append(System.lineSeparator());
          sb.append("  Expected: 0x43 0x61 0x0D 0x0A");
          sb.append(System.lineSeparator());
          sb.append("  Received: ");
          sb.append("0x");
          sb.append(Integer.toHexString(x0));
          sb.append("0x");
          sb.append(Integer.toHexString(x1));
          sb.append("0x");
          sb.append(Integer.toHexString(x2));
          sb.append("0x");
          sb.append(Integer.toHexString(x3));
          sb.append(System.lineSeparator());
          return invalid(
            List.of(CaParseError.of(position(uri), sb.toString())));
        }

        if (LOG.isDebugEnabled()) {
          LOG.debug(
            "parsed {} {} {} {}",
            Integer.valueOf(x0),
            Integer.valueOf(x1),
            Integer.valueOf(x2),
            Integer.valueOf(x3));
        }

        return valid(Unit.unit());
      } catch (final IOException e) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Failed to read magic number.");
        sb.append(System.lineSeparator());
        sb.append("  I/O error: ");
        sb.append(e.getMessage());
        sb.append(System.lineSeparator());
        return invalid(List.of(CaParseError.of(position(uri), sb.toString())));
      }
    }

    @Override
    public Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> parseSkeletonFromStream(
      final InputStream is,
      final URI uri)
    {
      return parseMagicNumber(is, uri)
        .flatMap(u -> parseVersion(is, uri)
          .flatMap(version -> this.parserForVersion(version, uri)
            .flatMap(p -> p.parseSkeletonFromStream(is, uri))));
    }

    private Validation<List<CaParseErrorType>, CaDefinitionParserType> parserForVersion(
      final CaParserVersionType version,
      final URI uri)
    {
      for (final CaParserVersionType supported : CaV1Protobuf3Parsing.supported()) {
        if (supported.equals(version)) {
          return valid(new CaV1Protobuf3Parsing());
        }
      }

      final StringBuilder sb = new StringBuilder(128);
      sb.append("Unsupported format version.");
      sb.append(System.lineSeparator());
      sb.append("  Requested: ");
      sb.append(version.major());
      sb.append(".");
      sb.append(version.minor());
      sb.append(System.lineSeparator());
      sb.append("  Supported: ");
      sb.append(System.lineSeparator());

      for (final CaParserVersionType supported : CaV1Protobuf3Parsing.supported()) {
        sb.append("    ");
        sb.append(supported.major());
        sb.append(".");
        sb.append(supported.minor());
        sb.append(System.lineSeparator());
      }

      return invalid(List.of(CaParseError.of(
        position(uri),
        sb.toString()
      )));
    }
  }
}
