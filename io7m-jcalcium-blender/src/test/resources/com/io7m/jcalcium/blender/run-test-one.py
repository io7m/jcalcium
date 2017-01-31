import bpy
import sys
import logging

logging.basicConfig(level=logging.DEBUG)

argv     = sys.argv
expected = "failure"

try:
  argv = argv[argv.index("--") + 1:]

  if len(argv) != 2:
    raise ValueError
  #endif

  expected = argv[0]
  logging.debug("expecting result: " + expected)
  if expected != "failure" and expected != "success":
    raise ValueError
  #endif

  output_path = argv[1]
  logging.debug("writing output to: " + output_path)

except ValueError as ex:
  logging.error(ex)
  logging.error("expected: ('failure' | 'success') output-path")
  sys.exit(2)
#endtry

bpy.ops.object.select_pattern(pattern="Armature")
if len(bpy.context.selected_objects) != 1:
  logging.error("Must have exactly one armature selected")
  sys.exit(2)
#endif

failed = False
try:
  bpy.ops.export_scene.csj(filepath=output_path)
  failed = False
except RuntimeError as ex:
  print(ex)
  failed = True
#endtry

if expected == "failure":
  if failed:
    logging.info("Test correctly failed")
    sys.exit(0)
  else:
    logging.info("Test unexpectedly succeeded!")
    sys.exit(1)
  #endif
elif expected == "success":
  if not failed:
    logging.info("Test correctly succeeded")
    sys.exit(0)
  else:
    logging.info("Test unexpectedly failed!")
    sys.exit(1)
  #endif
#endif

# Unreachable code!
logging.error("Unreachable code!")
sys.exit(2)
