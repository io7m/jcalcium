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
import com.io7m.jcalcium.core.definitions.CaDefinitionSkeletonType;
import com.io7m.jcalcium.core.definitions.CaFormatDescriptionType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserFormatProviderType;
import com.io7m.jcalcium.parser.api.CaDefinitionParserType;
import com.io7m.jcalcium.parser.api.CaParseErrorType;
import com.io7m.jcalcium.parser.api.CaParserVersionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jlexing.core.ImmutableLexicalPositionType;
import com.io7m.jnull.NullCheck;
import javaslang.collection.List;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    final CommandValidate validate = new CommandValidate();
    final CommandFormats formats = new CommandFormats();

    this.commands = new HashMap<>(8);
    this.commands.put("formats", formats);
    this.commands.put("validate", validate);

    this.commander = new JCommander(r);
    this.commander.setProgramName("calcium");
    this.commander.addCommand("validate", validate);
    this.commander.addCommand("formats", formats);
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

      final ServiceLoader<CaDefinitionParserFormatProviderType> loader =
        ServiceLoader.load(CaDefinitionParserFormatProviderType.class);
      final Iterator<CaDefinitionParserFormatProviderType> providers =
        loader.iterator();

      System.out.printf(
        "%-6s : %-6s : %-48s : %-10s : %-6s : %s\n",
        "# Name",
        "Suffix",
        "Mime type",
        "Version",
        "R/W",
        "Description");

      while (providers.hasNext()) {
        final CaDefinitionParserFormatProviderType provider = providers.next();
        final CaFormatDescriptionType format = provider.format();
        final List<CaParserVersionType> versions = provider.versions();
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

      final CaDefinitionParserFormatProviderType provider = this.findProvider();
      if (provider != null) {

        final CaDefinitionParserType parser = provider.create();

        final Path path = Paths.get(this.file);
        try (final InputStream is = Files.newInputStream(path)) {
          final Validation<List<CaParseErrorType>, CaDefinitionSkeletonType> result =
            parser.parseSkeletonFromStream(is, URI.create(this.file));
          if (result.isValid()) {
            LOG.debug("parsed successfully");
            // ...
          } else {
            LOG.error("parsing failed");
            result.getError().forEach(error -> {
              final ImmutableLexicalPositionType<Path> lexical = error.lexical();
              LOG.error(
                "{}:{}: {}",
                Integer.valueOf(lexical.getLine()),
                Integer.valueOf(lexical.getColumn()),
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

    private CaDefinitionParserFormatProviderType findProvider()
    {
      final ServiceLoader<CaDefinitionParserFormatProviderType> loader =
        ServiceLoader.load(CaDefinitionParserFormatProviderType.class);

      if (this.format == null) {
        LOG.debug("attempting to infer format from file suffix");
        final int index = this.file.lastIndexOf('.');
        if (index != -1) {
          final String suffix = this.file.substring(index + 1);
          final Iterator<CaDefinitionParserFormatProviderType> providers =
            loader.iterator();
          while (providers.hasNext()) {
            final CaDefinitionParserFormatProviderType current_provider =
              providers.next();
            if (current_provider.format().suffix().equals(suffix)) {
              LOG.debug("using provider: {}", current_provider);
              return current_provider;
            }
          }
        }

        LOG.error("File does not have a recognized suffix");
      } else {
        LOG.debug("attempting to find provider for {}", this.format);
        final Iterator<CaDefinitionParserFormatProviderType> providers =
          loader.iterator();
        while (providers.hasNext()) {
          final CaDefinitionParserFormatProviderType current_provider =
            providers.next();
          if (current_provider.format().name().equals(this.format)) {
            LOG.debug("using provider: {}", current_provider);
            return current_provider;
          }
        }

        LOG.error("Could not find a provider for the format '{}'", this.format);
      }

      return null;
    }

    private void validate(
      final CaDefinitionSkeletonType sk)
    {
      LOG.debug(
        "parsed skeleton: {}, {} bones, {} actions",
        sk.name().value(),
        Integer.valueOf(sk.bones().size()),
        Integer.valueOf(sk.actions().size()));
    }
  }
}
