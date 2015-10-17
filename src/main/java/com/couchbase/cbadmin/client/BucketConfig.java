/*
 * Copyright (C) 2013 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package com.couchbase.cbadmin.client;

import com.couchbase.cbadmin.assets.Bucket;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mnunberg
 */
public class BucketConfig {
  public final String name;
  private String password;
  public Bucket.BucketType bucketType;

  private Bucket.AuthType authType = Bucket.AuthType.SASL;
  private int proxyPort = 0;
  public int ramQuotaMB;
  public int replicaCount = 0;
  public boolean shouldIndexReplicas = false;

  public BucketConfig(String bktname) {
    name = bktname;
  }

  /**
   * Indicate the auth type is SASL auth.
   */
  public void setSaslAuth() {
    proxyPort = 0;
    authType = Bucket.AuthType.SASL;
  }

  /**
   * Indicate SASL auth should not be used.
   * This also enforces a proxy port for raw text access
   * @param txtProxyPort The port for plain text access
   */
  public void setNoAuth(int txtProxyPort) {
    proxyPort = txtProxyPort;
    authType = Bucket.AuthType.NONE;
  }

  /**
   * Sets the password for this bucket. Also sets SASL auth
   * @param passwd The SASL password.
   */
  public void setSaslPassword(String passwd) {
    setSaslAuth();
    password = passwd;
  }

  public Map<String,String> makeParams() {
    Map<String,String> params = new HashMap<String,String>();
    params.put("name", name);
    params.put("ramQuotaMB", "" + ramQuotaMB);
    params.put("replicaIndex", shouldIndexReplicas ? "1" : "0");
    params.put("replicaNumber", "" + replicaCount);

    if (authType == Bucket.AuthType.SASL ||
            (password != null && password.isEmpty() == false)) {
      params.put("authType", "sasl");
      if (password == null ) {
        params.put("saslPassword", "");
      } else {
        params.put("saslPassword", password);
      }
    } else {
      params.put("authType", "none");
      params.put("proxyPort", "" + proxyPort);
    }

    if (bucketType == Bucket.BucketType.COUCHBASE) {
      params.put("bucketType", "membase");
    } else {
      params.put("bucketType", "memcached");
    }

    return params;
  }
}