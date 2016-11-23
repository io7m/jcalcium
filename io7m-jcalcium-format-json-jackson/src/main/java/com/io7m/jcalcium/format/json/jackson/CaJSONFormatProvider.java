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

package com.io7m.jcalcium.format.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeletonType;
import com.io7m.jcalcium.core.definitions.CaFormatDescription;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.core.definitions.CaFormatVersionType;
import com.io7m.jcalcium.format.json.jackson.v1.CaV1JSONFormat;
import com.io7m.jcalcium.parser.api.CaDefinitionParserFormatProviderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jcalcium.serializer.api.CaDefinitionSerializerFormatProviderType;
import com.io7m.jcalcium.serializer.api.CaDefinitionSerializerType;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import javaslang.collection.List;
import javaslang.collection.SortedSet;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * A format provider for the JSON format.
 */

public final class CaJSONFormatProvider implements
  CaDefinitionParserFormatProviderType,
  CaDefinitionSerializerFormatProviderType
{
  private static final Logger LOG;
  private static final CaFormatDescription FORMAT;

  static {
    LOG = LoggerFactory.getLogger(CaJSONFormatProvider.class);

    {
      final CaFormatDescription.Builder b = CaFormatDescription.builder();
      b.setMimeType("application/vnd.io7m.calcium-json");
      b.setDescription("JSON encoded skeleton format");
      b.setName("CaJ");
      b.setSuffix("caj");
      FORMAT = b.build();
    }
  }

  /**
   * Construct a provider.
   */

  public CaJSONFormatProvider()
  {

  }

  @Override
  public CaFormatDescription parserFormat()
  {
    return FORMAT;
  }

  @Override
  public SortedSet<CaFormatVersion> parserSupportedVersions()
  {
    return CaV1JSONFormat.supported();
  }

  @Override
  public CaDefinitionParserType parserCreate()
  {
    return new DetectingParser();
  }

  @Override
  public CaFormatDescription serializerFormat()
  {
    return FORMAT;
  }

  @Override
  public SortedSet<CaFormatVersion> serializerSupportedVersions()
  {
    return CaV1JSONFormat.supported();
  }

  @Override
  public CaDefinitionSerializerType serializerCreate(
    final CaFormatVersion v)
    throws UnsupportedOperationException
  {
    return new Serializer(v);
  }

  private static final class SkeletonDeserializer
    extends StdDeserializer<CaDefinitionSkeletonType>
  {
    private static final Logger LOG;

    static {
      LOG = LoggerFactory.getLogger(SkeletonDeserializer.class);
    }

    SkeletonDeserializer()
    {
      super(CaDefinitionSkeletonType.class);
    }

    @Override
    public CaDefinitionSkeletonType deserialize(
      final JsonParser p,
      final DeserializationContext ctxt)
      throws IOException, JsonProcessingException
    {
      LOG.debug("deserializing");

      final String fv = p.nextFieldName();
      if ("version".equals(fv)) {
        final String v = p.nextTextValue();
        if (v.startsWith("calcium skeleton 1.")) {
          LOG.debug("instantiating parser for version {} file", v);
          final String sn = p.nextFieldName();
          if ("skeleton".equals(sn)) {
            p.nextToken();
            final CaV1JSONFormat.CaV1Skeleton sk =
              p.readValueAs(CaV1JSONFormat.CaV1Skeleton.class);
            return sk.toSkeleton();
          }
          throw ctxt.mappingException(
            String.format("Expected a skeleton field (received %s)", sn));
        }
        throw ctxt.mappingException("Unsupported version: " + v);
      }
      throw ctxt.mappingException(
        String.format("Expected a version field (received %s)", fv));
    }
  }

  private static final class SkeletonSerializer extends StdSerializer<CaDefinitionSkeletonType>
  {
    private static final Logger LOG;

    static {
      LOG = LoggerFactory.getLogger(SkeletonDeserializer.class);
    }

    private final CaFormatVersionType version;

    SkeletonSerializer(
      final CaFormatVersionType in_version)
    {
      super(CaDefinitionSkeletonType.class);
      this.version = NullCheck.notNull(in_version, "Version");
    }

    @Override
    public void serialize(
      final CaDefinitionSkeletonType value,
      final JsonGenerator gen,
      final SerializerProvider provider)
      throws IOException
    {
      gen.writeStartObject();
      gen.writeStringField(
        "version",
        String.format(
          "calcium skeleton %d.%d",
          Integer.valueOf(this.version.major()),
          Integer.valueOf(this.version.minor())));
      gen.writeObjectField(
        "skeleton", CaV1JSONFormat.CaV1Skeleton.fromCore(value));
      gen.writeEndObject();
    }
  }

  private static final class DetectingParser implements CaDefinitionParserType
  {
    private final ObjectMapper mapper;
    private final CaV1JSONFormat v1;

    DetectingParser()
    {
      final SimpleModule m = new SimpleModule();
      m.addDeserializer(
        CaDefinitionSkeletonType.class, new SkeletonDeserializer());

      this.mapper = CaJSON.createMapper();
      this.mapper.registerModule(m);
      this.v1 = new CaV1JSONFormat(this.mapper);
    }

    @Override
    public Validation<List<CaParseError>, CaDefinitionSkeletonType> parseSkeletonFromStream(
      final InputStream is,
      final URI uri)
    {
      NullCheck.notNull(is, "Input stream");
      NullCheck.notNull(uri, "URI");

      try {
        return Validation.valid(
          this.mapper.readValue(is, CaDefinitionSkeletonType.class));
      } catch (final JsonMappingException e) {
        final JsonLocation loc = e.getLocation();
        final javaslang.collection.List<CaParseError> xs =
          javaslang.collection.List.of(
            CaParseError.of(
              LexicalPosition.of(
                loc.getLineNr(),
                loc.getColumnNr(),
                Optional.of(Paths.get(uri))),
              e.getMessage()
            ));
        return Validation.invalid(xs);
      } catch (final JsonParseException e) {
        final JsonLocation loc = e.getLocation();
        final javaslang.collection.List<CaParseError> xs =
          javaslang.collection.List.of(
            CaParseError.of(
              LexicalPosition.of(
                loc.getLineNr(),
                loc.getColumnNr(),
                Optional.of(Paths.get(uri))),
              e.getMessage()
            ));
        return Validation.invalid(xs);
      } catch (final IOException e) {
        final javaslang.collection.List<CaParseError> xs =
          javaslang.collection.List.of(
            CaParseError.of(
              LexicalPosition.of(
                -1,
                -1,
                Optional.of(Paths.get(uri))),
              e.getMessage()
            ));
        return Validation.invalid(xs);
      }
    }
  }

  private static final class Serializer implements CaDefinitionSerializerType
  {
    private final ObjectMapper mapper;
    private final CaV1JSONFormat v1;

    Serializer(final CaFormatVersionType v)
    {
      final SimpleModule m = new SimpleModule();
      m.addSerializer(
        CaDefinitionSkeletonType.class,
        new SkeletonSerializer(v));

      this.mapper = CaJSON.createMapper();
      this.mapper.registerModule(m);
      this.v1 = new CaV1JSONFormat(this.mapper);
    }

    @Override
    public void serializeSkeletonToStream(
      final CaDefinitionSkeletonType skeleton,
      final OutputStream out)
      throws IOException
    {
      this.mapper.writeValue(out, skeleton);
    }
  }
}

