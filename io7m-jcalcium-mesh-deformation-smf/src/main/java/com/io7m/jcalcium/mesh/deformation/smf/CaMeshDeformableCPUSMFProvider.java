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

package com.io7m.jcalcium.mesh.deformation.smf;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursor3Type;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursor4Type;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorReadableType;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeCursorType;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeSemantic;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableAttributeSourceSelection;
import com.io7m.jcalcium.mesh.deformation.cpu.CaMeshDeformableCPUType;
import com.io7m.jcalcium.mesh.processing.smf.CaSchemas;
import com.io7m.jfunctional.Unit;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import com.io7m.jpra.runtime.java.JPRACursor1DType;
import com.io7m.jtensors.VectorWritable3DType;
import com.io7m.jtensors.VectorWritable4DType;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.smfj.bytebuffer.SMFByteBufferCursors;
import com.io7m.smfj.bytebuffer.SMFByteBufferFloat3Type;
import com.io7m.smfj.bytebuffer.SMFByteBufferFloat4Type;
import com.io7m.smfj.bytebuffer.SMFByteBufferIntegerUnsigned4Type;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackedAttribute;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackedAttributeSet;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackedMesh;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackedMeshLoaderType;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackedMeshes;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackedTriangles;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackerEventsType;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackingConfiguration;
import com.io7m.smfj.bytebuffer.SMFByteBufferPackingConfigurationType;
import com.io7m.smfj.core.SMFAttribute;
import com.io7m.smfj.core.SMFAttributeName;
import com.io7m.smfj.core.SMFComponentType;
import com.io7m.smfj.core.SMFErrorType;
import com.io7m.smfj.core.SMFHeader;
import com.io7m.smfj.core.SMFHeaderType;
import com.io7m.smfj.core.SMFTriangles;
import com.io7m.smfj.parser.api.SMFParseError;
import com.io7m.smfj.parser.api.SMFParserEventsMetaType;
import com.io7m.smfj.parser.api.SMFParserProviderType;
import com.io7m.smfj.parser.api.SMFParserSequentialType;
import com.io7m.smfj.processing.api.SMFProcessingError;
import com.io7m.smfj.validation.api.SMFSchema;
import com.io7m.smfj.validation.api.SMFSchemaAttribute;
import com.io7m.smfj.validation.api.SMFSchemaValidatorType;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.collection.Seq;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
import javaslang.collection.Vector;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;

import static com.io7m.jcalcium.mesh.processing.smf.CaSchemas.JOINT_INDICES_NAME;
import static com.io7m.jcalcium.mesh.processing.smf.CaSchemas.JOINT_WEIGHTS_NAME;
import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.valid;

/**
 * A mesh provider that can create SMF-backed CPU-deformable meshes.
 */

public final class CaMeshDeformableCPUSMFProvider
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CaMeshDeformableCPUSMFProvider.class);
  }

  private static final int INDEX_SOURCE = 0;
  private static final int INDEX_AUX = 1;
  private static final int INDEX_JOINTS = 2;
  private static final int INDEX_TARGET = 3;

  private CaMeshDeformableCPUSMFProvider()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Create a new CPU-deformable mesh from an SMF file.
   *
   * @param parser_provider A parser provider
   * @param validator       A mesh validator
   * @param meta            A metadata listener
   * @param config          The mesh configuration
   * @param events          An event receiver
   * @param path            The mesh path to be used in error messages
   * @param stream          A stream referring to an SMF file
   * @param <T>             The precise type of meshes created
   *
   * @return A deformable mesh, or a list of reasons why the mesh could not be
   * created
   */

  public static <T extends CaMeshDeformableCPUType> Validation<List<SMFErrorType>, T>
  createFromStream(
    final SMFParserProviderType parser_provider,
    final SMFSchemaValidatorType validator,
    final SMFParserEventsMetaType meta,
    final CaMeshDeformableCPUSMFConfiguration config,
    final CaMeshDeformableCPUSMFEventsType<T> events,
    final Path path,
    final InputStream stream)
  {
    NullCheck.notNull(parser_provider, "Parser provider");
    NullCheck.notNull(validator, "Validator");
    NullCheck.notNull(meta, "Meta");
    NullCheck.notNull(config, "Config");
    NullCheck.notNull(events, "Events");
    NullCheck.notNull(path, "Path");
    NullCheck.notNull(stream, "Stream");

    try {
      events.onStart();

      final SMFByteBufferPackedMeshLoaderType loader =
        SMFByteBufferPackedMeshes.newLoader(
          meta, new Packer<>(config, events, validator));

      return runParser(parser_provider, path, stream, loader).flatMap(
        ignored -> {
          try {
            final SMFByteBufferPackedMesh mesh = loader.mesh();

            final SortedMap<Integer, SMFByteBufferPackedAttributeSet> attribute_sets_by_id =
              mesh.attributeSetsByID();

            final SMFByteBufferPackedAttributeSet joint_attributes =
              attribute_sets_by_id.get(Integer.valueOf(INDEX_JOINTS)).get();
            final SMFByteBufferPackedAttributeSet source_attributes =
              attribute_sets_by_id.get(Integer.valueOf(INDEX_SOURCE)).get();
            final SMFByteBufferPackedAttributeSet aux_attributes =
              attribute_sets_by_id.get(Integer.valueOf(INDEX_AUX)).get();

            final long size =
              Integer.toUnsignedLong(source_attributes.byteBuffer().capacity());
            final ByteBuffer target =
              events.onAllocateTargetBuffer(size);

            Invariants.checkInvariantL(
              (long) target.capacity(),
              size == (long) target.capacity(),
              x -> "Allocated target buffer must be of size " + size);

            final SMFByteBufferPackedAttributeSet target_attributes =
              SMFByteBufferPackedAttributeSet.builder()
                .from(source_attributes)
                .setByteBuffer(target)
                .setId(INDEX_TARGET)
                .build();

            final SMFByteBufferPackingConfiguration joint_data_config =
              joint_attributes.configuration();
            final SortedMap<SMFAttributeName, SMFByteBufferPackedAttribute> joint_data_by_name =
              joint_data_config.packedAttributesByName();

            final JPRACursor1DType<SMFByteBufferIntegerUnsigned4Type> cursor_joint_index =
              SMFByteBufferCursors.createUnsigned4(
                joint_data_config,
                joint_data_by_name.get(JOINT_INDICES_NAME).get(),
                joint_attributes.byteBuffer());
            final JPRACursor1DType<SMFByteBufferFloat4Type> cursor_joint_weight =
              SMFByteBufferCursors.createFloat4(
                joint_data_config,
                joint_data_by_name.get(JOINT_WEIGHTS_NAME).get(),
                joint_attributes.byteBuffer());

            final Optional<SMFByteBufferPackedTriangles> tri_opt = mesh.triangles();
            Preconditions.checkPrecondition(
              tri_opt.isPresent(),
              "Triangles must be present");

            final Tuple2<
              SortedMap<String, CaMeshDeformableAttributeCursorReadableType>,
              SortedMap<String, CaMeshDeformableAttributeCursorType>> pair =
              createCursors(config, source_attributes, target);

            return valid(events.onCreated(
              mesh.header(),
              CaMeshDeformableCPUSMFPackedAttributeSet.of(joint_attributes),
              cursor_joint_index,
              cursor_joint_weight,
              CaMeshDeformableCPUSMFPackedAttributeSet.of(source_attributes),
              pair._1,
              CaMeshDeformableCPUSMFPackedAttributeSet.of(target_attributes),
              pair._2,
              CaMeshDeformableCPUSMFPackedAttributeSet.of(aux_attributes),
              tri_opt.get()
            ));

          } catch (final Exception e) {
            return invalid(List.of(
              SMFProcessingError.of(e.getMessage(), Optional.of(e))));
          }
        });
    } finally {
      events.onFinish();
    }
  }

  private static Validation<List<SMFErrorType>, Unit> runParser(
    final SMFParserProviderType parser_provider,
    final Path path,
    final InputStream stream,
    final SMFByteBufferPackedMeshLoaderType loader)
  {
    try (final SMFParserSequentialType parser =
           parser_provider.parserCreateSequential(loader, path, stream)) {
      parser.parseHeader();
      if (!loader.errors().isEmpty()) {
        return invalid(loader.errors());
      }

      parser.parseData();
      if (!loader.errors().isEmpty()) {
        return invalid(loader.errors());
      }
    } catch (final IOException e) {
      final SMFParseError error =
        SMFParseError.of(
          LexicalPosition.of(0, 0, Optional.of(path)),
          e.getMessage(),
          Optional.of(e));
      return invalid(List.of(error));
    }

    return valid(Unit.unit());
  }

  /**
   * Instantiate cursors for all of the required attributes.
   */

  private static Tuple2<
    SortedMap<String, CaMeshDeformableAttributeCursorReadableType>,
    SortedMap<String, CaMeshDeformableAttributeCursorType>>
  createCursors(
    final CaMeshDeformableCPUSMFConfigurationType config,
    final SMFByteBufferPackedAttributeSet source_attributes,
    final ByteBuffer target)
  {
    final SMFByteBufferPackingConfiguration source_data_config =
      source_attributes.configuration();

    SortedMap<String, CaMeshDeformableAttributeCursorReadableType> r_source_cursors =
      TreeMap.empty();
    SortedMap<String, CaMeshDeformableAttributeCursorType> r_target_cursors =
      TreeMap.empty();

    final Seq<CaMeshDeformableAttributeSourceSelection> attrs =
      config.sourceAttributes();
    for (int index = 0; index < attrs.size(); ++index) {
      final CaMeshDeformableAttributeSourceSelection source_attr =
        attrs.get(index);
      final SMFAttributeName attr_name =
        SMFAttributeName.of(source_attr.name());
      final SMFByteBufferPackedAttribute packed_attr =
        source_data_config.packedAttributesByName().get(attr_name).get();

      switch (packed_attr.attribute().componentCount()) {
        case 3: {
          final JPRACursor1DType<SMFByteBufferFloat3Type> js_cursor =
            SMFByteBufferCursors.createFloat3(
              source_data_config, packed_attr, source_attributes.byteBuffer());
          final CaMeshDeformableAttributeCursorReadableType source_cursor =
            new Cursor3(source_attr.semantic(), js_cursor);
          final JPRACursor1DType<SMFByteBufferFloat3Type> jt_cursor =
            SMFByteBufferCursors.createFloat3(
              source_data_config, packed_attr, target);
          final CaMeshDeformableAttributeCursorType target_cursor =
            new Cursor3(source_attr.semantic(), jt_cursor);
          r_source_cursors =
            r_source_cursors.put(source_attr.name(), source_cursor);
          r_target_cursors =
            r_target_cursors.put(source_attr.name(), target_cursor);
          break;
        }

        case 4: {
          final JPRACursor1DType<SMFByteBufferFloat4Type> js_cursor =
            SMFByteBufferCursors.createFloat4(
              source_data_config, packed_attr, source_attributes.byteBuffer());
          final CaMeshDeformableAttributeCursorReadableType source_cursor =
            new Cursor4(source_attr.semantic(), js_cursor);
          final JPRACursor1DType<SMFByteBufferFloat4Type> jt_cursor =
            SMFByteBufferCursors.createFloat4(
              source_data_config, packed_attr, target);
          final CaMeshDeformableAttributeCursorType target_cursor =
            new Cursor4(source_attr.semantic(), jt_cursor);
          r_source_cursors =
            r_source_cursors.put(source_attr.name(), source_cursor);
          r_target_cursors =
            r_target_cursors.put(source_attr.name(), target_cursor);
          break;
        }

        default: {
          throw new UnreachableCodeException();
        }
      }
    }

    return Tuple.of(r_source_cursors, r_target_cursors);
  }

  private static final class Packer<T extends CaMeshDeformableCPUType>
    implements SMFByteBufferPackerEventsType
  {
    private final SMFSchemaValidatorType validator;
    private final CaMeshDeformableCPUSMFConfiguration config;
    private final CaMeshDeformableCPUSMFEventsType<T> events;

    private Packer(
      final CaMeshDeformableCPUSMFConfiguration in_config,
      final CaMeshDeformableCPUSMFEventsType<T> in_events,
      final SMFSchemaValidatorType in_validator)
    {
      this.config = NullCheck.notNull(in_config, "Config");
      this.events = NullCheck.notNull(in_events, "Events");
      this.validator = NullCheck.notNull(in_validator, "Validator");
    }

    private static SMFByteBufferPackingConfiguration
    createMeshAuxiliaryPackingConfiguration(
      final SMFHeaderType header,
      final SMFByteBufferPackingConfigurationType mesh_source_config,
      final SMFByteBufferPackingConfigurationType mesh_joint_config)
    {
      final SortedMap<SMFAttributeName, SMFAttribute> header_by_name =
        header.attributesByName();
      final SortedMap<SMFAttributeName, SMFByteBufferPackedAttribute> s_names =
        mesh_source_config.packedAttributesByName();
      final SortedMap<SMFAttributeName, SMFByteBufferPackedAttribute> j_names =
        mesh_joint_config.packedAttributesByName();
      return SMFByteBufferPackingConfiguration.of(header_by_name.removeKeys(
        name -> s_names.containsKey(name) || j_names.containsKey(name)).values());
    }

    private static SMFByteBufferPackingConfiguration
    createMeshJointPackingConfiguration(
      final SMFHeaderType header)
    {
      final SortedMap<SMFAttributeName, SMFAttribute> by_name =
        header.attributesByName();
      return SMFByteBufferPackingConfiguration.of(Vector.of(
        by_name.get(JOINT_INDICES_NAME).get(),
        by_name.get(JOINT_WEIGHTS_NAME).get()));
    }

    private static SMFByteBufferPackingConfiguration
    createMeshSourcePackingConfiguration(
      final SMFHeaderType header,
      final Seq<CaMeshDeformableAttributeSourceSelection> required)
    {
      final SortedMap<SMFAttributeName, SMFAttribute> by_name =
        header.attributesByName();
      final SMFByteBufferPackingConfiguration.Builder mesh_source_config_b =
        SMFByteBufferPackingConfiguration.builder();

      for (int index = 0; index < required.size(); ++index) {
        final CaMeshDeformableAttributeSourceSelection select =
          required.get(index);
        final String name = select.name();
        LOG.debug("required attribute: {}", name);
        final SMFAttribute attr = by_name.get(SMFAttributeName.of(name)).get();
        mesh_source_config_b.addAttributesOrdered(attr);
      }

      return mesh_source_config_b.build();
    }

    private static SMFSchema createSchemaWithRequired(
      final SMFSchema schema,
      final CaMeshDeformableCPUSMFConfiguration config)
    {
      SortedMap<SMFAttributeName, SMFSchemaAttribute> required_by_name =
        schema.requiredAttributes();

      final Seq<CaMeshDeformableAttributeSourceSelection> attributes =
        config.sourceAttributes();

      for (int index = 0; index < attributes.size(); ++index) {
        final CaMeshDeformableAttributeSourceSelection attribute =
          attributes.get(index);
        final SMFAttributeName attr_name =
          SMFAttributeName.of(attribute.name());

        SMFSchemaAttribute schema_attr = null;
        switch (attribute.kind()) {
          case CURSOR_FLOAT_3: {
            schema_attr =
              SMFSchemaAttribute.of(
                attr_name,
                Optional.of(SMFComponentType.ELEMENT_TYPE_FLOATING),
                OptionalInt.of(3),
                OptionalInt.empty());
            break;
          }
          case CURSOR_FLOAT_4: {
            schema_attr =
              SMFSchemaAttribute.of(
                attr_name,
                Optional.of(SMFComponentType.ELEMENT_TYPE_FLOATING),
                OptionalInt.of(4),
                OptionalInt.empty());
            break;
          }
        }

        required_by_name = required_by_name.put(attr_name, schema_attr);
      }

      return SMFSchema.builder()
        .from(schema)
        .setRequiredAttributes(required_by_name)
        .setOptionalAttributes(TreeMap.empty())
        .build();
    }

    @Override
    public Validation<List<SMFErrorType>, SortedMap<Integer, SMFByteBufferPackingConfiguration>>
    onHeader(
      final SMFHeader head)
    {
      final SMFSchema schema_new =
        createSchemaWithRequired(CaSchemas.standardConventions(), this.config);

      return this.validator.validate(head, schema_new).map(header -> {
        final SMFByteBufferPackingConfiguration mesh_source_config =
          createMeshSourcePackingConfiguration(
            header,
            this.config.sourceAttributes());
        final SMFByteBufferPackingConfiguration mesh_joint_config =
          createMeshJointPackingConfiguration(header);
        final SMFByteBufferPackingConfiguration aux_b =
          createMeshAuxiliaryPackingConfiguration(
            header,
            mesh_source_config,
            mesh_joint_config);

        return TreeMap.ofEntries(
          Tuple.of(Integer.valueOf(INDEX_SOURCE), mesh_source_config),
          Tuple.of(Integer.valueOf(INDEX_AUX), aux_b),
          Tuple.of(Integer.valueOf(INDEX_JOINTS), mesh_joint_config));
      });
    }

    @Override
    public boolean onShouldPackTriangles()
    {
      return true;
    }

    @Override
    public ByteBuffer onAllocateTriangleBuffer(
      final SMFTriangles triangles,
      final long size)
    {
      final ByteBuffer r =
        this.events.onAllocateTriangleBuffer(triangles, size);
      Invariants.checkInvariantL(
        (long) r.capacity(),
        size == (long) r.capacity(),
        x -> "Allocated triangle buffer must be of size " + size);
      return r;
    }

    @Override
    public ByteBuffer onAllocateAttributeBuffer(
      final Integer id,
      final SMFByteBufferPackingConfiguration buffer_config,
      final long size)
    {
      switch (id.intValue()) {
        case INDEX_AUX: {
          final ByteBuffer r = this.events.onAllocateAuxiliaryBuffer(size);
          Invariants.checkInvariantL(
            (long) r.capacity(),
            size == (long) r.capacity(),
            x -> "Allocated auxiliary buffer must be of size " + size);
          return r;
        }
        case INDEX_SOURCE: {
          final ByteBuffer r = this.events.onAllocateSourceBuffer(size);
          Invariants.checkInvariantL(
            (long) r.capacity(),
            size == (long) r.capacity(),
            x -> "Allocated source buffer must be of size " + size);
          return r;
        }
        case INDEX_JOINTS: {
          final ByteBuffer r = this.events.onAllocateJointBuffer(size);
          Invariants.checkInvariantL(
            (long) r.capacity(),
            size == (long) r.capacity(),
            x -> "Allocated joint buffer must be of size " + size);
          return r;
        }
        default: {
          throw new UnreachableCodeException();
        }
      }
    }
  }

  private static final class Cursor3 implements
    CaMeshDeformableAttributeCursor3Type
  {
    private final JPRACursor1DType<SMFByteBufferFloat3Type> cursor;
    private final CaMeshDeformableAttributeSemantic semantic;
    private final SMFByteBufferFloat3Type view;

    Cursor3(
      final CaMeshDeformableAttributeSemantic in_semantic,
      final JPRACursor1DType<SMFByteBufferFloat3Type> in_cursor)
    {
      this.semantic = NullCheck.notNull(in_semantic, "Semantic");
      this.cursor = NullCheck.notNull(in_cursor, "Cursor");
      this.view = this.cursor.getElementView();
    }

    @Override
    public long vertex()
    {
      return Integer.toUnsignedLong(this.cursor.getElementIndex());
    }

    @Override
    public void setVertex(final long vertex)
    {
      this.cursor.setElementIndex(Math.toIntExact(vertex));
    }

    @Override
    public CaMeshDeformableAttributeSemantic semantic()
    {
      return this.semantic;
    }

    @Override
    public void get3D(final VectorWritable3DType out)
    {
      this.view.get3D(out);
    }

    @Override
    public void set3D(
      final double x,
      final double y,
      final double z)
    {
      this.view.set3D(x, y, z);
    }
  }

  private static final class Cursor4 implements
    CaMeshDeformableAttributeCursor4Type
  {
    private final JPRACursor1DType<SMFByteBufferFloat4Type> cursor;
    private final CaMeshDeformableAttributeSemantic semantic;
    private final SMFByteBufferFloat4Type view;

    Cursor4(
      final CaMeshDeformableAttributeSemantic in_semantic,
      final JPRACursor1DType<SMFByteBufferFloat4Type> in_cursor)
    {
      this.semantic = NullCheck.notNull(in_semantic, "Semantic");
      this.cursor = NullCheck.notNull(in_cursor, "Cursor");
      this.view = this.cursor.getElementView();
    }

    @Override
    public long vertex()
    {
      return Integer.toUnsignedLong(this.cursor.getElementIndex());
    }

    @Override
    public void setVertex(final long vertex)
    {
      this.cursor.setElementIndex(Math.toIntExact(vertex));
    }

    @Override
    public CaMeshDeformableAttributeSemantic semantic()
    {
      return this.semantic;
    }

    @Override
    public void get4D(final VectorWritable4DType out)
    {
      this.view.get4D(out);
    }

    @Override
    public void set4D(
      final double x,
      final double y,
      final double z,
      final double w)
    {
      this.view.set4D(x, y, z, w);
    }
  }
}
