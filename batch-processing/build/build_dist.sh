#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -ex

# This script requires JDK, VERSION and PURPOSE as environment variables

mkdir -p dist
cd dist

cd ..

mkdir -p dist/batch-processing
cd dist/batch-processing

curl https://storage.googleapis.com/harness-prod-public/public/shared/tools/alpn/release/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar  --output alpn-boot-8.1.13.v20181017.jar

echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cp ${HOME}/.bazel-dirs/bin/batch-processing/service/module_deploy.jar batchprocessing-service-capsule.jar
cp ../../batch-processing/config/batch-processing-config.yml .
cp ../../access-control/build/container/Dockerfile-batch-processing-jenkins-k8-openjdk ./Dockerfile
cp ../../access-control/build/container/Dockerfile-batch-processing-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../batch-processing/build/container/scripts/ .
java -jar batchprocessing-service-capsule.jar scan-classpath-metadata

cd ../..
