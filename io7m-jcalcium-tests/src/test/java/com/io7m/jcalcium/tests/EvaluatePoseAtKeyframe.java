package com.io7m.jcalcium.tests;

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPose;
import com.io7m.jcalcium.core.compiled.CaSkeletonRestPoseDType;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jcalcium.core.spaces.CaSpaceObjectType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatedJointDType;
import com.io7m.jcalcium.evaluator.api.CaEvaluatorSingleDType;
import com.io7m.jcalcium.evaluator.main.CaEvaluatorSingleD;
import com.io7m.jcalcium.loader.api.CaLoaderException;
import com.io7m.jcalcium.loader.api.CaLoaderFormatProviderType;
import com.io7m.jcalcium.loader.api.CaLoaderType;
import com.io7m.jtensors.MatrixM4x4D;
import com.io7m.jtensors.VectorI4D;
import com.io7m.jtensors.VectorM4D;
import com.io7m.jtensors.parameterized.PMatrixReadable4x4DType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.ServiceLoader;

import static com.io7m.jfunctional.Unit.unit;

public final class EvaluatePoseAtKeyframe
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(EvaluatePoseAtKeyframe.class);
  }

  private EvaluatePoseAtKeyframe()
  {

  }

  private static CaLoaderFormatProviderType findLoaderProvider(
    final String file)
  {
    final ServiceLoader<CaLoaderFormatProviderType> loader =
      ServiceLoader.load(CaLoaderFormatProviderType.class);

    LOG.debug("attempting to infer format from file suffix");
    final int index = file.lastIndexOf('.');
    if (index != -1) {
      final String suffix = file.substring(index + 1);
      final Iterator<CaLoaderFormatProviderType> providers =
        loader.iterator();
      while (providers.hasNext()) {
        final CaLoaderFormatProviderType current_provider =
          providers.next();
        if (current_provider.loaderFormat().suffix().equals(suffix)) {
          LOG.debug("using provider: {}", current_provider);
          return current_provider;
        }
      }
    }

    LOG.error("Could not find a provider for the format '{}'", file);
    return null;
  }

  public static void main(
    final String[] args)
    throws IOException, CaLoaderException
  {
    final CaLoaderFormatProviderType provider =
      findLoaderProvider(args[0]);
    final CaLoaderType loader =
      provider.loaderCreate();

    final Path path = Paths.get(args[0]);
    final CaActionName act = CaActionName.of(args[1]);
    final int keyframe = Integer.parseInt(args[2]);

    try (final InputStream is = Files.newInputStream(path)) {
      final MatrixM4x4D.ContextMM4D context =
        new MatrixM4x4D.ContextMM4D();
      final CaSkeleton skeleton =
        loader.loadCompiledSkeletonFromStream(is, path.toUri());
      final CaSkeletonRestPoseDType rest_pose =
        CaSkeletonRestPose.createD(context, skeleton);
      final CaEvaluatorSingleDType eval =
        CaEvaluatorSingleD.create(
          rest_pose, skeleton.actionsByName().get(act).get(), 60);

      eval.evaluateForGlobalFrame(0L, keyframe, 1.0);
      eval.evaluatedJointsD().forEachBreadthFirst(unit(), (i, depth, node) -> {
        final CaEvaluatedJointDType joint = node.value();
        final PMatrixReadable4x4DType<CaSpaceJointType, CaSpaceObjectType> m =
          joint.transformJointObject4x4D();
        final VectorM4D out = new VectorM4D();
        MatrixM4x4D.multiplyVector4D(
          context,
          m,
          new VectorI4D(0.0, 0.0, 0.0, 1.0),
          out);
        System.out.printf(
          "%f,%f,%f\n",
          Double.valueOf(out.getXD()),
          Double.valueOf(out.getYD()),
          Double.valueOf(out.getZD()));
      });
    }
  }
}
