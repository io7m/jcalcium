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

package com.io7m.jcalcium.compiler.api;

import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeletonHash;
import com.io7m.jintegers.Unsigned32;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNodeReadableType;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.collection.SortedMap;
import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * The hash function for skeletons.
 */

public final class CaCompilerSkeletonHashing
{
  private CaCompilerSkeletonHashing()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Create a hash of the given joints.
   *
   * @param joints_by_id The joints organized by their unique ID
   *
   * @return A hash of the joints
   */

  public static CaSkeletonHash create(
    final SortedMap<Integer, JOTreeNodeType<CaJoint>> joints_by_id)
  {
    NullCheck.notNull(joints_by_id, "Joints");

    try {
      final byte[] byte4 = new byte[4];
      final byte[] byte8 = new byte[8];
      final ByteBuffer buffer4 =
        ByteBuffer.wrap(byte4).order(ByteOrder.BIG_ENDIAN);
      final ByteBuffer buffer8 =
        ByteBuffer.wrap(byte8).order(ByteOrder.BIG_ENDIAN);

      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.reset();

      for (final Integer iid : joints_by_id.keySet()) {
        final JOTreeNodeType<CaJoint> node = joints_by_id.get(iid).get();
        final CaJoint joint = node.value();

        final long id = Integer.toUnsignedLong(joint.id());
        Unsigned32.packToBuffer(id, buffer4, 0);
        digest.update(byte4);

        digest.update(joint.name().value().getBytes(StandardCharsets.UTF_8));

        final long parent_id;
        final Optional<JOTreeNodeReadableType<CaJoint>> parent_opt =
          node.parentReadable();
        if (parent_opt.isPresent()) {
          final CaJoint parent = parent_opt.get().value();
          parent_id = Integer.toUnsignedLong(parent.id());
        } else {
          parent_id = Integer.toUnsignedLong(0xffffffff);
        }

        Unsigned32.packToBuffer(parent_id, buffer4, 0);
        digest.update(byte4);

        buffer8.putDouble(0, joint.orientation().getXD());
        digest.update(byte8);
        buffer8.putDouble(0, joint.orientation().getYD());
        digest.update(byte8);
        buffer8.putDouble(0, joint.orientation().getZD());
        digest.update(byte8);
        buffer8.putDouble(0, joint.orientation().getWD());
        digest.update(byte8);

        buffer8.putDouble(0, joint.scale().getXD());
        digest.update(byte8);
        buffer8.putDouble(0, joint.scale().getYD());
        digest.update(byte8);
        buffer8.putDouble(0, joint.scale().getZD());
        digest.update(byte8);

        buffer8.putDouble(0, joint.translation().getXD());
        digest.update(byte8);
        buffer8.putDouble(0, joint.translation().getYD());
        digest.update(byte8);
        buffer8.putDouble(0, joint.translation().getZD());
        digest.update(byte8);
      }

      return CaSkeletonHash.of(
        "SHA2-256", Hex.encodeHexString(digest.digest()));
    } catch (final NoSuchAlgorithmException e) {
      throw new UnsupportedOperationException(e);
    }
  }
}
