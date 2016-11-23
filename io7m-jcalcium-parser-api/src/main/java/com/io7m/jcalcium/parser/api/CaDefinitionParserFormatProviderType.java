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

package com.io7m.jcalcium.parser.api;

import com.io7m.jcalcium.core.definitions.CaFormatDescription;
import com.io7m.jcalcium.core.definitions.CaFormatDescriptionType;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.core.definitions.CaFormatVersionType;
import javaslang.collection.SortedSet;

/**
 * A provider of parsers for a given format.
 */

public interface CaDefinitionParserFormatProviderType
{
  /**
   * @return The format that this provider supports
   */

  CaFormatDescription parserFormat();

  /**
   * @return The supported versions of the format
   */

  SortedSet<CaFormatVersion> parserSupportedVersions();

  /**
   * @return A new parser for the format
   */

  CaDefinitionParserType parserCreate();
}
