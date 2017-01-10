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

package com.io7m.jcalcium.tests.processing.smf;

import com.io7m.smfj.processing.api.SMFFilterCommandContext;
import org.junit.After;
import org.junit.Before;

import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * The type of memory mesh filter contracts.
 */

public abstract class CaMemoryMeshFilterContract
{
  protected FileSystem filesystem;

  @Before
  public final void setUp()
  {
    this.filesystem = CaTestFilesystems.makeEmptyUnixFilesystem();
  }

  @After
  public final void tearDown()
    throws Exception
  {
    this.filesystem.close();
  }

  protected final SMFFilterCommandContext createContext()
  {
    final Path root = this.filesystem.getRootDirectories().iterator().next();
    return SMFFilterCommandContext.of(
      root.resolve("x/y/z"),
      root.resolve("x/y/z"));
  }
}
