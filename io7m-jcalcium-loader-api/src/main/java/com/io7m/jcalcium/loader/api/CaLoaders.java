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

package com.io7m.jcalcium.loader.api;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Functions for finding loader providers.
 */

public final class CaLoaders
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaLoaders.class);
  }

  private CaLoaders()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Use a service loader to locate a loader format provider for {@code file}.
   * If {@code format_opt} is empty, the format type is inferred from the suffix
   * of {@code file}.
   *
   * @param file       A file
   * @param format_opt A format
   *
   * @return A format provider
   *
   * @throws CaLoaderNoSuchProviderException If no provider is available
   */

  public static CaLoaderFormatProviderType findProvider(
    final Path file,
    final Optional<String> format_opt)
    throws CaLoaderNoSuchProviderException
  {
    NullCheck.notNull(file, "File");
    NullCheck.notNull(format_opt, "Format");

    final ServiceLoader<CaLoaderFormatProviderType> loader =
      ServiceLoader.load(CaLoaderFormatProviderType.class);

    if (format_opt.isPresent()) {
      final String format = format_opt.get();

      LOG.debug("Attempting to find provider for {}", format);
      final Iterator<CaLoaderFormatProviderType> providers =
        loader.iterator();
      while (providers.hasNext()) {
        final CaLoaderFormatProviderType current_provider =
          providers.next();
        if (Objects.equals(current_provider.loaderFormat().name(), format)) {
          LOG.debug("using provider: {}", current_provider);
          return current_provider;
        }
      }

      LOG.error("Could not find a provider for the format '{}'", format);
      throw new CaLoaderNoSuchProviderException(
        file.toUri(), "No usable provider for format: " + format);
    }

    final String file_text = file.toString();
    LOG.debug("Attempting to infer format from file suffix");
    final int index = file_text.lastIndexOf('.');
    if (index != -1) {
      final String suffix = file_text.substring(index + 1);
      final Iterator<CaLoaderFormatProviderType> providers =
        loader.iterator();
      while (providers.hasNext()) {
        final CaLoaderFormatProviderType current_provider =
          providers.next();
        if (Objects.equals(current_provider.loaderFormat().suffix(), suffix)) {
          LOG.debug("Using provider: {}", current_provider);
          return current_provider;
        }
      }
    }

    LOG.error("File {} does not have a recognized suffix", file);
    throw new CaLoaderNoSuchProviderException(
      file.toUri(), "Cannot infer format from file suffix");
  }
}
