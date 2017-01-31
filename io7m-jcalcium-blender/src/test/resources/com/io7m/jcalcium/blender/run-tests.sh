#!/bin/sh

OLD_IFS="${IFS}"
IFS="
"

TEST_BLENDER=`which blender` || exit 1
TEST_TMPDIR=`mktemp -d --tmpdir calcium-blender-XXXXXXXX` || exit 1
TEST_COUNT=0
TEST_FAILURES=0

printf "Running test suite on %s\n" `date +%Y-%m-%dT%H:%M:%S%z || exit 1` || exit 1
printf "Logging to ${TEST_TMPDIR}\n" || exit 1
printf "blender: %s\n" "${TEST_BLENDER}"
echo "--------------------------------------------------------------------------------"

for line in `cat run-test-suite.txt || exit 1`
do
  TEST_COUNT=`expr ${TEST_COUNT} + 1`
  TEST_BLEND=`echo "${line}" | awk '{print $1}'` || exit 1
  TEST_EXPECT=`echo "${line}" | awk '{print $2}'` || exit 1

  TEST_OUTPUT="${TEST_TMPDIR}/${TEST_BLEND}.output"
  TEST_RESULT=""

  "${TEST_BLENDER}" "${TEST_BLEND}" --background --addons calcium --python run-test-one.py -- "${TEST_EXPECT}" "${TEST_TMPDIR}/${TEST_BLEND}" 1>"${TEST_OUTPUT}" 2>&1
  case $? in
    0)
      TEST_RESULT="success"
      ;;
    1)
      TEST_RESULT="failure"
      TEST_FAILURES=`expr ${TEST_FAILURES} + 1`
      ;;
    *)
      TEST_RESULT="error"
      TEST_FAILURES=`expr ${TEST_FAILURES} + 1`
      ;;
  esac

  printf "%-50s (expect: %8s) | %-8s\n" "${TEST_BLEND}" "${TEST_EXPECT}" "${TEST_RESULT}"
done

echo "--------------------------------------------------------------------------------"

case ${TEST_FAILURES} in
  0)
    printf "PASS: %d of %d tests passed\n" "${TEST_COUNT}" "${TEST_COUNT}"
    exit 0
    ;;

  *)
    printf "FAIL: %d of %d tests failed\n" "${TEST_FAILURES}" "${TEST_COUNT}"
    exit 1
    ;;
esac

