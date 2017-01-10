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

package com.io7m.jcalcium.format.protobuf3.v1;

import com.io7m.jcalcium.core.CaActionName;
import com.io7m.jcalcium.core.CaJointName;
import com.io7m.jcalcium.core.CaCurveEasing;
import com.io7m.jcalcium.core.CaCurveInterpolation;
import com.io7m.jcalcium.core.CaSkeletonName;
import com.io7m.jcalcium.core.compiled.CaJoint;
import com.io7m.jcalcium.core.compiled.CaSkeleton;
import com.io7m.jcalcium.core.compiled.actions.CaActionCurves;
import com.io7m.jcalcium.core.compiled.actions.CaActionType;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeScale;
import com.io7m.jcalcium.core.compiled.actions.CaCurveKeyframeTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveOrientation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveScale;
import com.io7m.jcalcium.core.compiled.actions.CaCurveTranslation;
import com.io7m.jcalcium.core.compiled.actions.CaCurveType;
import com.io7m.jcalcium.core.spaces.CaSpaceJointType;
import com.io7m.jcalcium.format.protobuf3.CaLoaderCorruptedData;
import com.io7m.jcalcium.format.protobuf3.CaLoaderIOException;
import com.io7m.jcalcium.loader.api.CaLoaderException;
import com.io7m.jnull.NullCheck;
import com.io7m.jorchard.core.JOTreeNode;
import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jtensors.QuaternionI4D;
import com.io7m.jtensors.VectorI3D;
import com.io7m.jtensors.parameterized.PVectorI3D;
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.collection.Array;
import javaslang.collection.IndexedSeq;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class CaV1Loader
{
  private final URI uri;
  private final InputStream stream;

  CaV1Loader(
    final URI in_uri,
    final InputStream in_stream)
  {
    this.uri = NullCheck.notNull(in_uri, "URI");
    this.stream = NullCheck.notNull(in_stream, "Stream");
  }

  private static VectorI3D scale(
    final Skeleton.V1ScaleOrBuilder v)
  {
    return new VectorI3D(v.getX(), v.getY(), v.getZ());
  }

  private static PVectorI3D<CaSpaceJointType> translation(
    final Skeleton.V1TranslationOrBuilder v)
  {
    return new PVectorI3D<>(v.getX(), v.getY(), v.getZ());
  }

  private static QuaternionI4D quaternion(
    final Skeleton.V1QuaternionOrBuilder v)
  {
    return new QuaternionI4D(v.getX(), v.getY(), v.getZ(), v.getW());
  }

  private static CaCurveInterpolation interpolation(
    final Skeleton.V1Interpolation interpolation)
  {
    switch (interpolation) {
      case V1_INTERPOLATION_LINEAR:
        return CaCurveInterpolation.CURVE_INTERPOLATION_LINEAR;
      case V1_INTERPOLATION_EXPONENTIAL:
        return CaCurveInterpolation.CURVE_INTERPOLATION_EXPONENTIAL;
      case V1_INTERPOLATION_CONSTANT:
        return CaCurveInterpolation.CURVE_INTERPOLATION_CONSTANT;
      case V1_INTERPOLATION_QUADRATIC:
        return CaCurveInterpolation.CURVE_INTERPOLATION_QUADRATIC;
      case UNRECOGNIZED:
        throw new IllegalArgumentException(
          "Unrecognized interpolation type");
    }
    throw new UnreachableCodeException();
  }

  private static CaCurveEasing easing(
    final Skeleton.V1Easing easing)
  {
    switch (easing) {
      case V1_EASING_IN:
        return CaCurveEasing.CURVE_EASING_IN;
      case V1_EASING_IN_OUT:
        return CaCurveEasing.CURVE_EASING_IN_OUT;
      case V1_EASING_OUT:
        return CaCurveEasing.CURVE_EASING_OUT;
      case UNRECOGNIZED:
        throw new IllegalArgumentException("Unrecognized easing type");
    }
    throw new UnreachableCodeException();
  }

  private static CaCurveKeyframeTranslation curveKeyframeTranslation(
    final Skeleton.V1CurveKeyframeTranslation translation)
  {
    final CaCurveKeyframeTranslation.Builder b = CaCurveKeyframeTranslation.builder();
    b.setTranslation(translation(translation.getTranslation()));
    b.setEasing(easing(translation.getEasing()));
    b.setInterpolation(interpolation(translation.getInterpolation()));
    b.setIndex(translation.getIndex());
    return b.build();
  }

  private static SortedMap<Integer, CaCurveKeyframeTranslation> curveTranslationKeyframes(
    final Map<Integer, Skeleton.V1CurveKeyframeTranslation> tr)
  {
    SortedMap<Integer, CaCurveKeyframeTranslation> frames = TreeMap.empty();
    for (final Integer key : tr.keySet()) {
      frames = frames.put(key, curveKeyframeTranslation(tr.get(key)));
    }
    return frames;
  }

  private static CaCurveTranslation curveTranslation(
    final CaActionName action,
    final Skeleton.V1CurveTranslation translation)
  {
    final CaCurveTranslation.Builder b = CaCurveTranslation.builder();
    b.setKeyframes(curveTranslationKeyframes(translation.getKeyframesMap()));
    b.setJoint(CaJointName.of(translation.getJoint()));
    b.setAction(action);
    return b.build();
  }

  private static CaCurveKeyframeScale curveKeyframeScale(
    final Skeleton.V1CurveKeyframeScale scale)
  {
    final CaCurveKeyframeScale.Builder b = CaCurveKeyframeScale.builder();
    b.setScale(scale(scale.getScale()));
    b.setEasing(easing(scale.getEasing()));
    b.setInterpolation(interpolation(scale.getInterpolation()));
    b.setIndex(scale.getIndex());
    return b.build();
  }

  private static SortedMap<Integer, CaCurveKeyframeScale> curveScaleKeyframes(
    final Map<Integer, Skeleton.V1CurveKeyframeScale> tr)
  {
    SortedMap<Integer, CaCurveKeyframeScale> frames = TreeMap.empty();
    for (final Integer key : tr.keySet()) {
      frames = frames.put(key, curveKeyframeScale(tr.get(key)));
    }
    return frames;
  }

  private static CaCurveScale curveScale(
    final CaActionName action,
    final Skeleton.V1CurveScale scale)
  {
    final CaCurveScale.Builder b = CaCurveScale.builder();
    b.setKeyframes(curveScaleKeyframes(scale.getKeyframesMap()));
    b.setJoint(CaJointName.of(scale.getJoint()));
    b.setAction(action);
    return b.build();
  }

  private static CaCurveKeyframeOrientation curveKeyframeOrientation(
    final Skeleton.V1CurveKeyframeOrientation orientation)
  {
    final CaCurveKeyframeOrientation.Builder b = CaCurveKeyframeOrientation.builder();
    b.setOrientation(quaternion(orientation.getOrientation()));
    b.setEasing(easing(orientation.getEasing()));
    b.setInterpolation(interpolation(orientation.getInterpolation()));
    b.setIndex(orientation.getIndex());
    return b.build();
  }

  private static SortedMap<Integer, CaCurveKeyframeOrientation>
  curveOrientationKeyframes(
    final Map<Integer, Skeleton.V1CurveKeyframeOrientation> tr)
  {
    SortedMap<Integer, CaCurveKeyframeOrientation> frames = TreeMap.empty();
    for (final Integer key : tr.keySet()) {
      frames = frames.put(key, curveKeyframeOrientation(tr.get(key)));
    }
    return frames;
  }

  private static CaCurveOrientation curveOrientation(
    final CaActionName action,
    final Skeleton.V1CurveOrientation orientation)
  {
    final CaCurveOrientation.Builder b = CaCurveOrientation.builder();
    b.setKeyframes(curveOrientationKeyframes(orientation.getKeyframesMap()));
    b.setJoint(CaJointName.of(orientation.getJoint()));
    b.setAction(action);
    return b.build();
  }

  private static CaCurveType curve(
    final CaActionName action,
    final Skeleton.V1Curve curve)
  {
    switch (curve.getCurveCase()) {
      case TRANSLATION:
        return curveTranslation(action, curve.getTranslation());
      case SCALE:
        return curveScale(action, curve.getScale());
      case ORIENTATION:
        return curveOrientation(action, curve.getOrientation());
      case CURVE_NOT_SET:
        throw new IllegalArgumentException("Curve is unset");
    }
    throw new UnreachableCodeException();
  }

  private static CaJoint bone(
    final Skeleton.V1JointOrBuilder v_bone)
  {
    final CaJoint.Builder bb = CaJoint.builder();
    bb.setName(CaJointName.of(v_bone.getName()));
    bb.setId(v_bone.getId());
    bb.setScale(scale(v_bone.getScale()));
    bb.setOrientation(quaternion(v_bone.getOrientation()));
    bb.setTranslation(translation(v_bone.getTranslation()));
    return bb.build();
  }

  private static SortedMap<CaActionName, CaActionType> actions(
    final Map<String, Skeleton.V1Action> actions)
  {
    TreeMap<CaActionName, CaActionType> results = TreeMap.empty();
    for (final String name : actions.keySet()) {
      final Skeleton.V1Action act = actions.get(name);
      results = results.put(CaActionName.of(name), action(name, act));
    }
    return results;
  }

  private static CaActionType action(
    final String name,
    final Skeleton.V1Action act)
  {
    switch (act.getActionCase()) {
      case CURVES:
        return actionCurves(act.getCurves());
      case ACTION_NOT_SET:
        throw new IllegalArgumentException("Invalid action (not set): " + name);
    }

    throw new UnreachableCodeException();
  }

  private static CaActionType actionCurves(
    final Skeleton.V1ActionCurves curves)
  {
    final CaActionCurves.Builder ab = CaActionCurves.builder();
    final CaActionName action_name = CaActionName.of(curves.getName());
    ab.setName(action_name);
    ab.setCurves(actionCurvesByJoint(action_name, curves.getCurvesMap()));
    ab.setFramesPerSecond(curves.getFramesPerSecond());
    return ab.build();
  }

  private static SortedMap<CaJointName, IndexedSeq<CaCurveType>> actionCurvesByJoint(
    final CaActionName action,
    final Map<String, Skeleton.V1CurveList> curves)
  {
    TreeMap<CaJointName, IndexedSeq<CaCurveType>> results = TreeMap.empty();
    for (final String name : curves.keySet()) {
      results = results.put(
        CaJointName.of(name),
        Array.ofAll(
          curves.get(name)
            .getCurvesList()
            .stream()
            .map(curve -> curve(action, curve))
            .collect(Collectors.toList())));
    }
    return results;
  }

  CaSkeleton run()
    throws CaLoaderException
  {
    try {
      final Skeleton.V1Skeleton sk =
        Skeleton.V1Skeleton.parseFrom(this.stream);
      final JOTreeNodeType<CaJoint> node =
        this.bones(sk.getJointsMap());

      final CaSkeleton.Builder cb = CaSkeleton.builder();
      cb.setJoints(node);
      cb.setName(CaSkeletonName.of(sk.getName()));
      cb.setActionsByName(actions(sk.getActionsMap()));

      return cb.build();
    } catch (final IOException e) {
      throw new CaLoaderIOException(this.uri, e);
    } catch (final IllegalArgumentException e) {
      throw new CaLoaderCorruptedData(this.uri, e, e.getMessage());
    }
  }

  private JOTreeNodeType<CaJoint> bones(
    final Map<Integer, Skeleton.V1Joint> bones)
    throws CaLoaderCorruptedData
  {
    /*
     * The compiler will sequentially number bones in topological order. This
     * means that to quickly reconstruct the original hierarchy, the bones
     * must be sorted by ID and then added to a tree sequentially.
     */

    final List<Skeleton.V1Joint> bones_ordered =
      new ArrayList<>(bones.size());
    for (final Integer bone_id : bones.keySet()) {
      bones_ordered.add(bones.get(bone_id));
    }

    bones_ordered.sort(
      (o1, o2) -> Integer.compareUnsigned(o1.getId(), o2.getId()));

    if (bones_ordered.isEmpty()) {
      throw new CaLoaderCorruptedData(this.uri, "No parseable bones");
    }

    final Skeleton.V1Joint root_current =
      bones_ordered.get(0);
    final CaJoint root_bone =
      bone(root_current);
    final JOTreeNodeType<CaJoint> root_node =
      JOTreeNode.create(root_bone);
    SortedMap<Integer, JOTreeNodeType<CaJoint>> nodes_by_id =
      TreeMap.of(Integer.valueOf(0), root_node);

    for (int index = 1; index < bones_ordered.size(); ++index) {
      final Skeleton.V1Joint bone =
        bones_ordered.get(index);
      final JOTreeNodeType<CaJoint> node_parent =
        nodes_by_id.get(Integer.valueOf(bone.getParent())).get();
      final CaJointName node_name =
        CaJointName.of(bone.getName());
      final JOTreeNodeType<CaJoint> node = JOTreeNode.create(CaJoint.of(
        node_name,
        bone.getId(),
        translation(bone.getTranslation()),
        quaternion(bone.getOrientation()),
        scale(bone.getScale())));
      node_parent.childAdd(node);
      nodes_by_id = nodes_by_id.put(Integer.valueOf(bone.getId()), node);
    }

    return root_node;
  }
}
