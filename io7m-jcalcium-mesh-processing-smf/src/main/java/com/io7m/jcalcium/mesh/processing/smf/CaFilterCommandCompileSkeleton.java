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

import com.io7m.jcalcium.compiler.api.CaCompileError;
import com.io7m.jcalcium.compiler.api.CaCompilerProviderType;
import com.io7m.jcalcium.compiler.api.CaCompilerType;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.parser.api.CaDefinitionParserFormatProviderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerFormatProviderType;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerType;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import com.io7m.smfj.parser.api.SMFParseError;
import com.io7m.smfj.processing.api.SMFFilterCommandContext;
import com.io7m.smfj.processing.api.SMFFilterCommandParsing;
import com.io7m.smfj.processing.api.SMFMemoryMesh;
import com.io7m.smfj.processing.api.SMFMemoryMeshFilterType;
import com.io7m.smfj.processing.api.SMFProcessingError;
import javaslang.collection.List;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * A command that compiles a skeleton as a side effect, leaving the mesh
 * untouched.
 */

public final class CaFilterCommandCompileSkeleton implements
  SMFMemoryMeshFilterType
{
  /**
   * The command name.
   */

  public static final String NAME;

  private static final String SYNTAX;
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaFilterCommandCompileSkeleton.class);
    NAME = "compile-skeleton";
    SYNTAX = "<skeleton-input> <skeleton-output>";
  }

  private final Path skeleton_file_input;
  private final Path skeleton_file_output;

  private CaFilterCommandCompileSkeleton(
    final Path in_skeleton_file_input,
    final Path in_skeleton_file_output)
  {
    this.skeleton_file_input =
      NullCheck.notNull(in_skeleton_file_input, "Skeleton file input");
    this.skeleton_file_output =
      NullCheck.notNull(in_skeleton_file_output, "Skeleton file output");
  }

  /**
   * Create a new filter.
   *
   * @param in_skeleton_file_input  The path to the input skeleton file
   * @param in_skeleton_file_output The path to the output skeleton
   *
   * @return A new filter
   */

  public static SMFMemoryMeshFilterType create(
    final Path in_skeleton_file_input,
    final Path in_skeleton_file_output)
  {
    return new CaFilterCommandCompileSkeleton(
      in_skeleton_file_input,
      in_skeleton_file_output);
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

    if (text.length() == 2) {
      try {
        final Path skeleton_file_input =
          Paths.get(text.get(0));
        final Path skeleton_file_output =
          Paths.get(text.get(1));

        LOG.debug("skeleton file input: {}", skeleton_file_input);
        LOG.debug("skeleton file output: {}", skeleton_file_output);

        return Validation.valid(new CaFilterCommandCompileSkeleton(
          skeleton_file_input, skeleton_file_output));
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
          LexicalPosition.of(line, 0, file), sb.toString(), Optional.empty())));
      } catch (final IllegalArgumentException e) {
        return SMFFilterCommandParsing.errorExpectedGotValidation(
          file, line, makeSyntax(), text);
      }
    }
    return SMFFilterCommandParsing.errorExpectedGotValidation(
      file, line, makeSyntax(), text);
  }

  private static CaCompilerType findCompiler()
  {
    final ServiceLoader<CaCompilerProviderType> service_loader =
      ServiceLoader.load(CaCompilerProviderType.class);
    final Iterator<CaCompilerProviderType> providers =
      service_loader.iterator();

    if (providers.hasNext()) {
      final CaCompilerProviderType provider = providers.next();
      return provider.create();
    }

    throw new UnsupportedOperationException("No available skeleton compiler");
  }

  private static CaDefinitionParserType findParser()
  {
    final ServiceLoader<CaDefinitionParserFormatProviderType> service_loader =
      ServiceLoader.load(CaDefinitionParserFormatProviderType.class);
    final Iterator<CaDefinitionParserFormatProviderType> providers =
      service_loader.iterator();

    if (providers.hasNext()) {
      final CaDefinitionParserFormatProviderType provider = providers.next();
      return provider.parserCreate();
    }

    throw new UnsupportedOperationException("No available skeleton parser");
  }

  private static CaCompiledSerializerType findSerializer()
  {
    final ServiceLoader<CaCompiledSerializerFormatProviderType> service_loader =
      ServiceLoader.load(CaCompiledSerializerFormatProviderType.class);
    final Iterator<CaCompiledSerializerFormatProviderType> providers =
      service_loader.iterator();

    if (providers.hasNext()) {
      final CaCompiledSerializerFormatProviderType provider = providers.next();
      return provider.serializerCreate(CaFormatVersion.of(1, 0));
    }

    throw new UnsupportedOperationException("No available skeleton parser");
  }

  private static List<SMFProcessingError> processingErrorOfCompileAll(
    final List<CaCompileError> error)
  {
    return error.map(CaFilterCommandCompileSkeleton::processingErrorOfCompile);
  }

  private static SMFProcessingError processingErrorOfCompile(
    final CaCompileError error)
  {
    return SMFProcessingError.of(error.message(), Optional.empty());
  }

  private static List<SMFProcessingError> processingErrorOfParseAll(
    final List<CaParseError> error)
  {
    return error.map(CaFilterCommandCompileSkeleton::processingErrorOfParse);
  }

  private static SMFProcessingError processingErrorOfParse(
    final CaParseError error)
  {
    return SMFProcessingError.of(error.message(), Optional.empty());
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

    final CaDefinitionParserType parser = findParser();
    final CaCompilerType compiler = findCompiler();
    final CaCompiledSerializerType serial = findSerializer();

    final Path resolved_input = context.resolvePath(this.skeleton_file_input);
    final Path resolved_output = context.resolvePath(this.skeleton_file_output);

    List<SMFProcessingError> errors = List.empty();
    try (final InputStream is = Files.newInputStream(resolved_input)) {
      final Validation<List<CaParseError>, CaDefinitionSkeleton> parse_result =
        parser.parseSkeletonFromStream(is, resolved_input.toUri());
      if (parse_result.isValid()) {
        final Validation<List<CaCompileError>, CaSkeleton> compile_result =
          compiler.compile(parse_result.get());
        if (compile_result.isValid()) {
          try (final OutputStream os = Files.newOutputStream(resolved_output)) {
            serial.serializeCompiledSkeletonToStream(compile_result.get(), os);
          }
        } else {
          errors = errors.appendAll(compile_result.mapError(
            CaFilterCommandCompileSkeleton::processingErrorOfCompileAll).getError());
        }
      } else {
        errors = errors.appendAll(parse_result.mapError(
          CaFilterCommandCompileSkeleton::processingErrorOfParseAll).getError());
      }
    } catch (final IOException e) {
      errors = errors.append(
        SMFProcessingError.of("I/O error: " + e.getMessage(), Optional.of(e)));
    }

    if (errors.isEmpty()) {
      return Validation.valid(mesh);
    }

    return Validation.invalid(errors);
  }
}
