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

bl_info = {
  "name":        "Calcium JSON format",
  "author":      "io7m",
  "version":     (0, 1, 0),
  "blender":     (2, 66, 0),
  "location":    "File > Export > Calcium JSON (.caj)",
  "description": "Export armatures to Calcium format",
  "warning":     "",
  "wiki_url":    "",
  "tracker_url": "https://github.com/io7m/jcalcium/issues",
  "category":    "Import-Export"
}

import bpy
import bpy_extras.io_utils
import mathutils

CalciumOrientationHelper = bpy_extras.io_utils.orientation_helper_factory("CalciumOrientationHelper", axis_forward='-Z', axis_up='Y')

class ExportCalcium(bpy.types.Operator, bpy_extras.io_utils.ExportHelper, CalciumOrientationHelper):
  bl_idname = "export_scene.caj"
  bl_label = "Export Calcium"

  # The filename_ext field is accessed by ExportHelper.
  filename_ext = ".caj"

  filepath = bpy.props.StringProperty(subtype='FILE_PATH')
  verbose  = bpy.props.BoolProperty(name="Verbose logging",description="Enable verbose debug logging",default=True)

  def execute(self, context):
    self.filepath = bpy.path.ensure_ext(self.filepath, ".caj")

    args = {}
    args['verbose'] = self.verbose
    assert type(args['verbose']) == bool

    args['conversion_matrix'] = bpy_extras.io_utils.axis_conversion(to_forward=self.axis_forward, to_up=self.axis_up).to_4x4()
    assert type(args['conversion_matrix'] == mathutils.Matrix)

    from . import export
    e = export.CalciumExporter(args)

    try:
      e.write(self.filepath)
    except export.CalciumNoArmatureSelected as ex:
      self.report({'ERROR'}, ex.value)
    except export.CalciumTooManyArmaturesSelected as ex:
      self.report({'ERROR'}, ex.value)
    except export.CalciumExportFailed as ex:
      self.report({'ERROR'}, ex.value)
    #endtry

    return {'FINISHED'}
  #end

  def invoke(self, context, event):
    if not self.filepath:
      self.filepath = bpy.path.ensure_ext(bpy.data.filepath, ".caj")
    context.window_manager.fileselect_add(self)
    return {'RUNNING_MODAL'}
  #end
#endclass

def menuFunction(self, context):
  self.layout.operator(ExportCalcium.bl_idname, text="Calcium JSON (.caj)")
#end

def register():
  bpy.utils.register_class(ExportCalcium)
  bpy.types.INFO_MT_file_export.append(menuFunction)
#end

def unregister():
  bpy.utils.unregister_class(ExportCalcium)
  bpy.types.INFO_MT_file_export.remove(menuFunction)
#end

if __name__ == "__main__":
  register()
#endif
