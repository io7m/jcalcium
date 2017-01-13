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

package com.io7m.jcalcium.tests.evaluator.main;

import com.io7m.jaffirm.core.PreconditionViolationException;
import com.io7m.jcalcium.evaluator.api.CaActionKeyframeCurrent;
import com.io7m.jcalcium.evaluator.main.CaActionTimeline;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CaActionTimelineTest
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaActionTimelineTest.class);
  }

  @Rule public ExpectedException expected = ExpectedException.none();

  @Test
  public void testEmpty()
  {
    final IntRBTreeSet ks = new IntRBTreeSet();

    this.expected.expect(PreconditionViolationException.class);
    this.expected.expectMessage(
      StringContains.containsString("Must provide at least one keyframe"));
    new CaActionTimeline(ks, 60);
  }

  @Test
  public void testOne()
  {
    final IntRBTreeSet ks = new IntRBTreeSet();
    ks.add(0);

    final CaActionTimeline t = new CaActionTimeline(ks, 60);

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 0L, 1.0);
      Assert.assertEquals(0L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(0L, (long) r.keyframeNext());
      Assert.assertEquals(0.0, r.frameCurrentFractional(), 0.0);
    }
  }

  @Test
  public void testLimitsScale1()
  {
    final IntRBTreeSet ks = new IntRBTreeSet();
    ks.add(0);
    ks.add(60);

    final CaActionTimeline t = new CaActionTimeline(ks, 60);

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 0L, 1.0);
      Assert.assertEquals(0L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(60L, (long) r.keyframeNext());
      Assert.assertEquals(0.0, r.frameCurrentFractional(), 0.0);
    }

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 59L, 1.0);
      Assert.assertEquals(59L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(60L, (long) r.keyframeNext());
      Assert.assertEquals(59.0, r.frameCurrentFractional(), 0.0);
    }

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 60L, 1.0);
      Assert.assertEquals(0L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(60L, (long) r.keyframeNext());
      Assert.assertEquals(0.0, r.frameCurrentFractional(), 0.0);
    }
  }

  @Test
  public void testLimitsScale0p5()
  {
    final IntRBTreeSet ks = new IntRBTreeSet();
    ks.add(0);
    ks.add(60);

    final CaActionTimeline t = new CaActionTimeline(ks, 60);

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 0L, 0.5);
      Assert.assertEquals(0L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(60L, (long) r.keyframeNext());
      Assert.assertEquals(0.0, r.frameCurrentFractional(), 0.0);
    }

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 60L, 0.5);
      Assert.assertEquals(30L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(60L, (long) r.keyframeNext());
      Assert.assertEquals(30.0, r.frameCurrentFractional(), 0.0);
    }

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 120L, 0.5);
      Assert.assertEquals(0L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(60L, (long) r.keyframeNext());
      Assert.assertEquals(0.0, r.frameCurrentFractional(), 0.0);
    }
  }

  @Test
  public void testLimitsScale2()
  {
    final IntRBTreeSet ks = new IntRBTreeSet();
    ks.add(0);
    ks.add(60);

    final CaActionTimeline t = new CaActionTimeline(ks, 60);

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 0L, 2.0);
      Assert.assertEquals(0L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(60L, (long) r.keyframeNext());
      Assert.assertEquals(0.0, r.frameCurrentFractional(), 0.0);
    }

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 15L, 2.0);
      Assert.assertEquals(30L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(60L, (long) r.keyframeNext());
      Assert.assertEquals(30.0, r.frameCurrentFractional(), 0.0);
    }

    {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(0L, 30L, 2.0);
      Assert.assertEquals(0L, (long) r.frameCurrentIntegral());
      Assert.assertEquals(0L, (long) r.keyframePrevious());
      Assert.assertEquals(60L, (long) r.keyframeNext());
      Assert.assertEquals(0.0, r.frameCurrentFractional(), 0.0);
    }
  }

  @Test
  public void testSimple()
  {
    final IntRBTreeSet ks = new IntRBTreeSet();
    ks.add(0);
    ks.add(30);
    ks.add(60);

    final CaActionTimeline t = new CaActionTimeline(ks, 60);

    for (int index = 0; index < 600; ++index) {
      final CaActionKeyframeCurrent r =
        t.keyframeCurrent(
          0L,
          (long) index,
          0.1);

      LOG.debug(
        "r: {}: {} ← {} ({}) → {} ({})",
        String.format("%4d", Integer.valueOf(index)),
        Integer.valueOf(r.keyframePrevious()),
        Integer.valueOf(r.frameCurrentIntegral()),
        Double.valueOf(r.frameCurrentFractional()),
        Integer.valueOf(r.keyframeNext()),
        Double.valueOf(r.progress()));
    }
  }
}
