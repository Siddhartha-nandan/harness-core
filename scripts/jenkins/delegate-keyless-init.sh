#!/bin/bash
# Copyright 2023 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

curl -s -L -o /etc/yum.repos.d/google-cloud-sdk.repo https://harness.jfrog.io/artifactory/BuildsTools/yum-repos/google-cloud-sdk.repo
microdnf install -y yum
yum update -y
yum install -y python3
yum install -y python3-pip
yum install -y python3-requests
yum install -y google-cloud-cli --nodocs --skip-broken
yum install -y mongodb-enterprise-4.2.18 mongodb-enterprise-server-4.2.18 mongodb-enterprise-mongos-4.2.18 mongodb-enterprise-tools-4.2.18 --nodocs --skip-broken
DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC yum install -y tzdata wget sudo openssl jq gnupg unzip --nodocs --skip-broken
