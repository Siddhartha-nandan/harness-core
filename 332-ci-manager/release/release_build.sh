#!/usr/bin/env bash
# Copyright 2022 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -ex

export PLATFORM="jenkins"

export VERSION_FILE=332-ci-manager/build.properties
export BUILD=`cat ${VERSION_FILE} | grep 'build.number=' | sed -e 's: *build.number=::g'`
export PATCH=`cat ${VERSION_FILE} | grep 'build.patch=' | sed -e 's: *build.patch=::g'`
export VERSION=$BUILD-$PATCH

chmod +x 332-ci-manager/build/feature_build.sh
332-ci-manager/build/feature_build.sh
