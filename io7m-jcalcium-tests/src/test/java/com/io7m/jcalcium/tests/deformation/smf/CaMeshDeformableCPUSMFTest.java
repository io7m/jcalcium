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

package com.io7m.jcalcium.tests.deformation.smf;

import com.io7m.jaffirm.core.PreconditionViolationException;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorKind;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorReadable3Type;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorReadable4Type;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorReadableType;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorType;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeSemantic;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeSourceSelection;
import com.io7m.jcalcium.mesh.deformation.smf.CaMeshDeformableCPUSMFAbstract;
import com.io7m.jcalcium.mesh.deformation.smf.CaMeshDeformableCPUSMFConfiguration;
import com.io7m.jcalcium.mesh.deformation.smf.CaMeshDeformableCPUSMFEventsType;
import com.io7m.jcalcium.mesh.deformation.smf.CaMeshDeformableCPUSMFPackedAttributeSet;
import com.io7m.jcalcium.mesh.deformation.smf.CaMeshDeformableCPUSMFProvider;
import com.io7m.jcalcium.mesh.deformation.smf.CaSetAuxiliaryType;
import com.io7m.jcalcium.mesh.deformation.smf.CaSetJointType;
import com.io7m.jcalcium.mesh.deformation.smf.CaSetSourceType;
import com.io7m.jcalcium.mesh.deformation.smf.CaSetTargetType;
import com.io7m.jpra.runtime.java.JPRACursor1DType;
import com.io7m.jtensors.VectorM3D;
import com.io7m.jtensors.VectorM3L;
import com.io7m.jtensors.VectorM4D;
import com.io7m.jtensors.VectorM4L;
import com.io7m.smfj.bytebuffer.SMFByteBufferCursors;
import com.io7m.smfj.bytebuffer.SMFByteBufferFloat4Type;
import com.io7m.smfj.bytebuffer.SMFByteBufferIntegerUnsigned3Type;
import com.io7m.smfj.bytebuffer.SMFByteBufferIntegerUnsigned4Type;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackedAttribute;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackedTriangles;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackingConfiguration;
import com.io7m.smfj.core.SMFAttributeName;
import com.io7m.smfj.core.SMFErrorType;
import com.io7m.smfj.core.SMFHeader;
import com.io7m.smfj.core.SMFTriangles;
import com.io7m.smfj.format.text.SMFFormatText;
import com.io7m.smfj.parser.api.SMFParserEventsMeta;
import com.io7m.smfj.parser.api.SMFParserEventsMetaType;
import com.io7m.smfj.parser.api.SMFParserProviderType;
import com.io7m.smfj.validation.api.SMFSchemaValidatorType;
import com.io7m.smfj.validation.main.SMFSchemaValidator;
import javaslang.collection.List;
import javaslang.collection.SortedMap;
import javaslang.control.Validation;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class CaMeshDeformableCPUSMFTest
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaMeshDeformableCPUSMFTest.class);
  }

  private static final class CPUMesh extends CaMeshDeformableCPUSMFAbstract
  {
    private final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetJointType> joint_data;
    private final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetSourceType> source_data;
    private final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetTargetType> target_data;
    private final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetAuxiliaryType> aux_data;
    private final SMFByteBufferPackedTriangles triangle_data;

    public CPUMesh(
      final SortedMap<String, CaMeshDeformableAttributeCursorType> in_target_cursors,
      final SortedMap<String, CaMeshDeformableAttributeCursorReadableType> in_source_cursors,
      final JPRACursor1DType<SMFByteBufferIntegerUnsigned4Type> in_joint_indices_cursor,
      final JPRACursor1DType<SMFByteBufferFloat4Type> in_joint_weights_cursor,
      final long in_vertex_count,
      final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetJointType> in_joint_data,
      final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetSourceType> in_source_data,
      final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetTargetType> in_target_data,
      final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetAuxiliaryType> in_aux_data,
      final SMFByteBufferPackedTriangles in_triangle_data)
    {
      super(
        in_target_cursors,
        in_source_cursors,
        in_joint_indices_cursor,
        in_joint_weights_cursor,
        in_vertex_count);

      this.joint_data = in_joint_data;
      this.source_data = in_source_data;
      this.target_data = in_target_data;
      this.aux_data = in_aux_data;
      this.triangle_data = in_triangle_data;
    }
  }

  private static class CPUMeshEvents
    implements CaMeshDeformableCPUSMFEventsType<CPUMesh>
  {
    @Override
    public CPUMesh onCreated(
      final SMFHeader header,
      final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetJointType> joint_data,
      final JPRACursor1DType<SMFByteBufferIntegerUnsigned4Type> cursor_joint_index,
      final JPRACursor1DType<SMFByteBufferFloat4Type> cursor_joint_weight,
      final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetSourceType> source_data,
      final SortedMap<String, CaMeshDeformableAttributeCursorReadableType> source_cursors,
      final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetTargetType> target_data,
      final SortedMap<String, CaMeshDeformableAttributeCursorType> target_cursors,
      final CaMeshDeformableCPUSMFPackedAttributeSet<CaSetAuxiliaryType> aux_data,
      final SMFByteBufferPackedTriangles triangle_data)
    {
      return new CPUMesh(
        target_cursors,
        source_cursors,
        cursor_joint_index,
        cursor_joint_weight,
        header.vertexCount(),
        joint_data,
        source_data,
        target_data,
        aux_data,
        triangle_data);
    }
  }

  @Test
  public void testEmpty()
    throws Exception
  {
    final SMFParserProviderType provider = new SMFFormatText();
    final SMFSchemaValidatorType validator = new SMFSchemaValidator();
    final SMFParserEventsMetaType meta = SMFParserEventsMeta.ignore();
    final CaMeshDeformableCPUSMFConfiguration config =
      CaMeshDeformableCPUSMFConfiguration.builder().build();

    final Path path = Paths.get(
      "/com/io7m/jcalcium/tests/deformation/smf/empty.smft");

    try (final InputStream stream =
           CaMeshDeformableCPUSMFTest.class.getResourceAsStream(path.toString())) {

      final Validation<List<SMFErrorType>, CPUMesh> results =
        CaMeshDeformableCPUSMFProvider.createFromStream(
          provider, validator, meta, config, new CPUMeshEvents(), path, stream);
      Assert.assertTrue(results.isValid());
      final CPUMesh mesh = results.get();

      {
        final SMFByteBufferPackingConfiguration aux_config =
          mesh.aux_data.set().configuration();
        final SortedMap<SMFAttributeName, SMFByteBufferPackedAttribute> aux_by_name =
          aux_config.packedAttributesByName();
        LOG.debug("aux: {}", aux_by_name.keySet());

        Assert.assertTrue(aux_by_name.containsKey(SMFAttributeName.of("POSITION")));
        Assert.assertTrue(aux_by_name.containsKey(SMFAttributeName.of("NORMAL")));
        Assert.assertEquals(2L, (long) aux_by_name.size());
      }

      {
        final SMFByteBufferPackingConfiguration source_config =
          mesh.source_data.set().configuration();
        final SortedMap<SMFAttributeName, SMFByteBufferPackedAttribute> source_by_name =
          source_config.packedAttributesByName();
        LOG.debug("source: {}", source_by_name.keySet());

        Assert.assertEquals(0L, (long) source_by_name.size());
        Assert.assertEquals(0L, (long) mesh.meshSourceCursors().size());
        Assert.assertEquals(0L, (long) mesh.meshTargetCursors().size());
      }

      {
        final SMFByteBufferPackingConfiguration joint_config =
          mesh.joint_data.set().configuration();
        final SortedMap<SMFAttributeName, SMFByteBufferPackedAttribute> joint_by_name =
          joint_config.packedAttributesByName();
        LOG.debug("joints: {}", joint_by_name.keySet());

        Assert.assertTrue(joint_by_name.containsKey(SMFAttributeName.of(
          "JOINT_INDICES")));
        Assert.assertTrue(joint_by_name.containsKey(SMFAttributeName.of(
          "JOINT_WEIGHTS")));
        Assert.assertEquals(
          2L, (long) joint_by_name.size());
      }

      {
        final VectorM3L out = new VectorM3L();
        final JPRACursor1DType<SMFByteBufferIntegerUnsigned3Type> cursor =
          SMFByteBufferCursors.createUnsigned3Raw(
            mesh.triangle_data.byteBuffer(),
            mesh.triangle_data.triangleIndexSizeBits(),
            0,
            mesh.triangle_data.triangleSizeOctets());

        final SMFByteBufferIntegerUnsigned3Type view = cursor.getElementView();
        view.get3UL(out);

        Assert.assertEquals(0L, out.getXL());
        Assert.assertEquals(1L, out.getYL());
        Assert.assertEquals(2L, out.getZL());
      }

      {
        final VectorM4L out = new VectorM4L();
        mesh.jointIndicesForVertex(0L, out);
        Assert.assertEquals(0L, out.getXL());
        Assert.assertEquals(1L, out.getYL());
        Assert.assertEquals(2L, out.getZL());
        Assert.assertEquals(3L, out.getWL());
      }

      {
        final VectorM4D out = new VectorM4D();
        mesh.jointWeightsForVertex(0L, out);
        Assert.assertEquals(1.0, out.getXD(), 0.0);
        Assert.assertEquals(0.0, out.getYD(), 0.0);
        Assert.assertEquals(0.0, out.getZD(), 0.0);
        Assert.assertEquals(0.0, out.getWD(), 0.0);
      }
    }
  }

  @Rule public ExpectedException expected = ExpectedException.none();

  @Test
  public void testEmptyOutOfRangeJointIndices()
    throws Exception
  {
    final SMFParserProviderType provider = new SMFFormatText();
    final SMFSchemaValidatorType validator = new SMFSchemaValidator();
    final SMFParserEventsMetaType meta = SMFParserEventsMeta.ignore();
    final CaMeshDeformableCPUSMFConfiguration config =
      CaMeshDeformableCPUSMFConfiguration.builder().build();

    final Path path = Paths.get(
      "/com/io7m/jcalcium/tests/deformation/smf/empty.smft");

    try (final InputStream stream =
           CaMeshDeformableCPUSMFTest.class.getResourceAsStream(path.toString())) {

      final Validation<List<SMFErrorType>, CPUMesh> results =
        CaMeshDeformableCPUSMFProvider.createFromStream(
          provider, validator, meta, config, new CPUMeshEvents(), path, stream);
      Assert.assertTrue(results.isValid());
      final CPUMesh mesh = results.get();

      final VectorM4L out = new VectorM4L();
      this.expected.expect(PreconditionViolationException.class);
      this.expected.expectMessage(StringContains.containsString(
        "Vertex must exist"));
      mesh.jointIndicesForVertex(1L, out);
    }

    Assert.fail();
  }

  @Test
  public void testEmptyOutOfRangeJointWeights()
    throws Exception
  {
    final SMFParserProviderType provider = new SMFFormatText();
    final SMFSchemaValidatorType validator = new SMFSchemaValidator();
    final SMFParserEventsMetaType meta = SMFParserEventsMeta.ignore();
    final CaMeshDeformableCPUSMFConfiguration config =
      CaMeshDeformableCPUSMFConfiguration.builder().build();

    final Path path = Paths.get(
      "/com/io7m/jcalcium/tests/deformation/smf/empty.smft");

    try (final InputStream stream =
           CaMeshDeformableCPUSMFTest.class.getResourceAsStream(path.toString())) {

      final Validation<List<SMFErrorType>, CPUMesh> results =
        CaMeshDeformableCPUSMFProvider.createFromStream(
          provider, validator, meta, config, new CPUMeshEvents(), path, stream);
      Assert.assertTrue(results.isValid());
      final CPUMesh mesh = results.get();

      final VectorM4D out = new VectorM4D();
      this.expected.expect(PreconditionViolationException.class);
      this.expected.expectMessage(StringContains.containsString(
        "Vertex must exist"));
      mesh.jointWeightsForVertex(1L, out);
    }

    Assert.fail();
  }

  @Test
  public void testStandard()
    throws Exception
  {
    final SMFParserProviderType provider = new SMFFormatText();
    final SMFSchemaValidatorType validator = new SMFSchemaValidator();
    final SMFParserEventsMetaType meta = SMFParserEventsMeta.ignore();
    final CaMeshDeformableCPUSMFConfiguration config =
      CaMeshDeformableCPUSMFConfiguration.builder()
        .addSourceAttributes(
          CaMeshDeformableAttributeSourceSelection.of(
            "POSITION",
            CaMeshDeformableAttributeSemantic.POSITION,
            CaMeshDeformableAttributeCursorKind.CURSOR_FLOAT_3))
        .addSourceAttributes(
          CaMeshDeformableAttributeSourceSelection.of(
            "NORMAL",
            CaMeshDeformableAttributeSemantic.DIRECTION,
            CaMeshDeformableAttributeCursorKind.CURSOR_FLOAT_3))
        .addSourceAttributes(
          CaMeshDeformableAttributeSourceSelection.of(
            "TANGENT4",
            CaMeshDeformableAttributeSemantic.DIRECTION,
            CaMeshDeformableAttributeCursorKind.CURSOR_FLOAT_4))
        .build();

    final Path path = Paths.get(
      "/com/io7m/jcalcium/tests/deformation/smf/standard.smft");

    try (final InputStream stream =
           CaMeshDeformableCPUSMFTest.class.getResourceAsStream(path.toString())) {

      final Validation<List<SMFErrorType>, CPUMesh> results =
        CaMeshDeformableCPUSMFProvider.createFromStream(
          provider, validator, meta, config, new CPUMeshEvents(), path, stream);
      Assert.assertTrue(results.isValid());
      final CPUMesh mesh = results.get();

      {
        final SMFByteBufferPackingConfiguration aux_config =
          mesh.aux_data.set().configuration();
        final SortedMap<SMFAttributeName, SMFByteBufferPackedAttribute> aux_by_name =
          aux_config.packedAttributesByName();
        LOG.debug("aux: {}", aux_by_name.keySet());

        Assert.assertEquals(0L, (long) aux_by_name.size());
      }

      {
        final SMFByteBufferPackingConfiguration source_config =
          mesh.source_data.set().configuration();
        final SortedMap<SMFAttributeName, SMFByteBufferPackedAttribute> source_by_name =
          source_config.packedAttributesByName();
        LOG.debug("source: {}", source_by_name.keySet());

        Assert.assertTrue(source_by_name.containsKey(SMFAttributeName.of(
          "POSITION")));
        Assert.assertTrue(source_by_name.containsKey(SMFAttributeName.of(
          "NORMAL")));
        Assert.assertTrue(source_by_name.containsKey(SMFAttributeName.of(
          "TANGENT4")));

        Assert.assertEquals(3L, (long) source_by_name.size());
        Assert.assertEquals(3L, (long) mesh.meshSourceCursors().size());
        Assert.assertEquals(3L, (long) mesh.meshTargetCursors().size());

        Assert.assertEquals(
          (long) mesh.source_data.set().byteBuffer().capacity(),
          (long) mesh.target_data.set().byteBuffer().capacity());

        {
          final CaMeshDeformableAttributeCursorReadable3Type c =
            (CaMeshDeformableAttributeCursorReadable3Type)
              mesh.meshSourceCursors().get("POSITION").get();
          Assert.assertEquals(
            CaMeshDeformableAttributeSemantic.POSITION,  c.semantic());
          Assert.assertEquals(0L, c.vertex());

          final VectorM3D out = new VectorM3D();
          c.get3D(out);
          Assert.assertEquals(0.0, out.getXD(), 0.0);
          Assert.assertEquals(1.0, out.getYD(), 0.0);
          Assert.assertEquals(2.0, out.getZD(), 0.0);
        }

        {
          final CaMeshDeformableAttributeCursorReadable3Type c =
            (CaMeshDeformableAttributeCursorReadable3Type)
              mesh.meshSourceCursors().get("NORMAL").get();
          Assert.assertEquals(
            CaMeshDeformableAttributeSemantic.DIRECTION,  c.semantic());
          Assert.assertEquals(0L, c.vertex());

          final VectorM3D out = new VectorM3D();
          c.get3D(out);
          Assert.assertEquals(0.0, out.getXD(), 0.0);
          Assert.assertEquals(0.0, out.getYD(), 0.0);
          Assert.assertEquals(1.0, out.getZD(), 0.0);
        }

        {
          final CaMeshDeformableAttributeCursorReadable4Type c =
            (CaMeshDeformableAttributeCursorReadable4Type)
              mesh.meshSourceCursors().get("TANGENT4").get();
          Assert.assertEquals(
            CaMeshDeformableAttributeSemantic.DIRECTION,  c.semantic());
          Assert.assertEquals(0L, c.vertex());

          final VectorM4D out = new VectorM4D();
          c.get4D(out);
          Assert.assertEquals(1.0, out.getXD(), 0.0);
          Assert.assertEquals(0.0, out.getYD(), 0.0);
          Assert.assertEquals(0.0, out.getZD(), 0.0);
          Assert.assertEquals(1.0, out.getWD(), 0.0);
        }
      }

      {
        final SMFByteBufferPackingConfiguration joint_config =
          mesh.joint_data.set().configuration();
        final SortedMap<SMFAttributeName, SMFByteBufferPackedAttribute> joint_by_name =
          joint_config.packedAttributesByName();
        LOG.debug("joints: {}", joint_by_name.keySet());

        Assert.assertTrue(joint_by_name.containsKey(SMFAttributeName.of(
          "JOINT_INDICES")));
        Assert.assertTrue(joint_by_name.containsKey(SMFAttributeName.of(
          "JOINT_WEIGHTS")));
        Assert.assertEquals(
          2L, (long) joint_by_name.size());
      }
    }
  }

  @Test
  public void testBadAllocationJoint()
    throws Exception
  {
    final SMFParserProviderType provider = new SMFFormatText();
    final SMFSchemaValidatorType validator = new SMFSchemaValidator();
    final SMFParserEventsMetaType meta = SMFParserEventsMeta.ignore();
    final CaMeshDeformableCPUSMFConfiguration config =
      CaMeshDeformableCPUSMFConfiguration.builder().build();

    final Path path = Paths.get(
      "/com/io7m/jcalcium/tests/deformation/smf/standard.smft");

    try (final InputStream stream =
           CaMeshDeformableCPUSMFTest.class.getResourceAsStream(path.toString())) {

      final Validation<List<SMFErrorType>, CPUMesh> results =
        CaMeshDeformableCPUSMFProvider.createFromStream(
          provider, validator, meta, config, new CPUMeshEvents() {
            @Override
            public ByteBuffer onAllocateJointBuffer(
              final long size_octets)
            {
              return ByteBuffer.allocate(0);
            }
          }, path, stream);
      Assert.assertFalse(results.isValid());
    }
  }

  @Test
  public void testBadAllocationSource()
    throws Exception
  {
    final SMFParserProviderType provider = new SMFFormatText();
    final SMFSchemaValidatorType validator = new SMFSchemaValidator();
    final SMFParserEventsMetaType meta = SMFParserEventsMeta.ignore();
    final CaMeshDeformableCPUSMFConfiguration config =
      CaMeshDeformableCPUSMFConfiguration.builder().build();

    final Path path = Paths.get(
      "/com/io7m/jcalcium/tests/deformation/smf/standard.smft");

    try (final InputStream stream =
           CaMeshDeformableCPUSMFTest.class.getResourceAsStream(path.toString())) {

      final Validation<List<SMFErrorType>, CPUMesh> results =
        CaMeshDeformableCPUSMFProvider.createFromStream(
          provider, validator, meta, config, new CPUMeshEvents() {
            @Override
            public ByteBuffer onAllocateSourceBuffer(
              final long size_octets)
            {
              return ByteBuffer.allocate(1000);
            }
          }, path, stream);
      Assert.assertFalse(results.isValid());
    }
  }

  @Test
  public void testBadAllocationTarget()
    throws Exception
  {
    final SMFParserProviderType provider = new SMFFormatText();
    final SMFSchemaValidatorType validator = new SMFSchemaValidator();
    final SMFParserEventsMetaType meta = SMFParserEventsMeta.ignore();
    final CaMeshDeformableCPUSMFConfiguration config =
      CaMeshDeformableCPUSMFConfiguration.builder().build();

    final Path path = Paths.get(
      "/com/io7m/jcalcium/tests/deformation/smf/standard.smft");

    try (final InputStream stream =
           CaMeshDeformableCPUSMFTest.class.getResourceAsStream(path.toString())) {

      final Validation<List<SMFErrorType>, CPUMesh> results =
        CaMeshDeformableCPUSMFProvider.createFromStream(
          provider, validator, meta, config, new CPUMeshEvents() {
            @Override
            public ByteBuffer onAllocateTargetBuffer(
              final long size_octets)
            {
              return ByteBuffer.allocate(1000);
            }
          }, path, stream);
      Assert.assertFalse(results.isValid());
    }
  }

  @Test
  public void testBadAllocationAux()
    throws Exception
  {
    final SMFParserProviderType provider = new SMFFormatText();
    final SMFSchemaValidatorType validator = new SMFSchemaValidator();
    final SMFParserEventsMetaType meta = SMFParserEventsMeta.ignore();
    final CaMeshDeformableCPUSMFConfiguration config =
      CaMeshDeformableCPUSMFConfiguration.builder().build();

    final Path path = Paths.get(
      "/com/io7m/jcalcium/tests/deformation/smf/standard.smft");

    try (final InputStream stream =
           CaMeshDeformableCPUSMFTest.class.getResourceAsStream(path.toString())) {

      final Validation<List<SMFErrorType>, CPUMesh> results =
        CaMeshDeformableCPUSMFProvider.createFromStream(
          provider, validator, meta, config, new CPUMeshEvents() {
            @Override
            public ByteBuffer onAllocateAuxiliaryBuffer(
              final long size_octets)
            {
              return ByteBuffer.allocate(0);
            }
          }, path, stream);
      Assert.assertFalse(results.isValid());
    }
  }

  @Test
  public void testBadAllocationTriangle()
    throws Exception
  {
    final SMFParserProviderType provider = new SMFFormatText();
    final SMFSchemaValidatorType validator = new SMFSchemaValidator();
    final SMFParserEventsMetaType meta = SMFParserEventsMeta.ignore();
    final CaMeshDeformableCPUSMFConfiguration config =
      CaMeshDeformableCPUSMFConfiguration.builder().build();

    final Path path = Paths.get(
      "/com/io7m/jcalcium/tests/deformation/smf/standard.smft");

    try (final InputStream stream =
           CaMeshDeformableCPUSMFTest.class.getResourceAsStream(path.toString())) {

      final Validation<List<SMFErrorType>, CPUMesh> results =
        CaMeshDeformableCPUSMFProvider.createFromStream(
          provider, validator, meta, config, new CPUMeshEvents() {
            @Override
            public ByteBuffer onAllocateTriangleBuffer(
              final SMFTriangles triangles,
              final long size_octets)
            {
              return ByteBuffer.allocate(10000);
            }
          }, path, stream);
      Assert.assertFalse(results.isValid());
    }
  }
}
