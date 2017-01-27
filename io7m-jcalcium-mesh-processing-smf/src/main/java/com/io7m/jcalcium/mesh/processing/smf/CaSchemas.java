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

package com.io7m.jcalcium.mesh.processing.smf;

import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributes;
import com.io7m.jcoords.core.conversion.CAxis;
import com.io7m.jcoords.core.conversion.CAxisSystem;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.smfj.core.SMFAttributeName;
import com.io7m.smfj.core.SMFComponentType;
import com.io7m.smfj.core.SMFCoordinateSystem;
import com.io7m.smfj.core.SMFFaceWindingOrder;
import com.io7m.smfj.core.SMFSchemaIdentifier;
import com.io7m.smfj.validation.api.SMFSchema;
import com.io7m.smfj.validation.api.SMFSchemaAttribute;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Standard SMF schemas for <tt>jcalcium</tt>.
 */

public final class CaSchemas
{
  /**
   * The name of the attribute that is, by convention, used to store joint
   * indices.
   */

  public static final SMFAttributeName JOINT_INDICES_NAME;

  /**
   * The name of the attribute that is, by convention, used to store joint
   * weights.
   */

  public static final SMFAttributeName JOINT_WEIGHTS_NAME;

  /**
   * The name of the attribute that is, by convention, used to store vertex
   * normals.
   */

  public static final SMFAttributeName NORMALS_NAME;

  /**
   * The name of the attribute that is, by convention, used to store vertex
   * positions.
   */

  public static final SMFAttributeName POSITION_NAME;

  /**
   * The name of the attribute that is, by convention, used to store vertex
   * tangents.
   */

  public static final SMFAttributeName TANGENT4_NAME;

  static {
    JOINT_INDICES_NAME =
      SMFAttributeName.of("JOINT_INDICES");
    JOINT_WEIGHTS_NAME =
      SMFAttributeName.of("JOINT_WEIGHTS");
    NORMALS_NAME =
      SMFAttributeName.of(CaMeshDeformableAttributes.NORMAL.name());
    POSITION_NAME =
      SMFAttributeName.of(CaMeshDeformableAttributes.POSITION.name());
    TANGENT4_NAME =
      SMFAttributeName.of(CaMeshDeformableAttributes.TANGENT4.name());
  }

  private CaSchemas()
  {
    throw new UnreachableCodeException();
  }

  /**
   * The schema enforcing the standard conventions assumed by the
   * <tt>jcalcium</tt> package.
   *
   * @return A schema enforcing the standard conventions
   */

  public static SMFSchema standardConventions()
  {
    final SMFSchema.Builder builder = SMFSchema.builder();
    builder.setSchemaIdentifier(
      SMFSchemaIdentifier.of(0x494F374D, 0x63610000, 1, 0));

    builder.setRequiredCoordinateSystem(SMFCoordinateSystem.of(
      CAxisSystem.of(
        CAxis.AXIS_POSITIVE_X,
        CAxis.AXIS_POSITIVE_Y,
        CAxis.AXIS_NEGATIVE_Z),
      SMFFaceWindingOrder.FACE_WINDING_ORDER_COUNTER_CLOCKWISE));
    builder.setAllowExtraAttributes(true);

    final SMFSchemaAttribute position =
      SMFSchemaAttribute.of(
        POSITION_NAME,
        Optional.of(SMFComponentType.ELEMENT_TYPE_FLOATING),
        OptionalInt.of(3),
        OptionalInt.empty());

    final SMFSchemaAttribute joint_indices =
      SMFSchemaAttribute.of(
        JOINT_INDICES_NAME,
        Optional.of(SMFComponentType.ELEMENT_TYPE_INTEGER_UNSIGNED),
        OptionalInt.of(4),
        OptionalInt.empty());

    final SMFSchemaAttribute joint_weights =
      SMFSchemaAttribute.of(
        JOINT_WEIGHTS_NAME,
        Optional.of(SMFComponentType.ELEMENT_TYPE_FLOATING),
        OptionalInt.of(4),
        OptionalInt.empty());

    builder.putRequiredAttributes(position.name(), position);
    builder.putRequiredAttributes(joint_indices.name(), joint_indices);
    builder.putRequiredAttributes(joint_weights.name(), joint_weights);

    final SMFSchemaAttribute normal =
      SMFSchemaAttribute.of(
        NORMALS_NAME,
        Optional.of(SMFComponentType.ELEMENT_TYPE_FLOATING),
        OptionalInt.of(3),
        OptionalInt.empty());

    final SMFSchemaAttribute tangent4 =
      SMFSchemaAttribute.of(
        TANGENT4_NAME,
        Optional.of(SMFComponentType.ELEMENT_TYPE_FLOATING),
        OptionalInt.of(4),
        OptionalInt.empty());

    builder.putOptionalAttributes(normal.name(), normal);
    builder.putOptionalAttributes(tangent4.name(), tangent4);
    return builder.build();
  }
}
