package com.io7m.jcalcium.generators;

import com.io7m.jcalcium.core.definitions.actions.CaDefinitionCurveType;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jtensors.generators.QuaternionI4DGenerator;
import com.io7m.jtensors.generators.VectorI3DGenerator;
import com.io7m.jtensors.generators.parameterized.PVectorI3DGenerator;
import com.io7m.junreachable.UnreachableCodeException;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;

/**
 * A generator for {@link CaDefinitionCurveType}.
 */

public final class CaDefinitionCurveGenerator implements Generator<CaDefinitionCurveType>
{
  private final CaDefinitionCurveOrientationGenerator curve_ori_gen;
  private final CaDefinitionCurveTranslationGenerator curve_tra_gen;
  private final CaDefinitionCurveScaleGenerator curve_sca_gen;
  private final IntegerGenerator which_gen;

  /**
   * Construct a generator.
   *
   * @param in_tree The tree of bones
   */

  public CaDefinitionCurveGenerator(
    final JointTree in_tree)
  {
    final CaCurveInterpolationGenerator interp_gen =
      new CaCurveInterpolationGenerator();
    final CaCurveEasingGenerator easing_gen =
      new CaCurveEasingGenerator();
    final QuaternionI4DGenerator quat_gen =
      new QuaternionI4DGenerator();
    final PVectorI3DGenerator<CaSpaceJointType> pvec_gen =
      new PVectorI3DGenerator<>();
    final VectorI3DGenerator vec_gen =
      new VectorI3DGenerator();

    this.curve_ori_gen =
      new CaDefinitionCurveOrientationGenerator(
        interp_gen, easing_gen, quat_gen, in_tree);
    this.curve_tra_gen =
      new CaDefinitionCurveTranslationGenerator(
        interp_gen, easing_gen, pvec_gen, in_tree);
    this.curve_sca_gen =
      new CaDefinitionCurveScaleGenerator(
        interp_gen, easing_gen, vec_gen, in_tree);

    this.which_gen = new IntegerGenerator(0, 2);
  }

  @Override
  public CaDefinitionCurveType next()
  {
    switch (this.which_gen.nextInt()) {
      case 0: {
        return this.curve_ori_gen.next();
      }
      case 1: {
        return this.curve_sca_gen.next();
      }
      case 2: {
        return this.curve_tra_gen.next();
      }
      default: {
        throw new UnreachableCodeException();
      }
    }
  }
}
