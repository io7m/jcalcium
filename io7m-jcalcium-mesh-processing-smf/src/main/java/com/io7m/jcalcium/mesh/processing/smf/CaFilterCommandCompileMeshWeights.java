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

import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.loader.api.CaLoaderException;
import com.io7m.jcalcium.loader.api.CaLoaderFormatProviderType;
import com.io7m.jcalcium.loader.api.CaLoaderType;
import com.io7m.jcalcium.mesh.meta.CaMeshMetas;
import com.io7m.jcalcium.mesh.processing.core.CaMeshWeightAggregation;
import com.io7m.jcalcium.mesh.processing.core.CaMeshWeightsAggregated;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import com.io7m.smfj.core.SMFAttribute;
import com.io7m.smfj.core.SMFAttributeName;
import com.io7m.smfj.core.SMFHeader;
import com.io7m.smfj.parser.api.SMFParseError;
import com.io7m.smfj.processing.api.SMFAttributeArrayFloating1;
import com.io7m.smfj.processing.api.SMFAttributeArrayFloating4;
import com.io7m.smfj.processing.api.SMFAttributeArrayIntegerUnsigned4;
import com.io7m.smfj.processing.api.SMFAttributeArrayType;
import com.io7m.smfj.processing.api.SMFFilterCommandContext;
import com.io7m.smfj.processing.api.SMFFilterCommandParsing;
import com.io7m.smfj.processing.api.SMFMemoryMesh;
import com.io7m.smfj.processing.api.SMFMemoryMeshFilterType;
import com.io7m.smfj.processing.api.SMFMetadata;
import com.io7m.smfj.processing.api.SMFProcessingError;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
import javaslang.collection.Vector;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.io7m.smfj.core.SMFComponentType.ELEMENT_TYPE_FLOATING;
import static com.io7m.smfj.core.SMFComponentType.ELEMENT_TYPE_INTEGER_UNSIGNED;
import static java.util.Optional.empty;

/**
 * A filter command that aggregates weights for a mesh.
 *
 * @see CaMeshWeightAggregation
 */

public final class CaFilterCommandCompileMeshWeights implements
  SMFMemoryMeshFilterType
{
  /**
   * The command name.
   */

  public static final String NAME;

  private static final String SYNTAX;
  private static final Logger LOG;
  private static final String GROUP_ATTRIBUTE_PREFIX = "GROUP:";

  static {
    LOG = LoggerFactory.getLogger(CaFilterCommandCompileMeshWeights.class);
    NAME = "compile-mesh-weights";
    SYNTAX = "<skeleton> <indices-attribute> <weights-attribute> ('meta' | 'no-meta') <pattern>";
  }

  private final SMFAttributeName attr_name_indices;
  private final SMFAttributeName attr_name_weights;
  private final Pattern source_pattern;
  private final Path skeleton_file;
  private final AddMetadata meta;

  private CaFilterCommandCompileMeshWeights(
    final Path in_skeleton,
    final SMFAttributeName in_attr_name_indices,
    final SMFAttributeName in_attr_name_weights,
    final AddMetadata in_meta,
    final Pattern in_source_pattern)
  {
    this.skeleton_file =
      NullCheck.notNull(in_skeleton, "Skeleton");
    this.attr_name_indices =
      NullCheck.notNull(in_attr_name_indices, "Indices");
    this.attr_name_weights =
      NullCheck.notNull(in_attr_name_weights, "Weights");
    this.meta =
      NullCheck.notNull(in_meta, "AddMetadata");
    this.source_pattern =
      NullCheck.notNull(in_source_pattern, "Source pattern");
  }

  /**
   * Create a new filter.
   *
   * @param in_skeleton          The path to the skeleton file
   * @param in_attr_name_indices The name of the attribute that will contain
   *                             joint indices
   * @param in_attr_name_weights The name of the attribute that will contain
   *                             joint weights
   * @param in_source_pattern    A regular expression used to match input
   *                             attribute names
   * @param in_meta              Whether or not metadata should be introduced
   *                             into the mesh
   *
   * @return A new filter
   */

  public static SMFMemoryMeshFilterType create(
    final Path in_skeleton,
    final SMFAttributeName in_attr_name_indices,
    final SMFAttributeName in_attr_name_weights,
    final AddMetadata in_meta,
    final Pattern in_source_pattern)
  {
    return new CaFilterCommandCompileMeshWeights(
      in_skeleton,
      in_attr_name_indices,
      in_attr_name_weights,
      in_meta,
      in_source_pattern);
  }

  private static String makeSyntax()
  {
    return NAME + " " + SYNTAX;
  }

  /**
   * Attempt to parse a command.
   *
   * @param file The file, if any
   * @param line The line
   * @param text The text
   *
   * @return A parsed command or a list of parse errors
   */

  public static Validation<List<SMFParseError>, SMFMemoryMeshFilterType> parse(
    final Optional<Path> file,
    final int line,
    final List<String> text)
  {
    NullCheck.notNull(file, "file");
    NullCheck.notNull(text, "text");

    if (text.length() == 5) {
      try {
        final Path skeleton_file =
          Paths.get(text.get(0));
        final SMFAttributeName attr_indices =
          SMFAttributeName.of(text.get(1));
        final SMFAttributeName attr_weights =
          SMFAttributeName.of(text.get(2));
        final AddMetadata meta =
          AddMetadata.of(text.get(3));
        final Pattern pattern =
          Pattern.compile(text.get(4));

        LOG.debug("skeleton file:     {}", skeleton_file);
        LOG.debug("attribute indices: {}", attr_indices.value());
        LOG.debug("attribute weights: {}", attr_weights.value());
        LOG.debug("meta:              {}", meta);
        LOG.debug("pattern:           {}", pattern.pattern());

        return Validation.valid(new CaFilterCommandCompileMeshWeights(
          skeleton_file, attr_indices, attr_weights, meta, pattern));
      } catch (final PatternSyntaxException e) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Incorrect command syntax.");
        sb.append(System.lineSeparator());
        sb.append("  Bad regular expression: ");
        sb.append(System.lineSeparator());
        sb.append(e.getMessage());
        sb.append(System.lineSeparator());
        sb.append("  Expected: ");
        sb.append(makeSyntax());
        sb.append(System.lineSeparator());
        sb.append("  Received: ");
        sb.append(text.toJavaStream().collect(Collectors.joining(" ")));
        sb.append(System.lineSeparator());
        return Validation.invalid(List.of(SMFParseError.of(
          LexicalPosition.of(line, 0, file), sb.toString(), empty())));
      } catch (final IllegalArgumentException e) {
        return SMFFilterCommandParsing.errorExpectedGotValidation(
          file, line, makeSyntax(), text);
      }
    }
    return SMFFilterCommandParsing.errorExpectedGotValidation(
      file, line, makeSyntax(), text);
  }

  private static CaLoaderType findLoader()
  {
    final ServiceLoader<CaLoaderFormatProviderType> service_loader =
      ServiceLoader.load(CaLoaderFormatProviderType.class);
    final Iterator<CaLoaderFormatProviderType> providers =
      service_loader.iterator();

    if (providers.hasNext()) {
      final CaLoaderFormatProviderType provider = providers.next();
      return provider.loaderCreate();
    }

    throw new UnsupportedOperationException("No available skeleton loader");
  }

  @Override
  public String name()
  {
    return NAME;
  }

  @Override
  public String syntax()
  {
    return makeSyntax();
  }

  @Override
  public Validation<List<SMFProcessingError>, SMFMemoryMesh> filter(
    final SMFFilterCommandContext context,
    final SMFMemoryMesh mesh)
  {
    NullCheck.notNull(context, "context");
    NullCheck.notNull(mesh, "mesh");

    List<SMFProcessingError> errors = List.empty();

    final Map<SMFAttributeName, SMFAttributeArrayType> arrays = mesh.arrays();
    if (arrays.containsKey(this.attr_name_indices)) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Output attribute already exists.");
      sb.append(System.lineSeparator());
      sb.append("  Attribute: ");
      sb.append(this.attr_name_indices.value());
      sb.append(System.lineSeparator());
      errors = errors.append(SMFProcessingError.of(sb.toString(), empty()));
    }

    if (arrays.containsKey(this.attr_name_weights)) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Output attribute already exists.");
      sb.append(System.lineSeparator());
      sb.append("  Attribute: ");
      sb.append(this.attr_name_weights.value());
      sb.append(System.lineSeparator());
      errors = errors.append(SMFProcessingError.of(sb.toString(), empty()));
    }

    SortedMap<String, Vector<Double>> sources = TreeMap.empty();
    for (final SMFAttributeName name : arrays.keySet()) {
      if (name.value().startsWith(GROUP_ATTRIBUTE_PREFIX)) {
        final String name_sub =
          name.value().substring(GROUP_ATTRIBUTE_PREFIX.length());
        if (this.source_pattern.matcher(name_sub).matches()) {
          LOG.debug("matched attribute {}", name.value());

          final SMFAttributeArrayType array = arrays.get(name).get();
          if (array instanceof SMFAttributeArrayFloating1) {
            sources = sources.put(
              name.value(), ((SMFAttributeArrayFloating1) array).values());
          } else {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("Source attribute is of the wrong type.");
            sb.append(System.lineSeparator());
            sb.append("  Attribute: ");
            sb.append(name.value());
            sb.append(System.lineSeparator());
            sb.append("  Expected type: float 1 any");
            sb.append(System.lineSeparator());
            sb.append("  Received type: ");
            final SMFAttribute attr = mesh.header().attributesByName().get(name).get();
            sb.append(attr.componentType().getName());
            sb.append(" ");
            sb.append(attr.componentCount());
            sb.append(" ");
            sb.append(attr.componentSizeBits());
            sb.append(System.lineSeparator());
            errors = errors.append(SMFProcessingError.of(
              sb.toString(),
              empty()));
          }
        } else {
          LOG.debug("did not match attribute {}", name.value());
        }
      }
    }

    if (sources.isEmpty()) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("No attributes were matched by the given pattern.");
      sb.append(System.lineSeparator());
      sb.append("  Pattern: ");
      sb.append(this.source_pattern.pattern());
      sb.append(System.lineSeparator());
      errors = errors.append(SMFProcessingError.of(sb.toString(), empty()));
    }

    if (errors.isEmpty()) {
      try {
        return Validation.valid(this.process(context, mesh, sources));
      } catch (final CaLoaderException e) {
        return Validation.invalid(List.of(
          SMFProcessingError.of(e.getMessage(), Optional.of(e))));
      } catch (final NoSuchFileException e) {
        return Validation.invalid(List.of(
          SMFProcessingError.of(
            "No such file: " + e.getMessage(),
            Optional.of(e))));
      } catch (final IOException e) {
        return Validation.invalid(List.of(
          SMFProcessingError.of(e.getMessage(), Optional.of(e))));
      }
    }
    return Validation.invalid(errors);
  }

  private SMFMemoryMesh process(
    final SMFFilterCommandContext context,
    final SMFMemoryMesh mesh,
    final SortedMap<String, Vector<Double>> sources)
    throws CaLoaderException, IOException
  {
    final SortedMap<CaJointName, Vector<Double>> arrays_by_joint =
      sources.mapKeys(name -> CaJointName.of(name.substring(
        GROUP_ATTRIBUTE_PREFIX.length())));

    final Path file = context.resolvePath(this.skeleton_file);
    LOG.debug("resolved skeleton file: {}", file);

    final CaLoaderType loader = findLoader();
    try (final InputStream stream = Files.newInputStream(file)) {
      final CaSkeleton skeleton =
        loader.loadCompiledSkeletonFromStream(stream, file.toUri());
      final CaMeshWeightsAggregated packed =
        CaMeshWeightAggregation.aggregateWeights(skeleton, arrays_by_joint);

      final SMFAttribute attr_indices =
        SMFAttribute.of(
          this.attr_name_indices,
          ELEMENT_TYPE_INTEGER_UNSIGNED,
          4,
          packed.indexBitsRequired());
      final SMFAttribute attr_weights =
        SMFAttribute.of(this.attr_name_weights, ELEMENT_TYPE_FLOATING, 4, 32);

      final SMFHeader.Builder header_builder =
        SMFHeader.builder()
          .from(mesh.header())
          .addAttributesInOrder(attr_indices)
          .addAttributesInOrder(attr_weights);

      final Map<SMFAttributeName, SMFAttributeArrayType> arrays_new =
        mesh.arrays().put(
          this.attr_name_indices,
          SMFAttributeArrayIntegerUnsigned4.of(packed.vertexBoneIndices())).put(
          this.attr_name_weights,
          SMFAttributeArrayFloating4.of(packed.vertexWeights()));

      final SMFMemoryMesh.Builder mesh_builder =
        SMFMemoryMesh.builder()
          .from(mesh)
          .setArrays(arrays_new);

      switch (this.meta) {
        case META: {
          header_builder.setMetaCount(mesh.header().metaCount() + 1L);
          mesh_builder.addMetadata(SMFMetadata.of(
            Integer.toUnsignedLong(CaMeshMetas.VENDOR_ID),
            Integer.toUnsignedLong(CaMeshMetas.PRODUCT_ID),
            CaMeshMetas.serialize(skeleton.meta(), 1, 0)));
          break;
        }
        case NO_META: {
          break;
        }
      }

      return mesh_builder.setHeader(header_builder.build()).build();
    }
  }

  /**
   * A specification of whether or not skeleton metadata should be added to the
   * mesh.
   */

  public enum AddMetadata
  {
    /**
     * Metadata should be added.
     */

    META,

    /**
     * Metadata should not be added.
     */

    NO_META;

    /**
     * Parse a specification from a string
     *
     * @param text Must be "meta" or "no-meta"
     *
     * @return A specification
     */

    public static AddMetadata of(
      final String text)
    {
      switch (text) {
        case "meta":
          return META;
        case "no-meta":
          return NO_META;
        default: {
          throw new IllegalArgumentException("Expected 'meta' or 'no-meta'.");
        }
      }
    }
  }

}
