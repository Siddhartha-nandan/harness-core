#!/bin/sh
# Copyright 2022 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

#Build your java service and then pass in the path to that jar
if [[ $# = 0 ]] ; then printf "Error - You did not pass in the path the jar you want nativized!\n"; exit 1; else JAR_PATH="${1}"; fi

native-image --no-fallback \
-H:TraceClassInitialization=true \
-H:ConfigurationFileDirectories=target/config \
-H:+ReportExceptionStackTraces \
-H:+PrintClassInitialization \
--initialize-at-build-time=org.bouncycastle.jce.provider.BouncyCastleProviderConfiguration \
--initialize-at-build-time='org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi$X448' \
--initialize-at-build-time=org.bouncycastle.jce.provider.BouncyCastleProvider \
--initialize-at-build-time=com.google.common.collect.RegularImmutableMap \
--initialize-at-build-time=org.slf4j.LoggerFactory \
--initialize-at-build-time=org.slf4j.MDC \
--initialize-at-build-time=org.slf4j.impl.StaticLoggerBinder \
--initialize-at-build-time=jdk.xml.internal.SecuritySupport \
--initialize-at-build-time=org.apache.sshd.common.util.GenericUtils \
--initialize-at-build-time=ch.qos.logback \
--initialize-at-build-time=org.apache.sshd.client.subsystem.sftp.SftpFileSystemProvider \
--initialize-at-build-time=javax.xml.parsers.FactoryFinder \
--initialize-at-run-time=io.netty \
-jar $JAR_PATH
