package com.io7m.jcalcium.tests;

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.spaces.CaSpaceJointAbsoluteType;
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

public final class EvaluatePoseAtAllKeyframes
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(EvaluatePoseAtAllKeyframes.class);
  }

  private EvaluatePoseAtAllKeyframes()
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
    final CaActionName act_name = CaActionName.of(args[1]);
    final int keyframe_max = Integer.parseInt(args[2]);

    try (final InputStream is = Files.newInputStream(path)) {
      final CaSkeleton skel =
        loader.loadCompiledSkeletonFromStream(is, path.toUri());
      final CaActionType action =
        skel.actionsByName().get(act_name).get();
      final CaEvaluatorSingleDType eval =
        CaEvaluatorSingleD.create(skel, action, 60);
      final MatrixM4x4D.ContextMM4D context =
        new MatrixM4x4D.ContextMM4D();

      for (int index = 0; index < keyframe_max; ++index) {
        eval.evaluateForGlobalFrame(0L, index, 1.0);

        System.out.println("Keyframe " + index + ":");
        eval.evaluatedJointsD().forEachBreadthFirst(unit(), (i, depth, node) -> {
          final CaEvaluatedJointDType joint = node.value();
          final PMatrixReadable4x4DType<CaSpaceObjectType, CaSpaceJointAbsoluteType> m =
            joint.transformAbsolute4x4D();
          final VectorM4D out = new VectorM4D();
          MatrixM4x4D.multiplyVector4D(
            context,
            m,
            new VectorI4D(0.0, 0.0, 0.0, 1.0),
            out);
          System.out.printf(
            "%d: %f,%f,%f\n",
            Integer.valueOf(joint.id()),
            Double.valueOf(out.getXD()),
            Double.valueOf(out.getYD()),
            Double.valueOf(out.getZD()));
        });
        System.out.println();
      }
    }
  }
}
