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

package com.io7m.jcalcium.compiler.api;

/**
 * Compilation error codes.
 */

public enum CaCompileErrorCode
{
  /**
   * A joint specifies a nonexistent parent.
   */

  ERROR_JOINT_NONEXISTENT_PARENT,

  /**
   * The skeleton does not have a root joint.
   */

  ERROR_JOINT_NO_ROOT,

  /**
   * A cycle was detected in the joints of a skeleton.
   */

  ERROR_JOINT_CYCLE,

  /**
   * The skeleton has multiple root joints.
   */

  ERROR_MULTIPLE_ROOT_JOINTS,

  /**
   * An action supplies a nonexistent joint.
   */

  ERROR_ACTION_INVALID_BONE,

  /**
   * Two keyframes on a curve have the same index.
   */

  ERROR_ACTION_DUPLICATE_KEYFRAME,

  /**
   * An action has multiple curves of the same type for a joint.
   */

  ERROR_ACTION_MULTIPLE_CURVES_SAME_TYPE,

  /**
   * The root joint of a skeleton has a non-identity transform.
   */

  ERROR_JOINT_ROOT_NOT_IDENTITY_TRANSFORM,

  /**
   * An action has an invalid frames per second count.
   */

  ERROR_ACTION_INVALID_FPS
}
