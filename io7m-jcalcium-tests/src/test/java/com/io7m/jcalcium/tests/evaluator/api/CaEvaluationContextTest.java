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

package com.io7m.jcalcium.tests.evaluator.api;

import com.io7m.jcalcium.evaluator.api.CaEvaluationContext;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextMatricesType;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextType;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextVectorsType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class CaEvaluationContextTest
{
  private CaEvaluationContextType create()
  {
    return CaEvaluationContext.create();
  }

  @Rule public ExpectedException expected = ExpectedException.none();

  @Test
  public void testMatricesReuse()
  {
    final CaEvaluationContextType c = this.create();

    final CaEvaluationContextMatricesType cc0 = c.newMatrices();
    cc0.close();

    final CaEvaluationContextMatricesType cc1 = c.newMatrices();
    final CaEvaluationContextMatricesType cc2 = c.newMatrices();

    Assert.assertSame(cc0, cc1);
    Assert.assertNotSame(cc0, cc2);
    Assert.assertNotSame(cc1, cc2);
  }

  @Test
  public void testMatricesCloseTwice()
  {
    final CaEvaluationContextType c = this.create();

    final CaEvaluationContextMatricesType cc = c.newMatrices();
    cc.close();
    this.expected.expect(IllegalStateException.class);
    cc.close();
  }

  @Test
  public void testMatricesCloseUse0()
  {
    final CaEvaluationContextType c = this.create();

    final CaEvaluationContextMatricesType cc = c.newMatrices();
    cc.close();
    this.expected.expect(IllegalStateException.class);
    cc.accumulated4x4D();
  }

  @Test
  public void testMatricesCloseUse1()
  {
    final CaEvaluationContextType c = this.create();

    final CaEvaluationContextMatricesType cc = c.newMatrices();
    cc.close();
    this.expected.expect(IllegalStateException.class);
    cc.orientation4x4D();
  }

  @Test
  public void testMatricesCloseUse2()
  {
    final CaEvaluationContextType c = this.create();

    final CaEvaluationContextMatricesType cc = c.newMatrices();
    cc.close();
    this.expected.expect(IllegalStateException.class);
    cc.scale4x4D();
  }

  @Test
  public void testMatricesCloseUse3()
  {
    final CaEvaluationContextType c = this.create();

    final CaEvaluationContextMatricesType cc = c.newMatrices();
    cc.close();
    this.expected.expect(IllegalStateException.class);
    cc.translation4x4D();
  }

  @Test
  public void testVectorsReuse()
  {
    final CaEvaluationContextType c = this.create();

    final CaEvaluationContextVectorsType cc0 = c.newVectors();
    cc0.close();

    final CaEvaluationContextVectorsType cc1 = c.newVectors();
    final CaEvaluationContextVectorsType cc2 = c.newVectors();

    Assert.assertSame(cc0, cc1);
    Assert.assertNotSame(cc0, cc2);
    Assert.assertNotSame(cc1, cc2);
  }

  @Test
  public void testVectorsCloseTwice()
  {
    final CaEvaluationContextType c = this.create();

    final CaEvaluationContextVectorsType cc = c.newVectors();
    cc.close();
    this.expected.expect(IllegalStateException.class);
    cc.close();
  }
}
