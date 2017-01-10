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

import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.definitions.CaFormatVersion;
import com.io7m.jcalcium.format.protobuf3.CaProtobuf3FormatProvider;
import com.io7m.jcalcium.mesh.processing.smf.CaFilterCommandCompileMeshWeights;
import com.io7m.jcalcium.serializer.api.CaCompiledSerializerType;
import com.io7m.jcoords.core.conversion.CAxis;
import com.io7m.jcoords.core.conversion.CAxisSystem;
import com.io7m.jorchard.core.JOTreeNode;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import com.io7m.smfj.core.SMFAttribute;
import com.io7m.smfj.core.SMFAttributeName;
import com.io7m.smfj.core.SMFCoordinateSystem;
import com.io7m.smfj.core.SMFFaceWindingOrder;
import com.io7m.smfj.core.SMFHeader;
import com.io7m.smfj.core.SMFSchemaIdentifier;
import com.io7m.smfj.parser.api.SMFParseError;
import com.io7m.smfj.processing.api.SMFAttributeArrayFloating1;
import com.io7m.smfj.processing.api.SMFAttributeArrayFloating2;
import com.io7m.smfj.processing.api.SMFFilterCommandContext;
import com.io7m.smfj.processing.api.SMFMemoryMesh;
import com.io7m.smfj.processing.api.SMFMemoryMeshFilterType;
import com.io7m.smfj.processing.api.SMFProcessingError;
import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.TreeMap;
import javaslang.collection.Vector;
import javaslang.control.Validation;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.io7m.smfj.core.SMFComponentType.ELEMENT_TYPE_FLOATING;

public final class CaFilterCommandCompileMeshWeightsTest extends
  CaMemoryMeshFilterContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaFilterCommandCompileMeshWeightsTest.class);
  }

  private static SMFHeader baseHeader(
    final List<SMFAttribute> attributes)
  {
    final SMFCoordinateSystem coords =
      SMFCoordinateSystem.of(
        CAxisSystem.of(
          CAxis.AXIS_POSITIVE_X,
          CAxis.AXIS_POSITIVE_Y,
          CAxis.AXIS_NEGATIVE_Z),
        SMFFaceWindingOrder.FACE_WINDING_ORDER_COUNTER_CLOCKWISE);
    final SMFSchemaIdentifier schema =
      SMFSchemaIdentifier.builder().build();
    return SMFHeader.of(0L, 16L, 0L, schema, coords, attributes, 0L);
  }

  @Test
  public void testParseIncorrect0()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.empty());
    Assert.assertTrue(r.isInvalid());
  }

  @Test
  public void testParseIncorrect1()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.of("a"));
    Assert.assertTrue(r.isInvalid());
  }

  @Test
  public void testParseIncorrect2()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.of("a", "b"));
    Assert.assertTrue(r.isInvalid());
  }

  @Test
  public void testParseIncorrect3()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.of("a", "b", "c"));
    Assert.assertTrue(r.isInvalid());
  }

  @Test
  public void testParseIncorrect4()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.of("a", "b", "c", "d", "e"));
    Assert.assertTrue(r.isInvalid());
  }

  @Test
  public void testParseIncorrect6()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.of("a", "@#<", "c", "d"));
    Assert.assertTrue(r.isInvalid());
  }

  @Test
  public void testParseIncorrect7()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.of("a", "b", "@#<", "d"));
    Assert.assertTrue(r.isInvalid());
  }

  @Test
  public void testParseIncorrect8()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.of("a", "b", "c", "["));
    Assert.assertTrue(r.isInvalid());
  }

  @Test
  public void testParseCorrect0()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.of("a", "b", "c", "d"));
    Assert.assertTrue(r.isValid());
  }

  @Test
  public void testParseCorrect1()
  {
    final Validation<List<SMFParseError>, SMFMemoryMeshFilterType> r =
      CaFilterCommandCompileMeshWeights.parse(
        Optional.empty(),
        1,
        List.of("a", "b", "c", " "));
    Assert.assertTrue(r.isValid());
  }

  @Test
  public void testAttributeCollisionIndices()
    throws Exception
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);

    final CaSkeleton skeleton =
      CaSkeleton.of(CaSkeletonName.of("skeleton"), joints, TreeMap.empty());

    final Path path =
      this.writeSkeleton(skeleton);

    final SMFAttributeName attr_indices_output =
      SMFAttributeName.of("root");
    final SMFAttributeName attr_weights_output =
      SMFAttributeName.of("weights");

    final SMFMemoryMeshFilterType filter =
      CaFilterCommandCompileMeshWeights.create(
        path, attr_indices_output, attr_weights_output, Pattern.compile(" "));

    final List<SMFAttribute> attributes =
      List.of(
        SMFAttribute.of(attr_indices_output, ELEMENT_TYPE_FLOATING, 1, 32));

    final SMFHeader header =
      baseHeader(attributes);

    final SMFMemoryMesh mesh =
      SMFMemoryMesh.builder()
        .setHeader(header)
        .setArrays(HashMap.ofEntries(
          Tuple.of(
            attr_indices_output,
            SMFAttributeArrayFloating1.of(Vector.empty()))
        ))
        .setTriangles(Vector.empty())
        .setMetadata(Vector.empty())
        .build();

    final Path root = this.filesystem.getRootDirectories().iterator().next();
    final Validation<List<SMFProcessingError>, SMFMemoryMesh> r =
      filter.filter(SMFFilterCommandContext.of(root, root), mesh);

    Assert.assertTrue(r.isInvalid());
    Assert.assertThat(
      r.getError().get(0).message(),
      StringContains.containsString("Output attribute already exists."));

    r.getError().forEach(x -> LOG.error("{}", x.message()));
  }

  @Test
  public void testAttributeCollisionWeights()
    throws Exception
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);

    final CaSkeleton skeleton =
      CaSkeleton.of(CaSkeletonName.of("skeleton"), joints, TreeMap.empty());

    final Path path =
      this.writeSkeleton(skeleton);

    final SMFAttributeName attr_indices_output =
      SMFAttributeName.of("root");
    final SMFAttributeName attr_weights_output =
      SMFAttributeName.of("weights");

    final SMFMemoryMeshFilterType filter =
      CaFilterCommandCompileMeshWeights.create(
        path, attr_indices_output, attr_weights_output, Pattern.compile(" "));

    final List<SMFAttribute> attributes =
      List.of(
        SMFAttribute.of(attr_weights_output, ELEMENT_TYPE_FLOATING, 1, 32));

    final SMFHeader header =
      baseHeader(attributes);

    final SMFMemoryMesh mesh =
      SMFMemoryMesh.builder()
        .setHeader(header)
        .setArrays(HashMap.ofEntries(
          Tuple.of(
            attr_weights_output,
            SMFAttributeArrayFloating1.of(Vector.empty()))
        ))
        .setTriangles(Vector.empty())
        .setMetadata(Vector.empty())
        .build();

    final Path root = this.filesystem.getRootDirectories().iterator().next();
    final Validation<List<SMFProcessingError>, SMFMemoryMesh> r =
      filter.filter(SMFFilterCommandContext.of(root, root), mesh);

    Assert.assertTrue(r.isInvalid());
    Assert.assertThat(
      r.getError().get(0).message(),
      StringContains.containsString("Output attribute already exists."));

    r.getError().forEach(x -> LOG.error("{}", x.message()));
  }

  @Test
  public void testAttributeWrongType()
    throws Exception
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);

    final CaSkeleton skeleton =
      CaSkeleton.of(CaSkeletonName.of("skeleton"), joints, TreeMap.empty());

    final Path path =
      this.writeSkeleton(skeleton);

    final SMFAttributeName attr_root_input =
      SMFAttributeName.of("GROUP:root");
    final SMFAttributeName attr_indices_output =
      SMFAttributeName.of("joint_indices");
    final SMFAttributeName attr_weights_output =
      SMFAttributeName.of("joint_weights");

    final SMFMemoryMeshFilterType filter =
      CaFilterCommandCompileMeshWeights.create(
        path,
        attr_indices_output,
        attr_weights_output,
        Pattern.compile(".*"));

    final List<SMFAttribute> attributes =
      List.of(SMFAttribute.of(attr_root_input, ELEMENT_TYPE_FLOATING, 2, 32));

    final SMFHeader header =
      baseHeader(attributes);

    final SMFMemoryMesh mesh =
      SMFMemoryMesh.builder()
        .setHeader(header)
        .setArrays(HashMap.ofEntries(
          Tuple.of(
            attr_root_input,
            SMFAttributeArrayFloating2.of(Vector.empty()))
        ))
        .setTriangles(Vector.empty())
        .setMetadata(Vector.empty())
        .build();

    final Path root = this.filesystem.getRootDirectories().iterator().next();
    final Validation<List<SMFProcessingError>, SMFMemoryMesh> r =
      filter.filter(SMFFilterCommandContext.of(root, root), mesh);

    Assert.assertTrue(r.isInvalid());
    Assert.assertThat(
      r.getError().get(0).message(),
      StringContains.containsString("Source attribute is of the wrong type."));

    r.getError().forEach(x -> LOG.error("{}", x.message()));
  }

  @Test
  public void testAttributeNoneMatched()
    throws Exception
  {
    final CaJoint joint_root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> joints = JOTreeNode.create(joint_root);

    final CaSkeleton skeleton =
      CaSkeleton.of(CaSkeletonName.of("skeleton"), joints, TreeMap.empty());

    final Path path =
      this.writeSkeleton(skeleton);

    final SMFAttributeName attr_root_input =
      SMFAttributeName.of("GROUP:root");
    final SMFAttributeName attr_indices_output =
      SMFAttributeName.of("joint_indices");
    final SMFAttributeName attr_weights_output =
      SMFAttributeName.of("joint_weights");

    final SMFMemoryMeshFilterType filter =
      CaFilterCommandCompileMeshWeights.create(
        path,
        attr_indices_output,
        attr_weights_output,
        Pattern.compile("notmatching"));

    final List<SMFAttribute> attributes =
      List.of(SMFAttribute.of(attr_root_input, ELEMENT_TYPE_FLOATING, 1, 32));

    final SMFHeader header =
      baseHeader(attributes);

    final SMFMemoryMesh mesh =
      SMFMemoryMesh.builder()
        .setHeader(header)
        .setArrays(HashMap.ofEntries(
          Tuple.of(
            attr_root_input,
            SMFAttributeArrayFloating1.of(Vector.empty()))
        ))
        .setTriangles(Vector.empty())
        .setMetadata(Vector.empty())
        .build();

    final Path root = this.filesystem.getRootDirectories().iterator().next();
    final Validation<List<SMFProcessingError>, SMFMemoryMesh> r =
      filter.filter(SMFFilterCommandContext.of(root, root), mesh);

    Assert.assertTrue(r.isInvalid());
    Assert.assertThat(
      r.getError().get(0).message(),
      StringContains.containsString(
        "No attributes were matched by the given pattern."));

    r.getError().forEach(x -> LOG.error("{}", x.message()));
  }

  @Test
  public void testNonexistentSkeleton()
    throws Exception
  {
    final SMFAttributeName attr_root_input =
      SMFAttributeName.of("GROUP:root");
    final SMFAttributeName attr_indices_output =
      SMFAttributeName.of("joint_indices");
    final SMFAttributeName attr_weights_output =
      SMFAttributeName.of("joint_weights");

    final SMFMemoryMeshFilterType filter =
      CaFilterCommandCompileMeshWeights.create(
        this.filesystem.getPath("nonexistent"),
        attr_indices_output,
        attr_weights_output,
        Pattern.compile(".*"));

    final List<SMFAttribute> attributes =
      List.of(SMFAttribute.of(attr_root_input, ELEMENT_TYPE_FLOATING, 1, 32));

    final SMFHeader header =
      baseHeader(attributes);

    final SMFMemoryMesh mesh =
      SMFMemoryMesh.builder()
        .setHeader(header)
        .setArrays(HashMap.ofEntries(
          Tuple.of(
            attr_root_input,
            SMFAttributeArrayFloating1.of(Vector.empty()))
        ))
        .setTriangles(Vector.empty())
        .setMetadata(Vector.empty())
        .build();

    final Path root = this.filesystem.getRootDirectories().iterator().next();
    final Validation<List<SMFProcessingError>, SMFMemoryMesh> r =
      filter.filter(SMFFilterCommandContext.of(root, root), mesh);

    Assert.assertTrue(r.isInvalid());
    Assert.assertThat(
      r.getError().get(0).message(),
      StringContains.containsString("No such file"));

    r.getError().forEach(x -> LOG.error("{}", x.message()));
  }

  private Path writeSkeleton(
    final CaSkeleton skeleton)
    throws IOException
  {
    final CaFormatVersion version =
      CaFormatVersion.of(1, 0);
    final CaCompiledSerializerType serial =
      new CaProtobuf3FormatProvider().serializerCreate(version);

    final Path root = this.filesystem.getRootDirectories().iterator().next();
    final Path path = root.resolve("skeleton.ccp");
    Files.deleteIfExists(path);

    try (final OutputStream os = Files.newOutputStream(path)) {
      serial.serializeCompiledSkeletonToStream(skeleton, os);
      os.flush();
    }
    return path;
  }
}
