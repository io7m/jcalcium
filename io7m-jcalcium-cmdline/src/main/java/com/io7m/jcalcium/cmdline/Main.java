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
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeleton;
import com.io7m.jcalcium.core.definitions.CaFormatDescriptionType;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.loader.api.CaLoaderFormatProviderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserFormatProviderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseError;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerFormatProviderType;
import com.io7m.jcalcium.serializer.api.CaDefinitionSerializerFormatProviderType;
import com.io7m.jfunctional.Unit;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.smfj.frontend.SMFFilterCommandFile;
import com.io7m.smfj.frontend.SMFParserProviders;
import com.io7m.smfj.frontend.SMFSerializerProviders;
import com.io7m.smfj.parser.api.SMFParseError;
import com.io7m.smfj.parser.api.SMFParserProviderType;
import com.io7m.smfj.parser.api.SMFParserSequentialType;
import com.io7m.smfj.processing.api.SMFFilterCommandContext;
import com.io7m.smfj.processing.api.SMFFilterCommandModuleResolver;
import com.io7m.smfj.processing.api.SMFFilterCommandModuleResolverType;
import com.io7m.smfj.processing.api.SMFFilterCommandModuleType;
import com.io7m.smfj.processing.api.SMFMemoryMesh;
import com.io7m.smfj.processing.api.SMFMemoryMeshFilterType;
import com.io7m.smfj.processing.api.SMFMemoryMeshProducer;
import com.io7m.smfj.processing.api.SMFMemoryMeshProducerType;
import com.io7m.smfj.processing.api.SMFMemoryMeshSerializer;
import com.io7m.smfj.processing.api.SMFProcessingError;
import com.io7m.smfj.serializer.api.SMFSerializerProviderType;
import com.io7m.smfj.serializer.api.SMFSerializerType;
import javaslang.collection.List;
import javaslang.collection.Seq;
import javaslang.collection.SortedSet;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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
    final CommandCompileSkeleton compile_skeleton =
      new CommandCompileSkeleton();
    final CommandCompileMesh compile_mesh =
      new CommandCompileMesh();
    final CommandFormats formats =
      new CommandFormats();
    final CommandListFilters list_filters =
      new CommandListFilters();

    this.commands = new HashMap<>(8);
    this.commands.put("compile-skeleton", compile_skeleton);
    this.commands.put("compile-mesh", compile_mesh);
    this.commands.put("formats", formats);
    this.commands.put("list-filters", list_filters);

    this.commander = new JCommander(r);
    this.commander.setProgramName("calcium");
    this.commander.addCommand("compile-skeleton", compile_skeleton);
    this.commander.addCommand("compile-mesh", compile_mesh);
    this.commander.addCommand("formats", formats);
    this.commander.addCommand("list-filters", list_filters);
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
          if (Objects.equals(current_provider.parserFormat().suffix(), suffix)) {
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
        if (Objects.equals(current_provider.parserFormat().name(), format)) {
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
          if (Objects.equals(
            current_provider.serializerFormat().suffix(),
            suffix)) {
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
        if (Objects.equals(current_provider.serializerFormat().name(), format)) {
          LOG.debug("using provider: {}", current_provider);
          return current_provider;
        }
      }

      LOG.error("Could not find a provider for the format '{}'", format);
    }

    return null;
  }

  private static CaCompiledSerializerFormatProviderType findCompiledSerializerProvider(
    final String format,
    final String file)
  {
    final ServiceLoader<CaCompiledSerializerFormatProviderType> loader =
      ServiceLoader.load(CaCompiledSerializerFormatProviderType.class);

    if (format == null) {
      LOG.debug("attempting to infer format from file suffix");
      final int index = file.lastIndexOf('.');
      if (index != -1) {
        final String suffix = file.substring(index + 1);
        final Iterator<CaCompiledSerializerFormatProviderType> providers =
          loader.iterator();
        while (providers.hasNext()) {
          final CaCompiledSerializerFormatProviderType current_provider =
            providers.next();
          if (Objects.equals(
            current_provider.serializerFormat().suffix(),
            suffix)) {
            LOG.debug("using provider: {}", current_provider);
            return current_provider;
          }
        }
      }

      LOG.error("File {} does not have a recognized suffix", file);
    } else {
      LOG.debug("attempting to find provider for {}", format);
      final Iterator<CaCompiledSerializerFormatProviderType> providers =
        loader.iterator();
      while (providers.hasNext()) {
        final CaCompiledSerializerFormatProviderType current_provider =
          providers.next();
        if (Objects.equals(current_provider.serializerFormat().name(), format)) {
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

  @Parameters(commandDescription = "List available filters")
  private final class CommandListFilters extends CommandRoot
  {
    CommandListFilters()
    {

    }

    @Override
    public Unit call()
      throws Exception
    {
      super.call();

      final SMFFilterCommandModuleResolverType r =
        SMFFilterCommandModuleResolver.create();

      for (final String module_name : r.available().keySet()) {
        final SMFFilterCommandModuleType module =
          r.available().get(module_name).get();
        for (final String command_name : module.parsers().keySet()) {
          System.out.print(module_name);
          System.out.print(":");
          System.out.print(command_name);
          System.out.println();
        }
      }

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

      final String fmt_s = "%-6s : %-6s : %-48s : %-10s : %-12s : %s\n";
      System.out.printf(
        fmt_s,
        "# Name",
        "Suffix",
        "Mime type",
        "Version",
        "Op",
        "Description");

      this.listParserFormats(fmt_s);
      this.listDefinitionSerializerFormats(fmt_s);
      this.listCompiledSerializerFormats(fmt_s);
      this.listLoaderFormats(fmt_s);
      return unit();
    }

    private void listLoaderFormats(
      final String fmt_s)
    {
      final ServiceLoader<CaLoaderFormatProviderType> loader_loader =
        ServiceLoader.load(CaLoaderFormatProviderType.class);
      final Iterator<CaLoaderFormatProviderType> loader_providers =
        loader_loader.iterator();

      while (loader_providers.hasNext()) {
        final CaLoaderFormatProviderType provider =
          loader_providers.next();
        final CaFormatDescriptionType format =
          provider.loaderFormat();
        final SortedSet<CaFormatVersion> versions =
          provider.loaderSupportedVersions();
        versions.forEach(version -> System.out.printf(
          fmt_s,
          format.name(),
          format.suffix(),
          format.mimeType(),
          String.format(
            "%d.%d",
            Integer.valueOf(version.major()),
            Integer.valueOf(version.minor())),
          "load",
          format.description()));
      }
    }

    private void listCompiledSerializerFormats(
      final String fmt_s)
    {
      final ServiceLoader<CaCompiledSerializerFormatProviderType> compiled_serializer_loader =
        ServiceLoader.load(CaCompiledSerializerFormatProviderType.class);
      final Iterator<CaCompiledSerializerFormatProviderType> compiled_serializer_providers =
        compiled_serializer_loader.iterator();

      while (compiled_serializer_providers.hasNext()) {
        final CaCompiledSerializerFormatProviderType provider =
          compiled_serializer_providers.next();
        final CaFormatDescriptionType format =
          provider.serializerFormat();
        final SortedSet<CaFormatVersion> versions =
          provider.serializerSupportedVersions();
        versions.forEach(version -> System.out.printf(
          fmt_s,
          format.name(),
          format.suffix(),
          format.mimeType(),
          String.format(
            "%d.%d",
            Integer.valueOf(version.major()),
            Integer.valueOf(version.minor())),
          "compile",
          format.description()));
      }
    }

    private void listDefinitionSerializerFormats(
      final String fmt_s)
    {
      final ServiceLoader<CaDefinitionSerializerFormatProviderType> serializer_loader =
        ServiceLoader.load(CaDefinitionSerializerFormatProviderType.class);
      final Iterator<CaDefinitionSerializerFormatProviderType> serializer_providers =
        serializer_loader.iterator();

      while (serializer_providers.hasNext()) {
        final CaDefinitionSerializerFormatProviderType provider =
          serializer_providers.next();
        final CaFormatDescriptionType format =
          provider.serializerFormat();
        final SortedSet<CaFormatVersion> versions =
          provider.serializerSupportedVersions();
        versions.forEach(version -> System.out.printf(
          fmt_s,
          format.name(),
          format.suffix(),
          format.mimeType(),
          String.format(
            "%d.%d",
            Integer.valueOf(version.major()),
            Integer.valueOf(version.minor())),
          "serialize",
          format.description()));
      }
    }

    private void listParserFormats(
      final String fmt_s)
    {
      final ServiceLoader<CaDefinitionParserFormatProviderType> parser_loader =
        ServiceLoader.load(CaDefinitionParserFormatProviderType.class);
      final Iterator<CaDefinitionParserFormatProviderType> parser_providers =
        parser_loader.iterator();

      while (parser_providers.hasNext()) {
        final CaDefinitionParserFormatProviderType provider =
          parser_providers.next();
        final CaFormatDescriptionType format =
          provider.parserFormat();
        final SortedSet<CaFormatVersion> versions =
          provider.parserSupportedVersions();
        versions.forEach(version -> System.out.printf(
          fmt_s,
          format.name(),
          format.suffix(),
          format.mimeType(),
          String.format(
            "%d.%d",
            Integer.valueOf(version.major()),
            Integer.valueOf(version.minor())),
          "parse",
          format.description()));
      }
    }
  }

  @Parameters(commandDescription = "Compile mesh data")
  private final class CommandCompileMesh extends CommandRoot
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
      description = "The output file")
    private String file_out;

    @Parameter(
      names = "-format-out",
      description = "The output file format")
    private String format_out;

    @Parameter(
      names = "-commands",
      required = true,
      description = "The filter commands")
    private String file_commands;

    @Parameter(
      names = "-source-directory",
      description = "The source directory")
    private String source_directory = System.getProperty("user.dir");

    CommandCompileMesh()
    {

    }

    @Override
    public Unit call()
      throws Exception
    {
      super.call();

      final Optional<List<SMFMemoryMeshFilterType>> filters_opt =
        this.parseFilterCommands();

      if (!filters_opt.isPresent()) {
        Main.this.exit_code = 1;
        return unit();
      }

      final List<SMFMemoryMeshFilterType> filters = filters_opt.get();

      final Optional<SMFParserProviderType> provider_parser_opt =
        SMFParserProviders.findParserProvider(
          Optional.ofNullable(this.format_in),
          this.file_in);

      if (!provider_parser_opt.isPresent()) {
        Main.this.exit_code = 1;
        return unit();
      }

      final SMFParserProviderType provider_parser = provider_parser_opt.get();
      final Path path_in = Paths.get(this.file_in);

      final Optional<SMFMemoryMesh> mesh_opt =
        this.loadMemoryMesh(provider_parser, path_in);

      if (!mesh_opt.isPresent()) {
        Main.this.exit_code = 1;
        return unit();
      }

      final SMFFilterCommandContext context =
        SMFFilterCommandContext.of(
          Paths.get(this.source_directory).toAbsolutePath(),
          Paths.get(this.file_commands).toAbsolutePath());

      final Optional<SMFMemoryMesh> filtered_opt =
        this.runFilters(context, filters, mesh_opt.get());

      if (!filtered_opt.isPresent()) {
        Main.this.exit_code = 1;
        return unit();
      }

      final SMFMemoryMesh filtered = filtered_opt.get();

      if (this.file_out != null) {
        final Optional<SMFSerializerProviderType> provider_serializer_opt =
          SMFSerializerProviders.findSerializerProvider(
            Optional.ofNullable(this.format_out), this.file_out);

        if (!provider_serializer_opt.isPresent()) {
          Main.this.exit_code = 1;
          return unit();
        }

        final SMFSerializerProviderType provider_serializer =
          provider_serializer_opt.get();
        final Path path_out = Paths.get(this.file_out);

        LOG.debug("writing mesh to {}", path_out);
        try (final OutputStream os = Files.newOutputStream(path_out)) {
          try (final SMFSerializerType serializer =
                 provider_serializer.serializerCreate(
                   provider_serializer.serializerSupportedVersions().last(),
                   path_out,
                   os)) {
            SMFMemoryMeshSerializer.serialize(filtered, serializer);
          }
        } catch (final IOException e) {
          Main.this.exit_code = 1;
          LOG.error("could not serialize mesh: {}", e.getMessage());
          LOG.debug("i/o error: ", e);
        }
      }

      return unit();
    }

    private Optional<SMFMemoryMesh> runFilters(
      final SMFFilterCommandContext context,
      final Seq<SMFMemoryMeshFilterType> filters,
      final SMFMemoryMesh mesh)
    {
      SMFMemoryMesh mesh_current = mesh;
      for (int index = 0; index < filters.size(); ++index) {
        final SMFMemoryMeshFilterType filter = filters.get(index);
        LOG.debug("evaluating filter: {}", filter.name());

        final Validation<List<SMFProcessingError>, SMFMemoryMesh> result =
          filter.filter(context, mesh_current);
        if (result.isValid()) {
          mesh_current = result.get();
        } else {
          result.getError().map(e -> {
            LOG.error("filter: {}: {}", filter.name(), e.message());
            return unit();
          });
          return Optional.empty();
        }
      }

      return Optional.of(mesh_current);
    }

    private Optional<SMFMemoryMesh> loadMemoryMesh(
      final SMFParserProviderType provider_parser,
      final Path path_in)
      throws IOException
    {
      final SMFMemoryMeshProducerType loader =
        SMFMemoryMeshProducer.create();

      try (final InputStream is = Files.newInputStream(path_in)) {
        try (final SMFParserSequentialType parser =
               provider_parser.parserCreateSequential(loader, path_in, is)) {
          parser.parseHeader();
          if (!parser.parserHasFailed()) {
            parser.parseData();
          }
        }
        if (!loader.errors().isEmpty()) {
          loader.errors().map(e -> {
            final LexicalPosition<Path> lex = e.lexical();
            LOG.error(
              "{}:{}:{}: {}",
              this.file_in,
              Integer.valueOf(lex.line()),
              Integer.valueOf(lex.column()),
              e.message());
            return unit();
          });
          Main.this.exit_code = 1;
          return Optional.empty();
        }
      }
      return Optional.of(loader.mesh());
    }

    private Optional<List<SMFMemoryMeshFilterType>> parseFilterCommands()
      throws IOException
    {
      final Path path_commands = Paths.get(this.file_commands);
      final SMFFilterCommandModuleResolverType resolver =
        SMFFilterCommandModuleResolver.create();

      try (final InputStream stream = Files.newInputStream(path_commands)) {
        final Validation<List<SMFParseError>, List<SMFMemoryMeshFilterType>> r =
          SMFFilterCommandFile.parseFromStream(
            resolver,
            Optional.of(path_commands),
            stream);
        if (r.isValid()) {
          return Optional.of(r.get());
        }

        r.getError().map(e -> {
          final LexicalPosition<Path> lex = e.lexical();
          LOG.error(
            "{}:{}:{}: {}",
            path_commands,
            Integer.valueOf(lex.line()),
            Integer.valueOf(lex.column()),
            e.message());
          return unit();
        });

        return Optional.empty();
      }
    }
  }

  @Parameters(commandDescription = "Compile a skeleton file")
  private final class CommandCompileSkeleton extends CommandRoot
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

    CommandCompileSkeleton()
    {

    }

    @Override
    public Unit call()
      throws Exception
    {
      super.call();

      final CaCompilerType compiler = CaCompiler.create();
      final CaDefinitionParserFormatProviderType parser_provider =
        findParserProvider(this.format_in, this.file_in);
      final CaCompiledSerializerFormatProviderType serial_provider =
        findCompiledSerializerProvider(this.format_out, this.file_out);

      if (parser_provider == null) {
        LOG.error("Could not find a suitable format provider");
        Main.this.exit_code = 1;
        return unit();
      }

      if (serial_provider == null) {
        LOG.error("Could not find a suitable format provider");
        Main.this.exit_code = 1;
        return unit();
      }

      final CaDefinitionParserType parser = parser_provider.parserCreate();

      final Path path_in = Paths.get(this.file_in);
      final Path path_out = Paths.get(this.file_out);

      try (final InputStream is = Files.newInputStream(path_in)) {
        final Validation<List<CaParseError>, CaDefinitionSkeleton> parse_result =
          parser.parseSkeletonFromStream(is, URI.create(this.file_in));

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
        compiled.joints().forEachBreadthFirst(unit(), (input, depth, node) -> {
          final CaJoint bone = node.value();

          final Optional<JOTreeNodeReadableType<CaJoint>> parent_opt =
            node.parentReadable();
          if (parent_opt.isPresent()) {
            final JOTreeNodeReadableType<CaJoint> parent = parent_opt.get();
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

        try (final OutputStream out = Files.newOutputStream(path_out)) {
          final CaFormatVersion version =
            serial_provider.serializerSupportedVersions().last();
          serial_provider.serializerCreate(version)
            .serializeCompiledSkeletonToStream(compiled, out);
        }
      }

      return unit();
    }
  }
}
