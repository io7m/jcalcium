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

package com.io7m.jcalcium.cmdline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.io7m.jcalcium.compiler.api.CaCompileError;
import com.io7m.jcalcium.compiler.api.CaCompilerType;
import com.io7m.jcalcium.compiler.main.CaCompiler;
import com.io7m.jcalcium.core.compiled.CaBone;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.CaFormatDescriptionType;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.parser.api.CaDefinitionParserFormatProviderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jcalcium.serializer.api.CaDefinitionSerializerFormatProviderType;
import com.io7m.jcalcium.serializer.api.CaDefinitionSerializerType;
import com.io7m.jfunctional.Unit;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import javaslang.collection.List;
import javaslang.collection.SortedSet;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

import static com.io7m.jfunctional.Unit.unit;

/**
 * The main command line program.
 */

public final class Main implements Runnable
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Main.class);
  }

  private final Map<String, CommandType> commands;
  private final JCommander commander;
  private final String[] args;
  private int exit_code;

  private Main(final String[] in_args)
  {
    this.args = NullCheck.notNull(in_args);

    final CommandRoot r = new CommandRoot();
    final CommandCompile compile = new CommandCompile();
    final CommandFormats formats = new CommandFormats();
    final CommandTranscode transcode = new CommandTranscode();
    final CommandValidate validate = new CommandValidate();

    this.commands = new HashMap<>(8);
    this.commands.put("compile", compile);
    this.commands.put("formats", formats);
    this.commands.put("validate", validate);
    this.commands.put("transcode", transcode);

    this.commander = new JCommander(r);
    this.commander.setProgramName("calcium");
    this.commander.addCommand("compile", compile);
    this.commander.addCommand("formats", formats);
    this.commander.addCommand("transcode", transcode);
    this.commander.addCommand("validate", validate);
  }

  /**
   * The main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(final String[] args)
  {
    final Main cm = new Main(args);
    cm.run();
    System.exit(cm.exitCode());
  }

  private static CaDefinitionParserFormatProviderType findParserProvider(
    final String format,
    final String file)
  {
    final ServiceLoader<CaDefinitionParserFormatProviderType> loader =
      ServiceLoader.load(CaDefinitionParserFormatProviderType.class);

    if (format == null) {
      LOG.debug("attempting to infer format from file suffix");
      final int index = file.lastIndexOf('.');
      if (index != -1) {
        final String suffix = file.substring(index + 1);
        final Iterator<CaDefinitionParserFormatProviderType> providers =
          loader.iterator();
        while (providers.hasNext()) {
          final CaDefinitionParserFormatProviderType current_provider =
            providers.next();
          if (current_provider.parserFormat().suffix().equals(suffix)) {
            LOG.debug("using provider: {}", current_provider);
            return current_provider;
          }
        }
      }

      LOG.error("File {} does not have a recognized suffix", file);
    } else {
      LOG.debug("attempting to find provider for {}", format);
      final Iterator<CaDefinitionParserFormatProviderType> providers =
        loader.iterator();
      while (providers.hasNext()) {
        final CaDefinitionParserFormatProviderType current_provider =
          providers.next();
        if (current_provider.parserFormat().name().equals(format)) {
          LOG.debug("using provider: {}", current_provider);
          return current_provider;
        }
      }

      LOG.error("Could not find a provider for the format '{}'", format);
    }

    return null;
  }

  private static CaDefinitionSerializerFormatProviderType findSerializerProvider(
    final String format,
    final String file)
  {
    final ServiceLoader<CaDefinitionSerializerFormatProviderType> loader =
      ServiceLoader.load(CaDefinitionSerializerFormatProviderType.class);

    if (format == null) {
      LOG.debug("attempting to infer format from file suffix");
      final int index = file.lastIndexOf('.');
      if (index != -1) {
        final String suffix = file.substring(index + 1);
        final Iterator<CaDefinitionSerializerFormatProviderType> providers =
          loader.iterator();
        while (providers.hasNext()) {
          final CaDefinitionSerializerFormatProviderType current_provider =
            providers.next();
          if (current_provider.serializerFormat().suffix().equals(suffix)) {
            LOG.debug("using provider: {}", current_provider);
            return current_provider;
          }
        }
      }

      LOG.error("File {} does not have a recognized suffix", file);
    } else {
      LOG.debug("attempting to find provider for {}", format);
      final Iterator<CaDefinitionSerializerFormatProviderType> providers =
        loader.iterator();
      while (providers.hasNext()) {
        final CaDefinitionSerializerFormatProviderType current_provider =
          providers.next();
        if (current_provider.serializerFormat().name().equals(format)) {
          LOG.debug("using provider: {}", current_provider);
          return current_provider;
        }
      }

      LOG.error("Could not find a provider for the format '{}'", format);
    }

    return null;
  }

  /**
   * @return The program exit code
   */

  public int exitCode()
  {
    return this.exit_code;
  }

  @Override
  public void run()
  {
    try {
      this.commander.parse(this.args);

      final String cmd = this.commander.getParsedCommand();
      if (cmd == null) {
        final StringBuilder sb = new StringBuilder(128);
        this.commander.usage(sb);
        LOG.info("Arguments required.\n{}", sb.toString());
        return;
      }

      final CommandType command = this.commands.get(cmd);
      command.call();

    } catch (final ParameterException e) {
      final StringBuilder sb = new StringBuilder(128);
      this.commander.usage(sb);
      LOG.error("{}\n{}", e.getMessage(), sb.toString());
      this.exit_code = 1;
    } catch (final Exception e) {
      LOG.error("{}", e.getMessage(), e);
      this.exit_code = 1;
    }
  }

  private interface CommandType extends Callable<Unit>
  {

  }

  private class CommandRoot implements CommandType
  {
    @Parameter(
      names = "-verbose",
      converter = CaLogLevelConverter.class,
      description = "Set the minimum logging verbosity level")
    private CaLogLevel verbose = CaLogLevel.LOG_INFO;

    CommandRoot()
    {

    }

    @Override
    public Unit call()
      throws Exception
    {
      final ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
          Logger.ROOT_LOGGER_NAME);
      root.setLevel(this.verbose.toLevel());
      return unit();
    }
  }

  @Parameters(commandDescription = "List supported formats")
  private final class CommandFormats extends CommandRoot
  {
    CommandFormats()
    {

    }

    @Override
    public Unit call()
      throws Exception
    {
      super.call();


      System.out.printf(
        "%-6s : %-6s : %-48s : %-10s : %-6s : %s\n",
        "# Name",
        "Suffix",
        "Mime type",
        "Version",
        "R/W",
        "Description");

      final ServiceLoader<CaDefinitionParserFormatProviderType> parser_loader =
        ServiceLoader.load(CaDefinitionParserFormatProviderType.class);
      final Iterator<CaDefinitionParserFormatProviderType> parser_providers =
        parser_loader.iterator();

      while (parser_providers.hasNext()) {
        final CaDefinitionParserFormatProviderType provider = parser_providers.next();
        final CaFormatDescriptionType format = provider.parserFormat();
        final SortedSet<CaFormatVersion> versions = provider.parserSupportedVersions();
        versions.forEach(version -> {
          System.out.printf(
            "%-6s : %-6s : %-48s : %-10s : %-6s : %s\n",
            format.name(),
            format.suffix(),
            format.mimeType(),
            String.format(
              "%d.%d",
              Integer.valueOf(version.major()),
              Integer.valueOf(version.minor())),
            "read",
            format.description());
        });
      }

      final ServiceLoader<CaDefinitionSerializerFormatProviderType> serializer_loader =
        ServiceLoader.load(CaDefinitionSerializerFormatProviderType.class);
      final Iterator<CaDefinitionSerializerFormatProviderType> serializer_providers =
        serializer_loader.iterator();

      while (serializer_providers.hasNext()) {
        final CaDefinitionSerializerFormatProviderType provider = serializer_providers.next();
        final CaFormatDescriptionType format = provider.serializerFormat();
        final SortedSet<CaFormatVersion> versions = provider.serializerSupportedVersions();
        versions.forEach(version -> {
          System.out.printf(
            "%-6s : %-6s : %-48s : %-10s : %-6s : %s\n",
            format.name(),
            format.suffix(),
            format.mimeType(),
            String.format(
              "%d.%d",
              Integer.valueOf(version.major()),
              Integer.valueOf(version.minor())),
            "write",
            format.description());
        });
      }

      return unit();
    }
  }

  @Parameters(commandDescription = "Transcode a skeleton file")
  private final class CommandTranscode extends CommandRoot
  {
    @Parameter(
      names = "-file-in",
      required = true,
      description = "The input file")
    private String file_in;

    @Parameter(
      names = "-format-in",
      description = "The input file format")
    private String format_in;

    @Parameter(
      names = "-file-out",
      required = true,
      description = "The output file")
    private String file_out;

    @Parameter(
      names = "-format-out",
      description = "The output file format")
    private String format_out;

    CommandTranscode()
    {

    }

    @Override
    public Unit call()
      throws Exception
    {
      super.call();

      final CaDefinitionParserFormatProviderType provider_parser =
        findParserProvider(this.format_in, this.file_in);
      final CaDefinitionSerializerFormatProviderType provider_serializer =
        findSerializerProvider(this.format_out, this.file_out);

      if (provider_parser != null && provider_serializer != null) {
        final CaDefinitionParserType parser =
          provider_parser.parserCreate();
        final CaDefinitionSerializerType serializer =
          provider_serializer.serializerCreate(
            provider_serializer.serializerSupportedVersions().last());

        final Path path_in =
          Paths.get(this.file_in);
        final Path path_out =
          Paths.get(this.file_out);

        try (final InputStream is = Files.newInputStream(path_in)) {
          final Validation<List<CaParseError>, CaDefinitionSkeleton> result =
            parser.parseSkeletonFromStream(is, URI.create(this.file_in));
          if (result.isValid()) {
            LOG.debug("parsed successfully");

            try (final OutputStream out = Files.newOutputStream(path_out)) {
              serializer.serializeSkeletonToStream(result.get(), out);
            }

          } else {
            LOG.error("parsing failed");
            result.getError().forEach(error -> {
              final LexicalPosition<Path> lexical = error.lexical();
              LOG.error(
                "{}:{}: {}",
                Integer.valueOf(lexical.line()),
                Integer.valueOf(lexical.column()),
                error.message());
            });
          }
        }
      }

      return unit();
    }
  }

  @Parameters(commandDescription = "Validate a skeleton file")
  private final class CommandValidate extends CommandRoot
  {
    @Parameter(
      names = "-file",
      required = true,
      description = "The input file")
    private String file;

    @Parameter(
      names = "-format",
      description = "The input file format")
    private String format;

    CommandValidate()
    {

    }

    @Override
    public Unit call()
      throws Exception
    {
      super.call();

      final CaDefinitionParserFormatProviderType provider =
        findParserProvider(this.format, this.file);

      if (provider != null) {
        final CaDefinitionParserType parser = provider.parserCreate();

        final Path path = Paths.get(this.file);
        try (final InputStream is = Files.newInputStream(path)) {
          final Validation<List<CaParseError>, CaDefinitionSkeleton> result =
            parser.parseSkeletonFromStream(is, URI.create(this.file));
          if (result.isValid()) {
            LOG.debug("parsed successfully");
            // ...
          } else {
            LOG.error("parsing failed");
            result.getError().forEach(error -> {
              final LexicalPosition<Path> lexical = error.lexical();
              LOG.error(
                "{}:{}: {}",
                Integer.valueOf(lexical.line()),
                Integer.valueOf(lexical.column()),
                error.message());
            });
          }
        }

      } else {
        LOG.error("Could not find a suitable format provider");
        Main.this.exit_code = 1;
      }

      return unit();
    }

    private void validate(
      final CaDefinitionSkeleton sk)
    {
      LOG.debug(
        "parsed skeleton: {}, {} bones, {} actions",
        sk.name().value(),
        Integer.valueOf(sk.bones().size()),
        Integer.valueOf(sk.actions().size()));
    }
  }

  @Parameters(commandDescription = "Compile a skeleton file")
  private final class CommandCompile extends CommandRoot
  {
    @Parameter(
      names = "-file",
      required = true,
      description = "The input file")
    private String file;

    @Parameter(
      names = "-format",
      description = "The input file format")
    private String format;

    CommandCompile()
    {

    }

    @Override
    public Unit call()
      throws Exception
    {
      super.call();

      final CaCompilerType compiler = CaCompiler.create();
      final CaDefinitionParserFormatProviderType provider =
        findParserProvider(this.format, this.file);

      if (provider != null) {
        final CaDefinitionParserType parser = provider.parserCreate();

        final Path path = Paths.get(this.file);
        try (final InputStream is = Files.newInputStream(path)) {
          final Validation<List<CaParseError>, CaDefinitionSkeleton> parse_result =
            parser.parseSkeletonFromStream(is, URI.create(this.file));

          if (!parse_result.isValid()) {
            LOG.error("parsing failed");
            parse_result.getError().forEach(error -> {
              final LexicalPosition<Path> lexical = error.lexical();
              LOG.error(
                "{}:{}: {}",
                Integer.valueOf(lexical.line()),
                Integer.valueOf(lexical.column()),
                error.message());
            });
            return unit();
          }

          LOG.debug("compiling");
          final Validation<List<CaCompileError>, CaSkeleton> compile_result =
            compiler.compile(parse_result.get());

          if (!compile_result.isValid()) {
            LOG.error("compilation failed");
            compile_result.getError().forEach(
              error -> LOG.error("{}: {}", error.code(), error.message()));
            return unit();
          }

          final CaSkeleton compiled = compile_result.get();
          compiled.bones().forEachBreadthFirst(unit(), (input, depth, node) -> {
            final CaBone bone = node.value();

            final Optional<JOTreeNodeReadableType<CaBone>> parent_opt =
              node.parentReadable();
            if (parent_opt.isPresent()) {
              final JOTreeNodeReadableType<CaBone> parent = parent_opt.get();
              LOG.debug(
                "{}:{}:{}:{}",
                Integer.valueOf(depth),
                Integer.valueOf(parent.value().id()),
                Integer.valueOf(bone.id()),
                bone.name().value());
            } else {
              LOG.debug(
                "{}:{}:{}:{}",
                Integer.valueOf(depth),
                "-",
                Integer.valueOf(bone.id()),
                bone.name().value());
            }
          });
        }

      } else {
        LOG.error("Could not find a suitable format provider");
        Main.this.exit_code = 1;
      }

      return unit();
    }
  }
}
