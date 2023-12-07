/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.security;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class X509Utils {
  public static X509Certificate getX509Certificate(String cert) throws CertificateException {
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    byte[] certificateBytes = cert.getBytes();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(certificateBytes);
    return (X509Certificate) certificateFactory.generateCertificate(inputStream);
  }
}
