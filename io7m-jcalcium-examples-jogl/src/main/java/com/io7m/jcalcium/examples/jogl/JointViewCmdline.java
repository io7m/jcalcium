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

package com.io7m.jcalcium.examples.jogl;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jareas.core.AreaInclusiveUnsignedL;
import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.CaSkeletonHash;
import com.io7m.jcalcium.core.compiled.CaSkeletonMetadata;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPose;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPoseDType;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurves;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveTranslation;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointReadableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedSkeletonD;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedSkeletonMutableDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContext;
import com.io7m.jcalcium.evaluator.api.CaEvaluationContextType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorSingleType;
import com.io7m.jcalcium.evaluator.main.CaEvaluatorSingleD;
import com.io7m.jcalcium.loader.api.CaLoaderException;
import com.io7m.jcalcium.loader.api.CaLoaderFormatProviderType;
import com.io7m.jcalcium.loader.api.CaLoaderType;
import com.io7m.jcalcium.loader.api.CaLoaders;
import com.io7m.jcamera.JCameraContext;
import com.io7m.jcamera.JCameraSpherical;
import com.io7m.jcamera.JCameraSphericalInput;
import com.io7m.jcamera.JCameraSphericalInputType;
import com.io7m.jcamera.JCameraSphericalIntegrator;
import com.io7m.jcamera.JCameraSphericalIntegratorType;
import com.io7m.jcamera.JCameraSphericalSnapshot;
import com.io7m.jcamera.JCameraSphericalType;
import com.io7m.jcanephora.core.JCGLClearSpecification;
import com.io7m.jcanephora.core.JCGLExceptionNonCompliant;
import com.io7m.jcanephora.core.JCGLExceptionUnsupported;
import com.io7m.jcanephora.core.JCGLProjectionMatrices;
import com.io7m.jcanephora.core.JCGLProjectionMatricesType;
import com.io7m.jcanephora.core.api.JCGLContextType;
import com.io7m.jcanephora.core.api.JCGLFramebuffersType;
import com.io7m.jcanephora.core.api.JCGLInterfaceGL33Type;
import com.io7m.jcanephora.core.api.JCGLTexturesType;
import com.io7m.jcanephora.jogl.JCGLImplementationJOGL;
import com.io7m.jcanephora.jogl.JCGLImplementationJOGLType;
import com.io7m.jcanephora.profiler.JCGLProfiling;
import com.io7m.jcanephora.profiler.JCGLProfilingContextType;
import com.io7m.jcanephora.profiler.JCGLProfilingFrameType;
import com.io7m.jcanephora.profiler.JCGLProfilingType;
import com.io7m.jcanephora.texture_unit_allocator.JCGLTextureUnitAllocator;
import com.io7m.jcanephora.texture_unit_allocator.JCGLTextureUnitAllocatorType;
import com.io7m.jcanephora.texture_unit_allocator.JCGLTextureUnitContextParentType;
import com.io7m.jcanephora.texture_unit_allocator.JCGLTextureUnitContextType;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNode;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.QuaternionM4F;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.VectorI3F;
import com.io7m.jtensors.VectorI4D;
import com.io7m.jtensors.VectorI4F;
import com.io7m.jtensors.VectorM4D;
import com.io7m.jtensors.parameterized.PMatrixDirect4x4FType;
import com.io7m.jtensors.parameterized.PMatrixDirectM4x4F;
import com.io7m.jtensors.parameterized.PMatrixI3x3F;
import com.io7m.jtensors.parameterized.PVectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3F;
import com.io7m.jtensors.parameterized.PVectorI4F;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.junsigned.ranges.UnsignedRangeInclusiveL;
import com.io7m.r2.core.R2CopyDepth;
import com.io7m.r2.core.R2FilterType;
import com.io7m.r2.core.R2GeometryBuffer;
import com.io7m.r2.core.R2GeometryBufferComponents;
import com.io7m.r2.core.R2GeometryBufferDescription;
import com.io7m.r2.core.R2GeometryBufferType;
import com.io7m.r2.core.R2InstanceBillboardedDynamic;
import com.io7m.r2.core.R2InstanceBillboardedDynamicType;
import com.io7m.r2.core.R2InstanceSingle;
import com.io7m.r2.core.R2LightAmbientScreenSingle;
import com.io7m.r2.core.R2LightBufferComponents;
import com.io7m.r2.core.R2LightBufferDescription;
import com.io7m.r2.core.R2LightBufferType;
import com.io7m.r2.core.R2LightBuffers;
import com.io7m.r2.core.R2MaterialOpaqueBillboarded;
import com.io7m.r2.core.R2MaterialOpaqueSingle;
import com.io7m.r2.core.R2Matrices;
import com.io7m.r2.core.R2MatricesType;
import com.io7m.r2.core.R2ProjectionFOV;
import com.io7m.r2.core.R2SceneLights;
import com.io7m.r2.core.R2SceneLightsType;
import com.io7m.r2.core.R2SceneOpaques;
import com.io7m.r2.core.R2SceneOpaquesType;
import com.io7m.r2.core.R2SceneStencils;
import com.io7m.r2.core.R2SceneStencilsMode;
import com.io7m.r2.core.R2SceneStencilsType;
import com.io7m.r2.core.R2ShadowMapContextType;
import com.io7m.r2.core.R2Texture2DUsableType;
import com.io7m.r2.core.R2TransformSOT;
import com.io7m.r2.core.R2UnitSphereType;
import com.io7m.r2.core.debug.R2DebugCube;
import com.io7m.r2.core.debug.R2DebugCubeType;
import com.io7m.r2.core.debug.R2DebugInstances;
import com.io7m.r2.core.debug.R2DebugLineSegment;
import com.io7m.r2.core.debug.R2DebugVisualizerRendererParameters;
import com.io7m.r2.core.debug.R2DebugVisualizerRendererType;
import com.io7m.r2.core.shaders.provided.R2LightShaderAmbientSingle;
import com.io7m.r2.core.shaders.provided.R2SurfaceShaderBasicBillboarded;
import com.io7m.r2.core.shaders.provided.R2SurfaceShaderBasicParameters;
import com.io7m.r2.core.shaders.provided.R2SurfaceShaderBasicSingle;
import com.io7m.r2.core.shaders.types.R2ShaderInstanceBillboardedType;
import com.io7m.r2.core.shaders.types.R2ShaderInstanceSingleType;
import com.io7m.r2.core.shaders.types.R2ShaderLightSingleType;
import com.io7m.r2.filters.R2FilterLightApplicator;
import com.io7m.r2.filters.R2FilterLightApplicatorParameters;
import com.io7m.r2.filters.R2FilterLightApplicatorParametersType;
import com.io7m.r2.main.R2Main;
import com.io7m.r2.main.R2MainType;
import com.io7m.r2.meshes.defaults.R2UnitSphere;
import com.io7m.r2.spaces.R2SpaceEyeType;
import com.io7m.r2.spaces.R2SpaceWorldType;
import com.io7m.timehack6435126.TimeHack6435126;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.DebugGL3;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;
import javaslang.Tuple2;
import javaslang.collection.Iterator;
import javaslang.collection.List;
import javaslang.collection.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.io7m.jfunctional.Unit.unit;

public final class JointViewCmdline implements Runnable, KeyListener
{
  private static final Logger LOG;
  private static final boolean FILE;

  static {
    LOG = LoggerFactory.getLogger(JointViewCmdline.class);
    FILE = true;
  }

  private final String[] args;
  private final JCameraContext camera_context;
  private final JCameraSphericalType camera;
  private final JCameraSphericalInputType camera_input;
  private final JCameraSphericalIntegratorType camera_integrator;
  private final MatrixM4x4D.ContextMM4D matrix_context4x4d;
  private final PMatrixDirect4x4FType<R2SpaceWorldType, R2SpaceEyeType> matrix_view;
  private final AtomicReference<Double> time_scale;
  private JCGLProfilingType render_profiling;
  private R2MatricesType render_matrices;
  private JCGLProjectionMatricesType render_projections;
  private R2ProjectionFOV render_projection;
  private R2DebugVisualizerRendererType renderer;
  private R2GeometryBufferType render_gbuffer;
  private R2LightBufferType render_lbuffer;
  private R2SceneOpaquesType render_opaques;
  private R2SceneLightsType render_lights;
  private R2ShadowMapContextType render_shadows;
  private R2FilterType<R2FilterLightApplicatorParametersType> render_light_applicator;
  private JCGLClearSpecification render_clear;
  private R2ShaderInstanceBillboardedType<R2SurfaceShaderBasicParameters> render_skeleton_joint_shader;
  private R2SurfaceShaderBasicParameters render_skeleton_joint_parameters;
  private R2MaterialOpaqueBillboarded<R2SurfaceShaderBasicParameters> render_skeleton_joint_material;
  private R2LightAmbientScreenSingle render_light_ambient;
  private R2ShaderLightSingleType<R2LightAmbientScreenSingle> render_light_ambient_shader;
  private R2InstanceSingle render_floor;
  private R2ShaderInstanceSingleType<R2SurfaceShaderBasicParameters> render_floor_shader;
  private R2SurfaceShaderBasicParameters render_floor_parameters;
  private R2MaterialOpaqueSingle<R2SurfaceShaderBasicParameters> render_floor_material;
  private R2SceneStencilsType render_stencils;
  private R2DebugVisualizerRendererParameters render_debug_parameters;
  private JCGLTextureUnitAllocatorType render_texture_units;
  private R2DebugCubeType render_debug_cube;
  private R2UnitSphereType render_debug_sphere;
  private volatile int actions_index;
  private List<CaActionName> actions_ordered;
  private CaSkeleton skeleton;
  private GLWindow window;
  private JCGLContextType jc_context;
  private JCGLInterfaceGL33Type g33;
  private Map<CaActionName, CaEvaluatorSingleType> actions;
  private long time_then;
  private volatile long frame;
  private volatile long frame_start;
  private double time_accum;
  private JCameraSphericalSnapshot snap_curr;
  private JCameraSphericalSnapshot snap_prev;
  private R2MainType main;
  private R2InstanceBillboardedDynamicType skeleton_joints;
  private AreaInclusiveUnsignedL window_area;
  private CaEvaluationContextType eval_context;
  private CaEvaluatedSkeletonMutableDType eval_skeleton;

  private JointViewCmdline(
    final String[] in_args)
  {
    this.args = NullCheck.notNull(in_args, "args");
    this.actions = new ConcurrentHashMap<>();
    this.actions_ordered = List.empty();
    this.actions_index = 0;

    this.matrix_context4x4d = new MatrixM4x4D.ContextMM4D();
    this.matrix_view = PMatrixDirectM4x4F.newMatrix();

    this.camera_context =
      new JCameraContext();
    this.camera =
      JCameraSpherical.newCamera();
    this.camera_input =
      JCameraSphericalInput.newInput();
    this.camera_integrator =
      JCameraSphericalIntegrator.newIntegrator(this.camera, this.camera_input);

    this.time_scale = new AtomicReference<>(Double.valueOf(1.0));
    this.time_then = System.nanoTime();
    this.frame = 0L;
    this.frame_start = 0L;
  }

  public static void main(
    final String[] args)
  {
    TimeHack6435126.enableHighResolutionTimer();

    new JointViewCmdline(args).run();
  }

  private static CaSkeleton loadSkeleton(
    final Path path)
    throws CaLoaderException, IOException
  {
    LOG.debug("loading skeleton");

    if (FILE) {
      final CaLoaderFormatProviderType provider =
        CaLoaders.findProvider(path, Optional.empty());
      final CaLoaderType loader =
        provider.loaderCreate();

      try (final InputStream is = Files.newInputStream(path)) {
        final CaSkeleton skeleton =
          loader.loadCompiledSkeletonFromStream(is, path.toUri());
        LOG.debug(
          "joints: {} joints", Integer.valueOf(skeleton.jointsByID().size()));
        return skeleton;
      }
    }

    final CaJoint root =
      CaJoint.of(
        CaJointName.of("root"),
        0,
        new PVectorI3D<>(0.0, 0.0, 0.0),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint hip =
      CaJoint.of(
        CaJointName.of("hip"),
        1,
        new PVectorI3D<>(0.0, 3.0, 0.0),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint shoulder_left =
      CaJoint.of(
        CaJointName.of("shoulder-left"),
        2,
        new PVectorI3D<>(0.0, 0.0, 0.0),
        QuaternionI4D.makeFromAxisAngle(
          new VectorI3D(0.0, 0.0, -1.0),
          Math.toRadians(45.0)),
        new VectorI3D(1.0, 1.0, 1.0));
    final CaJoint shoulder_right =
      CaJoint.of(
        CaJointName.of("shoulder-right"),
        3,
        new PVectorI3D<>(0.0, 0.0, 0.0),
        QuaternionI4D.makeFromAxisAngle(
          new VectorI3D(0.0, 0.0, -1.0),
          Math.toRadians(-45.0)),
        new VectorI3D(1.0, 1.0, 1.0));

    final CaJoint hand_left =
      CaJoint.of(
        CaJointName.of("hand-left"),
        4,
        new PVectorI3D<>(-1.0, 0.0, 0.0),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));
    final CaJoint hand_right =
      CaJoint.of(
        CaJointName.of("hand-right"),
        5,
        new PVectorI3D<>(1.0, 0.0, 0.0),
        new QuaternionI4D(),
        new VectorI3D(1.0, 1.0, 1.0));

    final JOTreeNodeType<CaJoint> tree_root = JOTreeNode.create(root);
    final JOTreeNodeType<CaJoint> tree_hip = JOTreeNode.create(hip);
    tree_root.childAdd(tree_hip);
    final JOTreeNodeType<CaJoint> tree_arm_left = JOTreeNode.create(
      shoulder_left);
    tree_arm_left.childAdd(JOTreeNode.create(hand_left));
    tree_hip.childAdd(tree_arm_left);
    final JOTreeNodeType<CaJoint> tree_arm_right = JOTreeNode.create(
      shoulder_right);
    tree_arm_right.childAdd(JOTreeNode.create(hand_right));
    tree_hip.childAdd(tree_arm_right);

    final CaCurveKeyframeOrientation o_keyframe_0 =
      CaCurveKeyframeOrientation.of(
        0,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        QuaternionI4D.makeFromAxisAngle(
          new VectorI3D(0.0, 0.0, -1.0),
          Math.toRadians(45.0)));

    final CaCurveKeyframeOrientation o_keyframe_1 =
      CaCurveKeyframeOrientation.of(
        30,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        QuaternionI4D.makeFromAxisAngle(
          new VectorI3D(0.0, 0.0, -1.0),
          Math.toRadians(0.0)));

    final CaCurveKeyframeOrientation o_keyframe_2 =
      CaCurveKeyframeOrientation.of(
        60,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        QuaternionI4D.makeFromAxisAngle(
          new VectorI3D(0.0, 0.0, -1.0),
          Math.toRadians(45.0)));

    final CaCurveOrientation curve_orientation =
      CaCurveOrientation.builder()
        .setAction(CaActionName.of("action"))
        .setJoint(CaJointName.of("shoulder-left"))
        .putKeyframes(Integer.valueOf(0), o_keyframe_0)
        .putKeyframes(Integer.valueOf(30), o_keyframe_1)
        .putKeyframes(Integer.valueOf(60), o_keyframe_2)
        .build();

    final CaCurveKeyframeTranslation t_keyframe_0 =
      CaCurveKeyframeTranslation.of(
        0,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new PVectorI3D<>(0.0, 0.0, 0.0));

    final CaCurveKeyframeTranslation t_keyframe_1 =
      CaCurveKeyframeTranslation.of(
        30,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new PVectorI3D<>(0.0, 1.0, 0.0));

    final CaCurveKeyframeTranslation t_keyframe_2 =
      CaCurveKeyframeTranslation.of(
        60,
        CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR,
        CaCurveEasing.CURVE_EASING_IN_OUT,
        new PVectorI3D<>(0.0, 0.0, 0.0));

    final CaCurveTranslation curve_translation =
      CaCurveTranslation.builder()
        .setAction(CaActionName.of("action"))
        .setJoint(CaJointName.of("shoulder-right"))
        .putKeyframes(Integer.valueOf(0), t_keyframe_0)
        .putKeyframes(Integer.valueOf(30), t_keyframe_1)
        .putKeyframes(Integer.valueOf(60), t_keyframe_2)
        .build();

    final CaActionCurves action =
      CaActionCurves.builder()
        .setName(CaActionName.of("action"))
        .setFramesPerSecond(60)
        .putCurves(
          CaJointName.of("shoulder-left"),
          Vector.of(curve_orientation))
        .putCurves(
          CaJointName.of("shoulder-right"),
          Vector.of(curve_translation))
        .build();

    final CaSkeleton.Builder sb = CaSkeleton.builder();
    sb.setMeta(CaSkeletonMetadata.of(
      CaSkeletonName.of("skeleton"),
      CaSkeletonHash.of("SHA2-256", "3cb4e2c9e926cce1aa345f1e1724db8683a2aa1056b236ecd7c3aba318a9416d")));
    sb.setJoints(tree_root);
    sb.putActionsByName(action.name(), action);
    return sb.build();
  }

  @Override
  public void run()
  {
    try {
      LOG.info("start");

      if (this.args.length != 1) {
        LOG.info("usage: file.ccp");
        System.exit(1);
      }

      final Path path = Paths.get(this.args[0]);
      this.skeleton = loadSkeleton(path);
      this.loadActions(this.skeleton);

      LOG.debug("opening GL window");
      final GLProfile profile = GLProfile.get(GLProfile.GL3);
      final GLCapabilities caps = new GLCapabilities(profile);
      this.window = GLWindow.create(caps);
      this.window.setSize(640, 480);
      this.window.setTitle("Jointview: " + path);
      this.window.setVisible(true, true);
      this.window.addKeyListener(this);
      this.window_area = this.windowMakeArea();

      final GLContext context = this.window.getContext();
      context.makeCurrent();

      final JCGLImplementationJOGLType ji =
        JCGLImplementationJOGL.getInstance();
      this.jc_context = ji.newContextFromWithSupplier(
        context, c -> new DebugGL3(c.getGL().getGL3()), "main");
      this.g33 = this.jc_context.contextGetGL33();

      LOG.debug("opened window");

      this.renderInit();
      while (this.window.isVisible()) {
        this.render();
        this.window.display();

        try {
          TimeUnit.MILLISECONDS.sleep(16L);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        this.frame = Math.addExact(this.frame, 1L);
      }

    } catch (final CaLoaderException e) {
      LOG.error("Loader error: ", e);
      System.exit(1);
    } catch (final IOException e) {
      LOG.error("I/O error: ", e);
      System.exit(1);
    } catch (final JCGLExceptionUnsupported e) {
      LOG.error("OpenGL unsupported error: ", e);
      System.exit(1);
    } catch (final JCGLExceptionNonCompliant e) {
      LOG.error("OpenGL non-compliant error: ", e);
      System.exit(1);
    } finally {
      LOG.info("exit");
    }
  }

  private void renderInit()
  {
    this.main = R2Main.newBuilder().build(this.g33);

    final AreaInclusiveUnsignedL area = this.windowMakeArea();
    final JCGLTexturesType g_tex = this.g33.getTextures();

    this.render_texture_units =
      JCGLTextureUnitAllocator.newAllocatorWithStack(
        8, g_tex.textureGetUnits());
    this.render_profiling =
      JCGLProfiling.newProfiling(this.g33.getTimers());
    this.render_opaques =
      R2SceneOpaques.newOpaques();
    this.render_stencils =
      R2SceneStencils.newMasks();
    this.render_lights =
      R2SceneLights.newLights();
    this.render_debug_cube =
      R2DebugCube.newDebugCube(this.g33);

    this.render_debug_sphere =
      R2UnitSphere.newUnitSphere8(this.g33);

    this.render_debug_parameters =
      R2DebugVisualizerRendererParameters.builder()
        .setDebugCube(this.render_debug_cube)
        .setLights(this.render_lights)
        .setOpaqueInstances(this.render_opaques)
        .setUnitSphere(this.render_debug_sphere)
        .setDebugInstances(R2DebugInstances.builder().build())
        .build();

    this.render_shadows =
      this.main.getShadowMapRenderer().shadowBegin().shadowExecComplete();

    this.render_light_applicator =
      R2FilterLightApplicator.newFilter(
        this.main.getShaderPreprocessingEnvironment(),
        this.g33,
        this.main.getIDPool(),
        this.main.getUnitQuad());

    this.render_clear =
      JCGLClearSpecification.builder()
        .setColorBufferClear(new VectorI4F(0.0f, 0.0f, 0.0f, 1.0f))
        .setDepthBufferClear(1.0)
        .setStencilBufferClear(0)
        .setStrictChecking(false)
        .build();

    this.render_skeleton_joint_shader =
      R2SurfaceShaderBasicBillboarded.newShader(
        this.g33.getShaders(),
        this.main.getShaderPreprocessingEnvironment(),
        this.main.getIDPool());

    this.render_skeleton_joint_parameters =
      R2SurfaceShaderBasicParameters.builder()
        .setTextureDefaults(this.main.getTextureDefaults())
        .build();

    this.render_skeleton_joint_material =
      R2MaterialOpaqueBillboarded.of(
        this.main.getIDPool().freshID(),
        this.render_skeleton_joint_shader,
        this.render_skeleton_joint_parameters);

    final R2TransformSOT render_floor_transform = R2TransformSOT.newTransform();
    render_floor_transform.setScale(10.0f);
    render_floor_transform.getTranslation().set3F(0.0f, 0.0f, 0.0f);
    QuaternionM4F.makeFromAxisAngle(
      new VectorI3F(1.0f, 0.0f, 0.0f),
      Math.toRadians(-90.0),
      render_floor_transform.getOrientation());

    this.render_floor =
      R2InstanceSingle.of(
        this.main.getIDPool().freshID(),
        this.main.getUnitQuad().arrayObject(),
        render_floor_transform,
        PMatrixI3x3F.identity());

    this.render_floor_parameters =
      R2SurfaceShaderBasicParameters.builder()
        .setTextureDefaults(this.main.getTextureDefaults())
        .setAlbedoColor(new PVectorI4F<>(0.2f, 0.2f, 0.2f, 1.0f))
        .build();

    this.render_floor_shader =
      R2SurfaceShaderBasicSingle.newShader(
        this.g33.getShaders(),
        this.main.getShaderPreprocessingEnvironment(),
        this.main.getIDPool());

    this.render_floor_material =
      R2MaterialOpaqueSingle.of(
        this.main.getIDPool().freshID(),
        this.render_floor_shader,
        this.render_floor_parameters);

    this.render_light_ambient =
      R2LightAmbientScreenSingle.newLight(
        this.main.getUnitQuad(),
        this.main.getIDPool(),
        this.main.getTextureDefaults());
    this.render_light_ambient.colorWritable().set3F(1.0f, 1.0f, 1.0f);
    this.render_light_ambient.setIntensity(1.0f);

    this.render_light_ambient_shader =
      R2LightShaderAmbientSingle.newShader(
        this.g33.getShaders(),
        this.main.getShaderPreprocessingEnvironment(),
        this.main.getIDPool());

    final float aspect =
      (float) this.window_area.getRangeX().getInterval() /
        (float) this.window_area.getRangeY().getInterval();

    this.render_matrices =
      R2Matrices.newMatrices();

    this.render_projections =
      JCGLProjectionMatrices.newMatrices();

    this.render_projection =
      R2ProjectionFOV.newFrustumWith(
        this.render_projections,
        (float) Math.toRadians(120.0),
        aspect,
        0.001f,
        100.0f);

    this.renderInitCamera();
    this.renderInitFramebuffer(area);
    this.renderInitSkeleton();
  }

  private void renderInitCamera()
  {
    this.camera.cameraSetTargetPosition3f(0.0f, 0.0f, 0.0f);
    this.camera.cameraSetZoom(5.0f);
    this.camera.cameraSetAngleIncline((float) Math.toRadians(40.0));
    this.camera.cameraSetAngleHeading((float) Math.toRadians(-45.0));
  }

  private void renderInitFramebuffer(
    final AreaInclusiveUnsignedL area)
  {
    final JCGLFramebuffersType g_fb = this.g33.getFramebuffers();
    final JCGLTexturesType g_tex = this.g33.getTextures();

    final JCGLTextureUnitContextParentType tc_root =
      this.render_texture_units.getRootContext();

    final JCGLTextureUnitContextType tc =
      tc_root.unitContextNew();
    try {
      if (this.render_gbuffer != null) {
        this.render_gbuffer.delete(this.g33);
        this.render_gbuffer = null;
      }

      final R2GeometryBufferDescription gbuffer_desc =
        R2GeometryBufferDescription.builder()
          .setArea(area)
          .setComponents(R2GeometryBufferComponents.R2_GEOMETRY_BUFFER_NO_SPECULAR)
          .build();

      this.render_gbuffer =
        R2GeometryBuffer.newGeometryBuffer(g_fb, g_tex, tc, gbuffer_desc);

      if (this.render_lbuffer != null) {
        this.render_lbuffer.delete(this.g33);
        this.render_lbuffer = null;
      }

      final R2LightBufferDescription lbuffer_desc =
        R2LightBufferDescription.builder()
          .setArea(area)
          .setComponents(R2LightBufferComponents.R2_LIGHT_BUFFER_DIFFUSE_ONLY)
          .build();

      this.render_lbuffer =
        R2LightBuffers.newLightBuffer(g_fb, g_tex, tc, lbuffer_desc);

    } finally {
      tc.unitContextFinish(g_tex);
    }
  }

  private void renderInitSkeleton()
  {
    this.skeleton_joints =
      R2InstanceBillboardedDynamic.newBillboarded(
        this.main.getIDPool(),
        this.g33.getArrayBuffers(),
        this.g33.getArrayObjects(),
        this.skeleton.jointsByID().size());
  }

  private AreaInclusiveUnsignedL windowMakeArea()
  {
    return AreaInclusiveUnsignedL.of(
      new UnsignedRangeInclusiveL(0L, (long) this.window.getWidth() - 1L),
      new UnsignedRangeInclusiveL(0L, (long) this.window.getHeight() - 1L));
  }

  private boolean windowSizeChanged()
  {
    return this.window.getWidth() != this.window_area.getRangeX().getInterval()
      || this.window.getHeight() != this.window_area.getRangeY().getInterval();
  }

  private void render()
  {
    if (this.windowSizeChanged()) {
      LOG.trace("window size changed: {}");
      final AreaInclusiveUnsignedL area = this.windowMakeArea();
      this.renderInitFramebuffer(area);
      this.window_area = area;
    }

    this.renderCamera();
    this.renderSkeleton(this.time_scale.get().doubleValue());
  }

  private void renderSkeleton(
    final double time_scale)
  {
    Preconditions.checkPreconditionI(
      this.actions.size(),
      this.actions.size() > 0,
      size -> "Must have at least one action");

    final CaEvaluatorSingleType eval =
      this.actions.get(this.actions_ordered.get(this.actions_index));
    eval.evaluateForGlobalFrame(this.frame_start, this.frame, time_scale);

    final Int2ReferenceSortedMap<CaEvaluatedJointReadableDType> joints =
      this.eval_skeleton.jointsByID();

    final java.util.List<R2DebugLineSegment> joint_lines = new ArrayList<>();

    this.skeleton_joints.clear();
    final VectorM4D out_current = new VectorM4D();
    final VectorM4D out_parent = new VectorM4D();
    final int index_start = joints.firstIntKey();
    final int index_end = joints.lastIntKey();
    for (int index = index_start; index <= index_end; ++index) {
      Invariants.checkInvariantI(
        index,
        joints.containsKey(index),
        b -> "Joint map must contain joint " + b);

      final CaEvaluatedJointReadableDType joint_current = joints.get(index);
      MatrixM4x4D.multiplyVector4D(
        this.matrix_context4x4d,
        joint_current.transformJointObject4x4D(),
        new VectorI4D(0.0, 0.0, 0.0, 1.0),
        out_current);

      final PVectorI3F<R2SpaceWorldType> position_current =
        new PVectorI3F<>(
          (float) out_current.getXD(),
          (float) out_current.getYD(),
          (float) out_current.getZD());
      this.skeleton_joints.addInstance(position_current, 0.1f, 0.0f);

      final Optional<CaEvaluatedJointReadableDType> parent_opt =
        joint_current.parent();
      if (parent_opt.isPresent()) {
        final CaEvaluatedJointReadableDType joint_parent = parent_opt.get();

        MatrixM4x4D.multiplyVector4D(
          this.matrix_context4x4d,
          joint_parent.transformJointObject4x4D(),
          new VectorI4D(0.0, 0.0, 0.0, 1.0),
          out_parent);
        final PVectorI3F<R2SpaceWorldType> position_parent =
          new PVectorI3F<>(
            (float) out_parent.getXD(),
            (float) out_parent.getYD(),
            (float) out_parent.getZD());

        joint_lines.add(R2DebugLineSegment.of(
          position_parent,
          new PVectorI4F<>(0.8f, 1.0f, 1.0f, 1.0f),
          position_current,
          new PVectorI4F<>(1.0f, 0.8f, 1.0f, 1.0f)));
      } else {
        joint_lines.add(R2DebugLineSegment.of(
          position_current,
          new PVectorI4F<>(0.8f, 1.0f, 1.0f, 1.0f),
          position_current,
          new PVectorI4F<>(1.0f, 0.8f, 1.0f, 1.0f)));
      }
    }

    joint_lines.add(R2DebugLineSegment.of(
      new PVectorI3F<>(0.0f, 0.0f, 0.0f),
      new PVectorI4F<>(1.0f, 0.0f, 0.0f, 1.0f),
      new PVectorI3F<>(1.0f, 0.0f, 0.0f),
      new PVectorI4F<>(1.0f, 0.0f, 0.0f, 1.0f)));

    joint_lines.add(R2DebugLineSegment.of(
      new PVectorI3F<>(0.0f, 0.0f, 0.0f),
      new PVectorI4F<>(0.0f, 1.0f, 0.0f, 1.0f),
      new PVectorI3F<>(0.0f, 1.0f, 0.0f),
      new PVectorI4F<>(0.0f, 1.0f, 0.0f, 1.0f)));

    joint_lines.add(R2DebugLineSegment.of(
      new PVectorI3F<>(0.0f, 0.0f, 0.0f),
      new PVectorI4F<>(0.0f, 0.0f, 1.0f, 1.0f),
      new PVectorI3F<>(0.0f, 0.0f, -1.0f),
      new PVectorI4F<>(0.0f, 0.0f, 1.0f, 1.0f)));

    this.render_debug_parameters =
      this.render_debug_parameters.withDebugInstances(
        R2DebugInstances.builder().addAllLineSegments(joint_lines).build());

    this.render_stencils.stencilsReset();
    this.render_stencils.stencilsSetMode(
      R2SceneStencilsMode.STENCIL_MODE_INSTANCES_ARE_NEGATIVE);

    this.render_opaques.opaquesReset();
    this.render_opaques.opaquesAddSingleInstance(
      this.render_floor, this.render_floor_material);
    this.render_opaques.opaquesAddBillboardedInstance(
      this.skeleton_joints, this.render_skeleton_joint_material);

    this.render_lights.lightsReset();
    this.render_lights.lightsGetGroup(1).lightGroupAddSingle(
      this.render_light_ambient,
      this.render_light_ambient_shader);

    this.render_matrices.withObserver(
      this.matrix_view,
      this.render_projection,
      this,
      (mo, t) -> {
        final JCGLProfilingFrameType profiling_frame =
          t.render_profiling.startFrame();
        final JCGLProfilingContextType profiling_context =
          profiling_frame.getChildContext("root");
        final JCGLTextureUnitContextParentType tc_root =
          t.render_texture_units.getRootContext();
        final JCGLTextureUnitContextType tc =
          tc_root.unitContextNew();

        try {
          final JCGLFramebuffersType g_fb = t.g33.getFramebuffers();

          g_fb.framebufferDrawBind(t.render_gbuffer.primaryFramebuffer());
          t.render_gbuffer.clearBoundPrimaryFramebuffer(t.g33);
          t.main.getStencilRenderer().renderStencilsWithBoundBuffer(
            mo,
            profiling_context,
            t.main.getTextureUnitAllocator().getRootContext(),
            t.render_gbuffer.area(),
            t.render_stencils);

          t.main.getGeometryRenderer().renderGeometry(
            t.window_area,
            Optional.empty(),
            profiling_context,
            tc,
            mo,
            t.render_opaques);

          g_fb.framebufferDrawBind(t.render_lbuffer.primaryFramebuffer());
          t.render_lbuffer.clearBoundPrimaryFramebuffer(t.g33);
          t.main.getLightRenderer().renderLights(
            t.render_gbuffer,
            t.window_area,
            Optional.empty(),
            profiling_context,
            tc,
            t.render_shadows,
            mo,
            t.render_lights);

          g_fb.framebufferDrawUnbind();
          t.g33.getClear().clear(t.render_clear);
          t.render_light_applicator.runFilter(
            profiling_context,
            tc,
            R2FilterLightApplicatorParameters.builder()
              .setGeometryBuffer(t.render_gbuffer)
              .setLightDiffuseTexture(t.renderGetLightBufferDiffuse())
              .setLightSpecularTexture(t.main.getTextureDefaults().texture2DBlack())
              .setCopyDepth(R2CopyDepth.R2_COPY_DEPTH_ENABLED)
              .setOutputViewport(t.window_area)
              .build());

          t.main.getDebugVisualizerRenderer().renderScene(
            t.window_area,
            profiling_context,
            tc,
            mo,
            t.render_debug_parameters);

        } finally {
          tc.unitContextFinish(t.g33.getTextures());
        }
        return unit();
      });
  }

  private R2Texture2DUsableType renderGetLightBufferDiffuse()
  {
    return this.render_lbuffer.matchLightBuffer(
      this,
      (t, lbuffer_diffuse) -> lbuffer_diffuse.diffuseTexture(),
      (t, lbuffer_specular) -> {
        throw new UnreachableCodeException();
      },
      (t, lbuffer_diffuse) -> lbuffer_diffuse.diffuseTexture());
  }

  private void renderCamera()
  {
    final long time_now = System.nanoTime();
    final long time_diff = time_now - this.time_then;
    final double time_diff_s = (double) time_diff / 1000000000.0;
    this.time_accum = this.time_accum + time_diff_s;
    this.time_then = time_now;

    final float sim_delta = 1.0f / 60.0f;
    while (this.time_accum >= (double) sim_delta) {
      this.camera_integrator.integrate(sim_delta);

      this.snap_prev = this.snap_curr;
      this.snap_curr = this.camera.cameraMakeSnapshot();
      this.time_accum -= (double) sim_delta;
    }

    final float alpha = (float) (this.time_accum / (double) sim_delta);
    final JCameraSphericalSnapshot snap_interpolated =
      JCameraSphericalSnapshot.interpolate(
        this.snap_prev, this.snap_curr, alpha);

    snap_interpolated.cameraMakeViewMatrix(
      this.camera_context, this.matrix_view);
  }

  private void loadActions(
    final CaSkeleton skeleton)
  {
    final MatrixM4x4D.ContextMM4D c = new MatrixM4x4D.ContextMM4D();
    final CaSkeletonRestPoseDType rest_pose =
      CaSkeletonRestPose.createD(c, skeleton);

    this.eval_context =
      CaEvaluationContext.create();
    this.eval_skeleton =
      CaEvaluatedSkeletonD.create(this.eval_context, rest_pose);

    final Iterator<Tuple2<CaActionName, CaActionType>> iter =
      skeleton.actionsByName().iterator();
    while (iter.hasNext()) {
      final Tuple2<CaActionName, CaActionType> pair = iter.next();

      final CaEvaluatorSingleType eval =
        CaEvaluatorSingleD.create(
          this.eval_context,
          this.eval_skeleton,
          pair._2, 60);

      this.actions.put(pair._1, eval);
      this.actions_ordered = this.actions_ordered.append(pair._1);
      this.actions_index = this.actions_ordered.size() - 1;
    }
  }

  @Override
  public void keyPressed(
    final KeyEvent e)
  {
    NullCheck.notNull(e, "Event");

    /*
     * Ignore events that are the result of keyboard auto-repeat. This means
     * there's one single event when a key is pressed, and another when it is
     * released (as opposed to an endless stream of both when the key is held
     * down).
     */

    if ((e.getModifiers() & InputEvent.AUTOREPEAT_MASK) == InputEvent.AUTOREPEAT_MASK) {
      return;
    }

    switch (e.getKeyCode()) {

      case KeyEvent.VK_F: {
        this.camera_input.setOrbitInclinePositive(true);
        break;
      }
      case KeyEvent.VK_V: {
        this.camera_input.setOrbitInclineNegative(true);
        break;
      }

      case KeyEvent.VK_Q: {
        this.camera_input.setOrbitHeadingNegative(true);
        break;
      }
      case KeyEvent.VK_E: {
        this.camera_input.setOrbitHeadingPositive(true);
        break;
      }

      case KeyEvent.VK_G: {
        this.camera_input.setZoomingIn(true);
        break;
      }
      case KeyEvent.VK_B: {
        this.camera_input.setZoomingOut(true);
        break;
      }

      default: {
        // Nothing
        break;
      }
    }
  }

  @Override
  public void keyReleased(
    final KeyEvent e)
  {
    NullCheck.notNull(e, "Event");

    /*
     * Ignore events that are the result of keyboard auto-repeat. This means
     * there's one single event when a key is pressed, and another when it is
     * released (as opposed to an endless stream of both when the key is held
     * down).
     */

    if ((e.getModifiers() & InputEvent.AUTOREPEAT_MASK) == InputEvent.AUTOREPEAT_MASK) {
      return;
    }

    switch (e.getKeyCode()) {
      case KeyEvent.VK_ADD: {
        this.time_scale.updateAndGet(x -> {
          final Double next = Double.valueOf(x.doubleValue() + 0.01);
          LOG.debug("time scale: {}", next);
          return next;
        });
        break;
      }
      case KeyEvent.VK_SUBTRACT: {
        this.time_scale.updateAndGet(x -> {
          final Double next = Double.valueOf(x.doubleValue() - 0.01);
          LOG.debug("time scale: {}", next);
          return next;
        });
        break;
      }

      case KeyEvent.VK_R: {
        this.frame_start = this.frame;
        break;
      }

      case KeyEvent.VK_N: {
        this.actions_index = (this.actions_index + 1) % this.actions_ordered.size();
        LOG.debug("action: {}", Integer.valueOf(this.actions_index));
        break;
      }

      case KeyEvent.VK_F: {
        this.camera_input.setOrbitInclinePositive(false);
        break;
      }
      case KeyEvent.VK_V: {
        this.camera_input.setOrbitInclineNegative(false);
        break;
      }

      case KeyEvent.VK_Q: {
        this.camera_input.setOrbitHeadingNegative(false);
        break;
      }
      case KeyEvent.VK_E: {
        this.camera_input.setOrbitHeadingPositive(false);
        break;
      }

      case KeyEvent.VK_G: {
        this.camera_input.setZoomingIn(false);
        break;
      }
      case KeyEvent.VK_B: {
        this.camera_input.setZoomingOut(false);
        break;
      }

      default: {
        // Nothing
        break;
      }
    }
  }
}
