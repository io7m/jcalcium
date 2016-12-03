#
# Copyright © 2016 <code@io7m.com> http://io7m.com
#
# Permission to use, copy, modify, and/or distribute this software for any
# purpose with or without fee is hereby granted, provided that the above
# copyright notice and this permission notice appear in all copies.
#
# THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
# WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
# ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
# WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
# ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
# OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
#

import bmesh
import bpy
import bpy_extras.io_utils
import bpy_types
import datetime
import io
import mathutils
import os
import json

class CalciumNoArmatureSelected(Exception):
  def __init__(self, value):
    self.value = value
  #end
  def __str__(self):
    return repr(self.value)
  #end
#endclass

class CalciumTooManyArmaturesSelected(Exception):
  def __init__(self, value):
    self.value = value
  #end
  def __str__(self):
    return repr(self.value)
  #end
#endclass

class CalciumExportFailed(Exception):
  def __init__(self, value):
    self.value = value
  #end
  def __str__(self):
    return repr(self.value)
  #end
#endclass

CALCIUM_LOG_MESSAGE_DEBUG = 0
CALCIUM_LOG_MESSAGE_INFO  = 1
CALCIUM_LOG_MESSAGE_ERROR = 2

#
# A logger that writes to a log file and stdout.
#

class CalciumLogger:
  def __init__(self, severity, file):
    assert type(severity) == int
    assert type(file) == io.TextIOWrapper

    self.__file   = file
    self.severity = severity
    self.counts   = {}
    self.counts[CALCIUM_LOG_MESSAGE_DEBUG] = 0
    self.counts[CALCIUM_LOG_MESSAGE_INFO]  = 0
    self.counts[CALCIUM_LOG_MESSAGE_ERROR] = 0

    self.debug("debug logging enabled")
  #end

  def __name(self, severity):
    assert type(severity) == int
    if severity == CALCIUM_LOG_MESSAGE_DEBUG:
      return "debug"
    #end
    if severity == CALCIUM_LOG_MESSAGE_INFO:
      return "info"
    #end
    if severity == CALCIUM_LOG_MESSAGE_ERROR:
      return "error"
    #end
  #end

  def log(self, severity, message):
    assert type(severity) == int
    assert type(message) == str

    self.counts[severity] = self.counts[severity] + 1
    if severity >= self.severity:
      text = "calcium: " + self.__name(severity) + ": " + message
      self.__file.write(text + "\n")
      print(text)
    #endif
  #end

  def error(self, message):
    self.log(CALCIUM_LOG_MESSAGE_ERROR, message)
  #end

  def debug(self, message):
    self.log(CALCIUM_LOG_MESSAGE_DEBUG, message)
  #end

  def info(self, message):
    self.log(CALCIUM_LOG_MESSAGE_INFO, message)
  #end

#endclass

#
# A single bone.
#

class CalciumBone:
  def __init__(self, name, parent, translation, orientation, scale):
    assert type(name) == str
    assert type(translation) == mathutils.Vector
    assert type(orientation) == mathutils.Quaternion
    assert type(scale) == mathutils.Vector

    if parent != None:
      assert type(parent) == str
    #endif

    self.name = name
    self.parent = parent
    self.translation = translation
    self.orientation = orientation
    self.scale = scale
  #end

  def toJSON(self):
    data = {}
    data['name'] = self.name
    if self.parent != None:
      data['parent'] = self.parent
    #endif
    data['translation'] = [self.translation.x, self.translation.y, self.translation.z]
    data['orientation-xyzw'] = [self.orientation.x, self.orientation.y, self.orientation.z, self.orientation.w]
    data['scale'] = [self.scale.x, self.scale.y, self.scale.z]
    return data
  #end
#endclass

#
# A single keyframe that has not yet been evaluated.
#

class CalciumKeyframeUnevaluated:
  def __init__(self, index, interpolation, easing):
    assert type(index) == int
    assert type(interpolation) == str
    assert type(easing) == str

    self.index = index
    self.interpolation = interpolation
    self.easing = easing
  #end
#endclass

#
# A single keyframe.
#

class CalciumKeyframe:
  def __init__(self, index, interpolation, easing, kind, data):
    assert type(index) == int
    assert type(interpolation) == str
    assert type(easing) == str
    assert type(kind) == str

    if kind == 'translation':
      assert type(data) == mathutils.Vector
    elif kind == 'scale':
      assert type(data) == mathutils.Vector
    elif kind == 'orientation':
      assert type(data) == mathutils.Quaternion
    else:
      assert False, ("Unrecognized kind: " + kind)
    #endif

    self.index = index
    self.interpolation = interpolation
    self.easing = easing
    self.kind = kind
    self.data = data
  #end

  def toJSON(self):
    data = {}
    data['index'] = self.index
    data['interpolation'] = self.interpolation
    data['easing'] = self.easing

    if self.kind == 'translation':
      assert type(self.data) == mathutils.Vector
      data['translation'] = [self.data.x, self.data.y, self.data.z]
    elif self.kind == 'scale':
      assert type(self.data) == mathutils.Vector
      data['scale'] = [self.data.x, self.data.y, self.data.z]
    elif self.kind == 'orientation':
      assert type(self.data) == mathutils.Quaternion
      data['quaternion-xyzw'] = [self.data.x, self.data.y, self.data.z, self.data.w]
    else:
      assert False, ("Unrecognized kind: " + kind)
    #endif

    return data
  #end
#endclass

#
# A single curve that holds keyframes that affect a single property of a bone
# (such as translation, scale, orientation, etc).
#

class CalciumCurve:
  def __init__(self, bone, kind, keyframes):
    assert type(bone) == str
    assert type(kind) == str
    assert type(keyframes) == type({})

    for keyframe in keyframes.values():
      assert type(keyframe) == CalciumKeyframe
    #endfor

    self.bone = bone
    self.kind = kind
    self.keyframes = keyframes
  #endif

  def toJSON(self):
    data = {}
    data['bone'] = self.bone
    data['type'] = self.kind

    keyframes_json = []
    for keyframe in self.keyframes.values():
      assert type(keyframe) == CalciumKeyframe
      keyframes_json.append(keyframe.toJSON())
    #endfor

    data['keyframes'] = keyframes_json
    return data
  #end
#endclass

#
# A single action that holds a set of curves.
#

class CalciumAction:
  def __init__(self, kind, name, fps, data):
    assert type(kind) == str
    assert type(name) == str
    assert type(fps) == int

    self.kind = kind
    self.name = name
    self.fps = fps

    if kind == 'curves':
      assert type(data) == list
      for curve in data:
        assert type(curve) == CalciumCurve
      #end

      self.data = data
    else:
      assert False, ("Unrecognized action kind: " + kind)
    #endif
  #end

  def toJSON(self):
    data = {}
    data['type'] = self.kind
    data['name'] = self.name
    data['frames-per-second'] = self.fps

    if self.kind == 'curves':
      data_curves = []
      for curve in self.data:
        assert type(curve) == CalciumCurve
        data_curves.append(curve.toJSON())
      #end
      data['curves'] = data_curves
    else:
      assert False, ("Unrecognized action kind: " + kind)
    #endif

    return data
  #end
#endclass

#
# A skeleton that aggregates a set of bones and actions.
#

class CalciumSkeleton:
  def __init__(self, name, bones, actions):
    assert type(name) == str
    assert type(bones) == list
    assert type(actions) == list

    for bone in bones:
      assert type(bone) == CalciumBone
    #endfor

    for action in actions:
      assert type(action) == CalciumAction
    #endfor

    self.name = name
    self.bones = bones
    self.actions = actions
  #end

  def toJSON(self):
    data = {}
    data['name'] = self.name

    data_bones = []
    for bone in self.bones:
      assert type(bone) == CalciumBone
      data_bones.append(bone.toJSON())
    #endfor

    data_actions = []
    for action in self.actions:
      assert type(action) == CalciumAction
      data_actions.append(action.toJSON())
    #endfor

    data['bones'] = data_bones
    data['actions'] = data_actions
    return data
  #end
#endclass

class CalciumExporter:

  def __init__(self, options):
    assert type(options) == type({})

    self.__axis_matrix = options['conversion_matrix']
    assert type(self.__axis_matrix) == mathutils.Matrix

    self.__logger = None
    self.__logger_severity = CALCIUM_LOG_MESSAGE_DEBUG

    v = options['verbose']
    assert type(v) == bool

    if v:
      self.__logger_severity = CALCIUM_LOG_MESSAGE_DEBUG
    else:
      self.__logger_severity = CALCIUM_LOG_MESSAGE_INFO
    #end
  #end

  def __transformScaleToExport(self, v):
    assert type(v) == mathutils.Vector
    return mathutils.Vector((v.x, v.z, v.y))
  #end

  def __transformTranslationToExport(self, v):
    assert type(v) == mathutils.Vector
    return self.__axis_matrix * v
  #end

  def __transformOrientationToExport(self, q):
    assert type(q) == mathutils.Quaternion

    aa = q.to_axis_angle()
    axis = aa[0]
    axis = self.__axis_matrix * axis
    return mathutils.Quaternion(axis, aa[1])
  #end

  __supported_interpolation = {
    "CONSTANT" : "constant",
    "LINEAR"   : "linear",
    "QUAD"     : "quadratic",
    "EXPO"     : "exponential"
  }

  def __transformInterpolation(self, i):
    return self.__supported_interpolation.get(i, None)
  #end

  __supported_easing = {
    "EASE_IN"     : "in",
    "EASE_OUT"    : "out",
    "EASE_IN_OUT" : "in-out"
  }

  def __transformEasing(self, e):
    return self.__supported_easing.get(e, None)
  #end

  #
  # Check that for all keyframes k in a channel c, there is a corresponding
  # keyframe in all other channels at the same frame as k.
  #

  def __checkKeyframesCorresponding(self, action, group_name, group_channels):
    assert type(action) == bpy.types.Action
    assert type(group_name) == str
    assert type(group_channels) == type({})

    self.__logger.debug("[%s] __checkKeyframesCorresponding %s" % (action.name, group_name))

    ok = True
    for channel_name, channel_frames in group_channels.items():
      for channel_name_other, channel_frames_other in group_channels.items():

        if channel_name_other == channel_name:
          continue
        #endif

        for frame_index in channel_frames.keys():
          if not (frame_index in channel_frames_other):
            text =  "A keyframe for a channel of a group is missing corresponding keyframes in the other group channels.\n"
            text += "  Action:                        %s\n" % action.name
            text += "  Group:                         %s\n" % group_name
            text += "  Frame at:                      %d\n" % frame_index
            text += "  Channel:                       %s\n" % channel_name
            text += "  Channel with missing keyframe: %s\n" % channel_name_other
            text += "  Possible solution: Create a keyframe at frame %d for channel %s of group %s\n" % (frame_index, channel_name_other, group_name)
            self.__logger.error(text)
            ok = False
            continue
          #endif
        #endfor
      #endfor
    #endfor

    return ok
  #end

  #
  # Check that all of the given group channels have the same number of
  # keyframes.
  #

  def __checkKeyframesCountsEqual(self, action, group_name, group_channels):
    assert type(action) == bpy.types.Action
    assert type(group_name) == str
    assert type(group_channels) == type({})

    self.__logger.debug("[%s] __checkKeyframesCountsEqual %s" % (action.name, group_name))

    counts = {}
    for channel_name, channel in group_channels.items():
      assert type(channel_name) == str
      assert type(channel) == bpy.types.FCurve
      counts[channel_name] = len(channel.keyframe_points)
    #endfor

    uniques = len(set(counts.values()))
    if uniques > 1:
      text  = "The channels of a group have a different number of keyframes.\n"
      text += "  Action:                      %s\n" % action.name
      text += "  Group:                       %s\n" % group_name

      for channel_name, count in counts.items():
        text += "  Keyframe count for channel %s: %d\n" % (channel_name, count)
      #endfor

      text += "  Solution: Create a matching number of keyframes for all channels in the group\n"
      self.__logger.error(text)
      return False
    #endif

    assert uniques == 1
    return True
  #end

  #
  # Check that all of the channels of a given group are present. If none
  # of them are present, then the group is simply assumed not to exist and
  # ignored.
  #

  def __checkAllChannelsArePresent(self, action, group_name, group_channels):
    assert type(action) == bpy.types.Action
    assert type(group_name) == str
    assert type(group_channels) == type({})

    self.__logger.debug("[%s] __checkAllChannelsArePresent %s" % (action.name, group_name))

    #
    # If all of the channels are missing, then the group is simply assumed
    # not to exist.
    #

    missing = 0
    for channel_name, channel in group_channels.items():
      if channel == None:
        missing += 1
      #endif
    #endif

    self.__logger.debug("[%s] __checkAllChannelsArePresent %s: missing %d" % (action.name, group_name, missing))
    if missing == len(group_channels):
      self.__logger.debug("[%s] group %s has no keyframes, ignoring it" % (action.name, group_name))
      return False
    #endif

    #
    # However, if one or more channels are missing, then this is an error.
    #

    if missing > 0:
      for channel_name, channel in group_channels.items():
        text =  "No keyframes are defined for a channel of a group.\n"
        text += "  Action:   %s\n" % action.name
        text += "  Group:    %s\n" % group_name
        text += "  Channel:  %s\n" % channel_name
        text += "  Solution: Create the same number of keyframes for all channels of the group\n"
        self.__logger.error(text)
      #endfor
      return False
    #endif

    return True
  #end

  #
  # For the given group, collect all of the keyframes.
  #

  def __calculateKeyframesCollect(self, action, group_name, group_channels):
    assert type(action) == bpy.types.Action
    assert type(group_name) == str
    assert type(group_channels) == type({})

    self.__logger.debug("[%s] __calculateKeyframesCollect %s" % (action.name, group_name))

    keyframes_by_channel = {}
    for channel_name, channel in group_channels.items():
      assert type(channel_name) == str
      assert type(channel) == bpy.types.FCurve

      channel_frames = {}
      for frame in channel.keyframe_points:
        channel_frames[int(frame.co.x)] = frame
      #endfor
      keyframes_by_channel[channel_name] = channel_frames
    #endfor

    return keyframes_by_channel
  #end

  #
  # Collect all keyframes for export. This assumes that all of the other __calculateKeyframes
  # preconditions have been evaluated.
  #

  def __calculateKeyframesCollectForExport(self, action, group_name, keyframes_by_channel):
    assert type(action) == bpy.types.Action
    assert type(group_name) == str
    assert type(keyframes_by_channel) == type({})

    self.__logger.debug("[%s] __calculateKeyframesCollectForExport %s" % (action.name, group_name))

    error = False
    keyframes = {}
    for channel_name, channel_keyframes in keyframes_by_channel.items():
      for keyframe_index, keyframe in channel_keyframes.items():

        self.__logger.debug("[%s][%s][keyframe %d]: interpolation %s" % (action.name, group_name, keyframe_index, keyframe.interpolation))
        self.__logger.debug("[%s][%s][keyframe %d]: easing %s" % (action.name, group_name, keyframe_index, keyframe.easing))

        #
        # The interpolation type must be one of the supported types, and it
        # must be the same for all channels at this keyframe. We don't support,
        # for example, linear interpolation of the 'x' component of a vector
        # whilst concurrently performing exponential interpolation of the 'y'
        # component of a vector; they must all be the same interpolation type.
        #

        ex_interpolation = self.__transformInterpolation(keyframe.interpolation)
        if ex_interpolation == None:
          text = "The keyframe interpolation type is not supported.\n"
          text += "  Action:        %s\n" % action.name
          text += "  Group:         %s\n" % group_name
          text += "  Channel:       %s\n" % channel_name
          text += "  Keyframe:      %d\n" % keyframe_index
          text += "  Interpolation: %s\n" % keyframe.interpolation
          text += "  Possible solution: Use a supported interpolation type (%s)\n" % list(self.__supported_interpolation.values())
          self.__logger.error(text)
          error = True
        else:
          if keyframe_index in keyframes:
            existing = keyframes[keyframe_index]
            if existing != None:
              assert type(existing) == CalciumKeyframeUnevaluated

              if existing.interpolation != ex_interpolation:
                text  = "The interpolation value is not the same for all channels at this keyframe.\n"
                text += "  Action:                         %s\n" % action.name
                text += "  Group:                          %s\n" % group_name
                text += "  Channel:                        %s\n" % channel_name
                text += "  Keyframe:                       %d\n" % keyframe_index
                text += "  Interpolation:                  %s\n" % keyframe.interpolation
                text += "  Interpolation in other channel: %s\n" % existing.interpolation
                text += "  Possible solution: Set the interpolation value to %s for all channels at this keyframe\n" % existing.interpolation
                self.__logger.error(text)
                error = True
              #endif
            #endif
          #endif
        #endif

        #
        # The easing type must be one of the supported types, and it
        # must be the same for all channels at this keyframe. We don't support,
        # for example, easing some components of a vector
        # without also easing the other components concurrently; all components
        # must have the same easing.
        #

        ex_easing = self.__transformEasing(keyframe.easing)
        if ex_easing == None:
          text = "The keyframe easing type is not supported.\n"
          text += "  Action:        %s\n" % action.name
          text += "  Group:         %s\n" % group_name
          text += "  Channel:       %s\n" % channel_name
          text += "  Keyframe:      %d\n" % keyframe_index
          text += "  Interpolation: %s\n" % keyframe.easing
          text += "  Possible solution: Use a supported easing type (%s)\n" % list(self.__supported_easing.values())
          self.__logger.error(text)
          error = True
        else:
          if keyframe_index in keyframes:
            existing = keyframes[keyframe_index]
            if existing != None:
              assert type(existing) == CalciumKeyframeUnevaluated

              if existing.easing != ex_easing:
                text  = "The easing value is not the same for all channels at this keyframe.\n"
                text += "  Action:                         %s\n" % action.name
                text += "  Group:                          %s\n" % group_name
                text += "  Channel:                        %s\n" % channel_name
                text += "  Keyframe:                       %d\n" % keyframe_index
                text += "  Interpolation:                  %s\n" % keyframe.easing
                text += "  Interpolation in other channel: %s\n" % existing.easing
                text += "  Possible solution: Set the easing value to %s for all channels at this keyframe\n" % existing.easing
                self.__logger.error(text)
                error = True
              #endif
            #endif
          #endif
        #endif

        if not error:
          assert type(ex_interpolation) == str
          assert type(ex_easing) == str
          keyframes[keyframe_index] = CalciumKeyframeUnevaluated(keyframe_index, ex_interpolation, ex_easing)
        #endif
      #endfor
    #endfor

    if error:
      return None
    #endif

    return keyframes
  #end

  #
  # Calculate all keyframes for all channels in the given group.
  #

  def __calculateKeyframesForCurves(self, action, group_name, group_channels):
    assert type(action) == bpy.types.Action
    assert type(group_name) == str
    assert type(group_channels) == type({})

    self.__logger.debug("[%s] __calculateKeyframesForCurves %s" % (action.name, group_name))

    if not self.__checkAllChannelsArePresent(action, group_name, group_channels):
      return None
    #endif

    if not self.__checkKeyframesCountsEqual(action, group_name, group_channels):
      return None
    #endif

    keyframes_by_channel = self.__calculateKeyframesCollect(action, group_name, group_channels)
    if not self.__checkKeyframesCorresponding(action, group_name, keyframes_by_channel):
      return None
    #endif

    return self.__calculateKeyframesCollectForExport(action, group_name, keyframes_by_channel)
  #end

  #
  # Aggregate all of the translation curves for the given bone in the given action.
  #

  def __makeBoneCurvesTranslation(self, armature, action, bone_name):
    assert type(armature) == bpy_types.Object
    assert type(action) == bpy.types.Action
    assert armature.type == 'ARMATURE'
    assert type(bone_name) == str

    group_name          = 'pose.bones["%s"].location' % bone_name
    group_channels      = {}
    group_channels["X"] = action.fcurves.find(group_name, 0)
    group_channels["Y"] = action.fcurves.find(group_name, 1)
    group_channels["Z"] = action.fcurves.find(group_name, 2)

    frames = self.__calculateKeyframesForCurves(action, group_name, group_channels)
    if frames == None:
      return None
    #endif

    frames_count = len(frames)
    self.__logger.debug("action[%s]: translation frame count: %d" % (action.name, frames_count))
    if frames_count > 0:
      assert bone_name in armature.pose.bones, "No bone %s in armature" % bone_name
      bone = armature.pose.bones[bone_name]
      assert type(bone) == bpy_types.PoseBone

      #
      # Evaluate the final state of the bone at each of the keyframes.
      #

      evaluated_keyframes = {}
      for index in sorted(frames.keys()):
        assert type(index) == int
        frame = frames[index]
        assert type(frame) == CalciumKeyframeUnevaluated

        bpy.context.scene.frame_set(index)

        value = self.__transformTranslationToExport(bone.matrix_basis.to_translation())
        keyframe = CalciumKeyframe(index, frame.interpolation, frame.easing, 'translation', value)
        evaluated_keyframes[index] = keyframe
      #end

      self.__logger.debug("action[%s]: returning %d evaluated frames" % (action.name, len(evaluated_keyframes)))
      assert len(evaluated_keyframes) == frames_count
      return CalciumCurve(bone_name, 'translation', evaluated_keyframes)
    #endif

    self.__logger.debug("action[%s]: no frames, returning None" % action.name)
    return None
  #end

  #
  # Aggregate all of the scale curves for the given bone in the given action.
  #

  def __makeBoneCurvesScale(self, armature, action, bone_name):
    assert type(armature) == bpy_types.Object
    assert type(action) == bpy.types.Action
    assert armature.type == 'ARMATURE'
    assert type(bone_name) == str

    group_name = 'pose.bones["%s"].scale' % bone_name
    group_channels      = {}
    group_channels["X"] = action.fcurves.find(group_name, 0)
    group_channels["Y"] = action.fcurves.find(group_name, 1)
    group_channels["Z"] = action.fcurves.find(group_name, 2)

    frames = self.__calculateKeyframesForCurves(action, group_name, group_channels)
    if frames == None:
      return None
    #endif

    frames_count = len(frames)
    self.__logger.debug("action[%s]: scale frame count: %d" % (action.name, frames_count))
    if frames_count > 0:
      assert bone_name in armature.pose.bones, "No bone %s in armature" % bone_name
      bone = armature.pose.bones[bone_name]
      assert type(bone) == bpy_types.PoseBone

      #
      # Evaluate the final state of the bone at each of the keyframes.
      #

      evaluated_keyframes = {}
      for index in sorted(frames.keys()):
        assert type(index) == int
        frame = frames[index]
        assert type(frame) == CalciumKeyframeUnevaluated

        bpy.context.scene.frame_set(index)

        value = self.__transformScaleToExport(bone.matrix_basis.to_scale())
        keyframe = CalciumKeyframe(index, frame.interpolation, frame.easing, 'scale', value)
        evaluated_keyframes[index] = keyframe
      #end

      self.__logger.debug("action[%s]: returning %d evaluated frames" % (action.name, len(evaluated_keyframes)))
      assert len(evaluated_keyframes) == frames_count
      return CalciumCurve(bone_name, 'scale', evaluated_keyframes)
    #endif

    self.__logger.debug("action[%s]: no frames, returning None" % action.name)
    return None
  #end

  #
  # Aggregate all of the orientation curves for the given bone in the given action.
  #

  def __makeBoneCurvesOrientation(self, armature, action, bone_name):
    assert type(armature) == bpy_types.Object
    assert type(action) == bpy.types.Action
    assert armature.type == 'ARMATURE'
    assert type(bone_name) == str

    group_name = 'pose.bones["%s"].rotation_quaternion' % bone_name
    group_channels      = {}
    group_channels["W"] = action.fcurves.find(group_name, 0)
    group_channels["X"] = action.fcurves.find(group_name, 1)
    group_channels["Y"] = action.fcurves.find(group_name, 2)
    group_channels["Z"] = action.fcurves.find(group_name, 3)

    frames = self.__calculateKeyframesForCurves(action, group_name, group_channels)
    if frames == None:
      return None
    #endif

    frames_count = len(frames)
    self.__logger.debug("action[%s]: orientation frame count: %d" % (action.name, frames_count))
    if frames_count > 0:
      assert bone_name in armature.pose.bones, "No bone %s in armature" % bone_name
      bone = armature.pose.bones[bone_name]
      assert type(bone) == bpy_types.PoseBone

      #
      # Evaluate the final state of the bone at each of the keyframes.
      #

      evaluated_keyframes = {}
      for index in sorted(frames.keys()):
        assert type(index) == int
        frame = frames[index]
        assert type(frame) == CalciumKeyframeUnevaluated

        bpy.context.scene.frame_set(index)

        value = self.__transformOrientationToExport(bone.matrix_basis.to_quaternion())
        keyframe = CalciumKeyframe(index, frame.interpolation, frame.easing, 'orientation', value)
        evaluated_keyframes[index] = keyframe
      #end

      self.__logger.debug("action[%s]: returning %d evaluated frames" % (action.name, len(evaluated_keyframes)))
      assert len(evaluated_keyframes) == frames_count
      return CalciumCurve(bone_name, 'orientation', evaluated_keyframes)
    #endif

    self.__logger.debug("action[%s]: no frames, returning None" % action.name)
    return None
  #end

  #
  # Transform all of the given actions to Calcium actions.
  #

  def __makeActions(self, armature, actions):
    assert type(actions) == bpy.types.bpy_prop_collection
    assert type(armature) == bpy_types.Object
    assert armature.type == 'ARMATURE'
    assert len(actions) > 0, "Must have at least one action"

    calcium_actions = []

    try:
      if armature.animation_data is not None:
        self.__logger.debug("__makeActions: saving action %s" % armature.animation_data.action)
        saved_action = armature.animation_data.action
      else:
        self.__logger.debug("__makeActions: creating temporary animation data")
        armature.animation_data_create()
      #endif

      for action in actions:
        if action.name == 'poses':
          continue
        #endif

        self.__logger.debug("__makeActions: %s" % action.name)
        armature.animation_data.action = action

        curves = []
        for bone_name in armature.pose.bones.keys():
          curve_trans = self.__makeBoneCurvesTranslation(armature, action, bone_name)
          if curve_trans != None:
            assert type(curve_trans) == CalciumCurve
            curves.append(curve_trans)
          #endif

          curve_scale = self.__makeBoneCurvesScale(armature, action, bone_name)
          if curve_scale != None:
            assert type(curve_scale) == CalciumCurve
            curves.append(curve_scale)
          #endif

          curve_orient = self.__makeBoneCurvesOrientation(armature, action, bone_name)
          if curve_orient != None:
            assert type(curve_orient) == CalciumCurve
            curves.append(curve_orient)
          #endif
        #end

        calcium_actions.append(CalciumAction('curves', action.name, bpy.context.scene.render.fps, curves))
      #end

      self.__logger.debug("__makeActions: returning %d actions" % len(calcium_actions))
      return calcium_actions
    finally:
      if saved_action:
        self.__logger.debug("__makeActions: restoring saved action %s" % saved_action)
        armature.animation_data.action = saved_action
      else:
        self.__logger.debug("__makeActions: clearing temporary animation data")
        armature.animation_data_clear()
      #endif
    #endtry
  #end

  #
  # Construct a list of bones for the given armature.
  #

  def __makeBones(self, armature):
    assert type(armature) == bpy_types.Object
    assert armature.type == 'ARMATURE'
    self.__logger.debug("__makeBones: %s" % armature.name)

    calcium_bones = []
    for pose_bone in armature.pose.bones:
      assert type(pose_bone) == bpy_types.PoseBone
      bone = pose_bone.bone
      assert type(bone) == bpy_types.Bone

      #
      # The matrix_local field of each bone is relative to the origin
      # of the armature. To retrieve a parent-relative matrix, it's
      # necessary to multiply the bone's matrix by the inverse of its
      # parent matrix.
      #

      if bone.parent:
        mat = bone.matrix_local * bone.parent.matrix_local.inverted()
      else:
        mat = bone.matrix_local
      #endif

      bone_trans  = self.__transformTranslationToExport(mat.to_translation())
      bone_scale  = self.__transformScaleToExport(mat.to_scale())
      bone_orient = self.__transformOrientationToExport(bone.matrix.to_quaternion())

      bone_parent = None
      if bone.parent != None:
        bone_parent = bone.parent.name
      #endif

      calcium_bones.append(CalciumBone(bone.name, bone_parent, bone_trans, bone_orient, bone_scale))
    #end

    return calcium_bones
  #end

  #
  # Construct a Calcium skeleton from the given armature.
  #

  def __makeSkeleton(self, armature):
    assert type(armature) == bpy_types.Object
    assert armature.type == 'ARMATURE'

    actions = []
    if len(bpy.data.actions) > 0:
      frame_saved = bpy.context.scene.frame_current
      try:
        actions = self.__makeActions(armature, bpy.data.actions)
        assert type(actions) == list
      finally:
        bpy.context.scene.frame_set(frame_saved)
      #endtry
    #endif

    bones = self.__makeBones(armature)
    assert type(bones) == list

    return CalciumSkeleton(armature.name, bones, actions)
  #end

  def __writeSkeleton(self, out_file, skeleton):
    assert type(out_file) == io.TextIOWrapper
    assert type(skeleton) == CalciumSkeleton

    out_file.write("{\n")
    out_file.write("  \"version\": \"calcium skeleton 1.0\",\n")
    out_file.write("  \"skeleton\": ")
    out_file.write(json.dumps(skeleton.toJSON(), sort_keys=False, indent=2))
    out_file.write("\n")
    out_file.write("}\n")
  #end

  def __writeFile(self, out_file, armature):
    assert type(out_file) == io.TextIOWrapper
    assert type(armature) == bpy_types.Object
    assert armature.type == 'ARMATURE'

    skeleton = self.__makeSkeleton(armature)
    assert type(skeleton) == CalciumSkeleton
    self.__writeSkeleton(out_file, skeleton)
  #end

  #
  # The main entry point. Serialize the selected armature object to a temporary
  # file. Maintain a log file, fail if any error messages have been logged,
  # and then atomically rename the temporary file to the given output path
  # if no errors have occurred.
  #

  def write(self, path):
    assert type(path) == str
    error_path = path + ".log"
    tmp_path = path + ".tmp"

    with open(error_path, "wt") as error_file:
      self.__logger = CalciumLogger(self.__logger_severity, error_file)
      t = datetime.datetime.now()
      self.__logger.info("Export started at %s" % t.isoformat())
      self.__logger.info("File: %s" % path)
      self.__logger.info("Log:  %s" % error_path)

      armature = False
      if len(bpy.context.selected_objects) > 0:
        for obj in bpy.context.selected_objects:
          if obj.type == 'ARMATURE':
            if armature:
              message = "Too many armatures selected: At most one of the selected objects can be an armature when exporting"
              self.__logger.error(message)
              raise CalciumTooManyArmaturesSelected(message)
            #endif
            armature = obj
          #endif
        #endfor
      #endif

      if False == armature:
        message = "No armatures selected: An armature object must be selected for export"
        self.__logger.error(message)
        raise CalciumNoArmatureSelected(message)
      #endif

      assert type(armature) == bpy_types.Object
      assert armature.type == 'ARMATURE'

      self.__logger.debug("write: opening: %s" % tmp_path)
      with open(tmp_path, "wt") as out_file:
        self.__writeFile(out_file, armature)

        errors = self.__logger.counts[CALCIUM_LOG_MESSAGE_ERROR]
        if errors > 0:
          self.__logger.error("Export failed with %d errors." % errors)
          raise CalciumExportFailed("Exporting failed due to errors.\nSee the log file at: %s" % error_path)
        else:
          self.__logger.info("Exported successfully")
          os.rename(tmp_path, path)
        #endif
      #endwith
    #endwith
  #end

#endclass
