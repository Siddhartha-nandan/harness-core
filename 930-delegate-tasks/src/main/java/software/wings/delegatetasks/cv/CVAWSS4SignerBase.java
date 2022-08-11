/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.cv;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class has been borrowed from AWS Java Samples.
 */

public class CVAWSS4SignerBase {
  public static final String EMPTY_BODY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
  public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

  public static final String SCHEME = "AWS4";
  public static final String ALGORITHM = "HMAC-SHA256";
  public static final String TERMINATOR = "aws4_request";

  /** format strings for the date/time and date stamps required during signing **/
  public static final String ISO8601BasicFormat = "yyyyMMdd'T'HHmmss'Z'";
  public static final String DateStringFormat = "yyyyMMdd";

  protected URL endpointUrl;
  protected String httpMethod;
  protected String serviceName;
  protected String regionName;

  protected final SimpleDateFormat dateTimeFormat;
  protected final SimpleDateFormat dateStampFormat;

  /**
   * Create a new AWS V4 signer.
   *
   * @param endpointUrl
   *            The service endpoint, including the path to any resource.
   * @param httpMethod
   *            The HTTP verb for the request, e.g. GET.
   * @param serviceName
   *            The signing name of the service, e.g. 's3'.
   * @param regionName
   *            The system name of the AWS region associated with the
   *            endpoint, e.g. us-east-1.
   */
  public CVAWSS4SignerBase(URL endpointUrl, String httpMethod, String serviceName, String regionName) {
    this.endpointUrl = endpointUrl;
    this.httpMethod = httpMethod;
    this.serviceName = serviceName;
    this.regionName = regionName;

    dateTimeFormat = new SimpleDateFormat(ISO8601BasicFormat);
    dateTimeFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
    dateStampFormat = new SimpleDateFormat(DateStringFormat);
    dateStampFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
  }

  /**
   * Returns the canonical collection of header names that will be included in
   * the signature. For AWS4, all header names must be included in the process
   * in sorted canonicalized order.
   */
  protected static String getCanonicalizeHeaderNames(Map<String, String> headers) {
    List<String> sortedHeaders = new ArrayList<>();
    sortedHeaders.addAll(headers.keySet());
    Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

    StringBuilder buffer = new StringBuilder();
    for (String header : sortedHeaders) {
      if (buffer.length() > 0) {
        buffer.append(';');
      }
      buffer.append(header.toLowerCase());
    }

    return buffer.toString();
  }

  /**
   * Computes the canonical headers with values for the request. For AWS4, all
   * headers must be included in the signing process.
   */
  protected static String getCanonicalizedHeaderString(Map<String, String> headers) {
    if (isEmpty(headers)) {
      return "";
    }

    // step1: sort the headers by case-insensitive order
    List<String> sortedHeaders = new ArrayList<>();
    sortedHeaders.addAll(headers.keySet());
    Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

    // step2: form the canonical header:value entries in sorted order.
    // Multiple white spaces in the values should be compressed to a single
    // space.
    StringBuilder buffer = new StringBuilder();
    for (String key : sortedHeaders) {
      buffer.append(key.toLowerCase().replaceAll("\\s+", " "));
      buffer.append(':');
      buffer.append(headers.get(key).replaceAll("\\s+", " "));
      buffer.append('\n');
    }

    return buffer.toString();
  }

  /**
   * Returns the canonical request string to go into the signer process; this
   consists of several canonical sub-parts.
   * @return
   */
  protected static String getCanonicalRequest(URL endpoint, String httpMethod, String queryParameters,
      String canonicalizedHeaderNames, String canonicalizedHeaders, String bodyHash) {
    return httpMethod + "\n" + getCanonicalizedResourcePath(endpoint) + "\n" + queryParameters + "\n"
        + canonicalizedHeaders + "\n" + canonicalizedHeaderNames + "\n" + bodyHash;
  }

  /**
   * Returns the canonicalized resource path for the service endpoint.
   */
  protected static String getCanonicalizedResourcePath(URL endpoint) {
    if (endpoint == null) {
      return "/";
    }
    String path = endpoint.getPath();
    if (isEmpty(path)) {
      return "/";
    }

    String encodedPath = urlEncode(path, true);
    if (encodedPath.charAt(0) == '/') {
      return encodedPath;
    } else {
      return "/".concat(encodedPath);
    }
  }

  /**
   * Examines the specified query string parameters and returns a
   * canonicalized form.
   * <p>
   * The canonicalized query string is formed by first sorting all the query
   * string parameters, then URI encoding both the key and value and then
   * joining them, in order, separating key value pairs with an '&'.
   *
   * @param parameters
   *            The query string parameters to be canonicalized.
   *
   * @return A canonicalized form for the specified query string parameters.
   */
  public static String getCanonicalizedQueryString(Map<String, String> parameters) {
    if (isEmpty(parameters)) {
      return "";
    }

    SortedMap<String, String> sorted = new TreeMap<>();

    Iterator<Map.Entry<String, String>> pairs = parameters.entrySet().iterator();
    while (pairs.hasNext()) {
      Map.Entry<String, String> pair = pairs.next();
      String key = pair.getKey();
      String value = pair.getValue();
      sorted.put(urlEncode(key, false), urlEncode(value, false));
    }

    StringBuilder builder = new StringBuilder();
    pairs = sorted.entrySet().iterator();
    while (pairs.hasNext()) {
      Map.Entry<String, String> pair = pairs.next();
      builder.append(pair.getKey());
      builder.append('=');
      builder.append(pair.getValue());
      if (pairs.hasNext()) {
        builder.append('&');
      }
    }

    return builder.toString();
  }

  protected static String getStringToSign(
      String scheme, String algorithm, String dateTime, String scope, String canonicalRequest) {
    return scheme + "-" + algorithm + "\n" + dateTime + "\n" + scope + "\n" + toHex(hash(canonicalRequest));
  }

  /**
   * Hashes the string contents (assumed to be UTF-8) using the SHA-256
   * algorithm.
   */
  public static byte[] hash(String text) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(text.getBytes("UTF-8"));
      return md.digest();
    } catch (Exception e) {
      throw new RuntimeException("Unable to compute hash while signing request: " + e.getMessage(), e);
    }
  }

  /**
   * Hashes the byte array using the SHA-256 algorithm.
   */
  public static byte[] hash(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(data);
      return md.digest();
    } catch (Exception e) {
      throw new RuntimeException("Unable to compute hash while signing request: " + e.getMessage(), e);
    }
  }

  protected static byte[] sign(String stringData, byte[] key, String algorithm) {
    try {
      byte[] data = stringData.getBytes("UTF-8");
      Mac mac = Mac.getInstance(algorithm);
      mac.init(new SecretKeySpec(key, algorithm));
      return mac.doFinal(data);
    } catch (Exception e) {
      throw new RuntimeException("Unable to calculate a request signature: " + e.getMessage(), e);
    }
  }

  public static String urlEncode(String url, boolean keepPathSlash) {
    String encoded;
    try {
      encoded = URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 encoding is not supported.", e);
    }
    if (keepPathSlash) {
      encoded = encoded.replace("%2F", "/");
    }
    return encoded;
  }

  public static String toHex(byte[] data) {
    StringBuilder sb = new StringBuilder(data.length * 2);
    for (int i = 0; i < data.length; i++) {
      String hex = Integer.toHexString(data[i]);
      if (hex.length() == 1) {
        // Append leading zero.
        sb.append('0');
      } else if (hex.length() == 8) {
        // Remove ff prefix from negative numbers.
        hex = hex.substring(6);
      }
      sb.append(hex);
    }
    return sb.toString().toLowerCase(Locale.getDefault());
  }
}
