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

package com.io7m.jcalcium.evaluator.api;

/**
 * <p>The type of evaluation contexts.</p>
 *
 * <p>An evaluation context provides a central place for implementations to
 * request reusable temporary storage. For example, an evaluator that serially
 * processes hundreds of joints may require several temporary matrices per
 * joint. The evaluator must either declare all storage itself, increasing the
 * memory usage required on a per-evaluator basis, or it must declare several
 * local temporary matrices and hope that the virtual machine's escape analysis
 * eliminates them. Instead, the evaluator can share an explicit evaluation
 * context with all other evaluators and can request temporary storage for
 * matrices on demand in a manner that allows the context to reuse the matrices
 * without performing extra allocations.</p>
 */

public interface CaEvaluationContextType
{
  /**
   * Create a new set of matrices. The method is allowed to return the same
   * instance as has been returned previously if and only if the {@link
   * java.lang.AutoCloseable#close()} method has been called on the returned
   * instance.
   *
   * @return A new set of matrices
   */

  CaEvaluationContextMatricesType newMatrices();
}
