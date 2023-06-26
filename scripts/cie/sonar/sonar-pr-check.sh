#!/bin/bash
# Copyright 2022 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

#set -ex

#trap 'clean_up_files' EXIT

trap 'report_error' ERR

function clean_temp_files() {
  local_files=( $1 )
  for file in ${local_files[@]}
   do
      [ -f $file ] && rm -f $file
   done
}

function report_error(){
    echo 'please re-trigger this stage after resolving the issue.'
    echo 'to trigger this specific stage comment "trigger ss" in your github PR'
    exit 1
}

function usage() {
  echo "This scripts runs sonar scan to generate reports for vulnerabilities \
  on files in a PR."
  exit 0
}

function check_cmd_status() {
  if [ $1 != 0 ]; then
      echo "ERROR: $LINENO: $2. Exiting..."; exit 1
  fi
}

function create_empty_files() {
  for file in $1
   do
      [ -f $file ] && >$file || >$file
   done
}

function get_info_from_file(){
  local_filename=$1
  cat $local_filename | sort -u | tr '\r\n' ',' | rev | cut -c2- | rev
  check_cmd_status "$?" "Unable to find file to extract info for sonar file."
}

BAZEL_ARGS="--announce_rc --keep_going --show_timestamps --verbose_failures --remote_max_connections=1000 --remote_retries=1"
BAZEL_OUTPUT_PATH="/tmp/execroot/harness_monorepo/bazel-out/k8-fastbuild/bin"
COVERAGE_ARGS="--collect_code_coverage --combined_report=lcov --coverage_report_generator=//tools/bazel/sonarqube:sonarqube_coverage_generator \
--experimental_fetch_all_coverage_outputs --experimental_strict_java_deps=off --cache_test_results=yes --test_output=summary --build_runfile_links"
COVERAGE_REPORT_PATH='/tmp/execroot/harness_monorepo/bazel-out/_coverage/_coverage_report.dat'
JARS_ARRAY=("libmodule-hjar.jar" "libmodule.jar" "module.jar")
JARS_FILE="modules_jars.txt"
MODULES_FILE="modules.txt"
MODULES_TESTS_FILE="modules_tests.txt"
PR_SRCS_FILE="pr_srcs.txt"
SONAR_CONFIG_FILE='TEMP-sonar-project.properties'

# This script is required to generate the test util bzl file in root directory.
scripts/bazel/generate_credentials.sh

while getopts ":hb:c:k:" opt; do
  case $opt in
    h) usage; exit 5 ;;
    b) BASE_SHA=$OPTARG ;;
    c) COMMIT_SHA=$OPTARG ;;
    k) SONAR_KEY=$OPTARG ;;
    *) echo "Invalid Flag"; exit 5 ;;
  esac
done

HARNESS_CORE_MODULES=$(bazel query "//...:*" | grep -w "module" | awk -F/ '{print $3}' | sort -u | tr '\r\n' ' ')
check_cmd_status "$?" "Failed to list harness core modules."
#echo "HARNESS_CORE_MODULES: $HARNESS_CORE_MODULES"

GIT_DIFF=$(git diff --name-only $COMMIT_SHA..$BASE_SHA)

echo "------------------------------------------------"
echo -e "GIT DIFF:\n$GIT_DIFF"
echo "------------------------------------------------"

for FILE in $GIT_DIFF;
  do
    extension=${FILE##*.}
    if [ -f "${FILE}" ] && [ "${extension}" = "java" ]; then
      FILES+=("${FILE}")
      echo "$(echo ${FILE} | awk -F/ '{print $1}')" >> $MODULES_FILE
    else
      echo "${FILE} not found....."
    fi
  done

# Check if the file is empty, meaning there is no java file changed in the PR.
if ! grep -q -v '[^[:space:]]' "$MODULES_FILE"; then
  echo "INFO: No Java File change detected. Skipping the Scan....."
  exit 0
fi

PR_FILES=$(echo ${FILES[@]} | sort -u | tr ' ' ',')
check_cmd_status "$?" "Failed to get diff between commits."
echo -e "PR_FILES:\n${PR_FILES}"

PR_MODULES=$(cat $MODULES_FILE | sort -u | tr '\n' ' ')
check_cmd_status "$?" "Failed to get modules from commits."
echo -e "PR_MODULES:\n$PR_MODULES"

if [ "$(cat $MODULES_FILE | sort -u | wc -l)" -gt 1 ]; then
    echo "ERROR: PR is touching multiple modules, Generating Coverage and Code Smells is not possible. Exiting....."
    exit 1
fi

for module in $PR_MODULES
  do
     [ -d ${module} ] && [[ "${HARNESS_CORE_MODULES}" =~ "${module}" ]] \
     && BAZEL_COMPILE_MODULES+=("//${module}/...") \
     || echo "$module is not present in the bazel modules list."
  done

# Running Bazel Coverage
echo "INFO: BAZEL COMMAND: bazel coverage ${BAZEL_ARGS} ${COVERAGE_ARGS} -- ${BAZEL_COMPILE_MODULES[@]}"
bazel coverage ${BAZEL_ARGS} ${COVERAGE_ARGS} -- "${BAZEL_COMPILE_MODULES[@]}" || true
check_cmd_status "$?" "Failed to run coverage."

for file in $(echo $GIT_DIFF | tr '\r\n' ' ')
  do
     TEMP_RES=$(grep -w 'src' <<< $file | sed 's|src|:|' | awk -F: '{print $1}' | sed 's|$|src|')
     echo "$TEMP_RES" >> $PR_SRCS_FILE
  done
PR_SRCS_DIR=$(cat $PR_SRCS_FILE | sort -u)
echo -e "PR_SRCS_DIR:\n${PR_SRCS_DIR}"

for DIR in ${PR_SRCS_DIR}
  do
    find ${DIR} -type f -name "*Test.java" | tr '\r\n' ',' >> $MODULES_TESTS_FILE
  done
MODULES_TESTS=$(get_info_from_file $MODULES_TESTS_FILE)

for MODULE in $(cat $PR_SRCS_FILE | sort -u)
  do
    for JAR in ${JARS_ARRAY[@]}
      do
        if [ -f "${BAZEL_OUTPUT_PATH}/${MODULE}/../$JAR" ]; then
          echo "${BAZEL_OUTPUT_PATH}/${MODULE}/../$JAR" >> $JARS_FILE
        fi
      done
  done < $PR_SRCS_FILE
echo -e "JARS:\n$(cat ${JARS_FILE})"
JARS_BINS=$(get_info_from_file $JARS_FILE)

[ ! -f "${SONAR_CONFIG_FILE}" ] \
&& echo "sonar.projectKey=harness-core-sonar-pr" > ${SONAR_CONFIG_FILE} \
&& echo "sonar.log.level=DEBUG" >> ${SONAR_CONFIG_FILE}

echo "sonar.sources=$PR_FILES" >> ${SONAR_CONFIG_FILE}
echo "sonar.tests=$MODULES_TESTS" >> ${SONAR_CONFIG_FILE}
echo "sonar.inclusions=$PR_FILES" >> ${SONAR_CONFIG_FILE}
echo "sonar.test.inclusions=$MODULES_TESTS" >> ${SONAR_CONFIG_FILE}
echo "sonar.java.binaries=$JARS_BINS" >> ${SONAR_CONFIG_FILE}
echo "sonar.java.libraries=$JARS_BINS" >> ${SONAR_CONFIG_FILE}
echo "sonar.coverageReportPaths=$COVERAGE_REPORT_PATH" >> ${SONAR_CONFIG_FILE}
echo "sonar.pullrequest.key=$PR_NUMBER" >> ${SONAR_CONFIG_FILE}
echo "sonar.pullrequest.branch=$PR_BRANCH" >> ${SONAR_CONFIG_FILE}
echo "sonar.pullrequest.base=$BASE_BRANCH" >> ${SONAR_CONFIG_FILE}
echo "sonar.pullrequest.github.repository=$REPO_NAME" >> ${SONAR_CONFIG_FILE}

echo "INFO: Sonar Properties"
cat ${SONAR_CONFIG_FILE}

echo "INFO: Running Sonar Scan."
[ -f "${SONAR_CONFIG_FILE}" ] \
&& sonar-scanner -Dsonar.login=${SONAR_KEY} -Dsonar.host.url=https://sonar.harness.io \
|| echo "ERROR: Sonar Properties File not found."
