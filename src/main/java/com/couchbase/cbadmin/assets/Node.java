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

package com.couchbase.cbadmin.assets;

import com.couchbase.cbadmin.client.CouchbaseAdminImpl;
import com.couchbase.cbadmin.client.RestApiException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author mnunberg
 */
public class Node implements ClusterAsset {
  public enum Membership { ACTIVE, INACTIVE_ADDED, INACTIVE_FAILED, UNKNOWN }
  public enum Status { HEALTHY, UNHEALTHY, WARMUP, UNKNOWN }

  private CouchbaseAdminImpl selfClient;
  private URL restUrl;
  private URL couchUrl;
  private JsonObject rawJson;
  private String versionString;
  private String NSOtpNode;
  private int clusterCompatVersion = 0;
  private Membership membership = Membership.UNKNOWN;
  private Status status = Status.UNKNOWN;

  public URL getRestUrl() {
    return restUrl;
  }

  public URL getCouchUrl() {
    return couchUrl;
  }

  public String getNSOtpNode() {
    return NSOtpNode;
  }

  public String getClusterVersion() {
    return versionString;
  }

  public int getClusterCompatMajorVersion() {
    if (clusterCompatVersion == 1) {
      return Cluster.COMPAT_1x;
    } else {
      return Cluster.COMPAT_20;
    }
  }

  public int getClusterCompatVersion() {
    return clusterCompatVersion;
  }

  public Membership getMembership() {
    return membership;
  }

  public Status getStatus() {
    return status;
  }

  /**
   * Checks to see that the node is functioning OK by examining some of its
   * status variables.
   * @return
   */
  public boolean isOk() {
    if (membership != Membership.ACTIVE) {
      return false;
    }
    if (status != Status.HEALTHY) {
      return false;
    }
    return true;
  }

  @Override
  public JsonObject getRawJson() {
    return rawJson;
  }

  public Node(JsonObject def) throws RestApiException {
    JsonElement eTmp;
    String sTmp;
    eTmp = def.get("hostname");
    if (eTmp == null) {
      throw new RestApiException("Expected 'hostname'", def);
    }
    sTmp = eTmp.getAsString();

    try {
      restUrl = new URL("http://" + sTmp + "/");
    } catch (MalformedURLException ex) {
      throw new RuntimeException(ex);
    }

    eTmp = def.get("couchApiBase");
    if (eTmp != null) {
      try {
        sTmp = eTmp.getAsString();
        couchUrl = new URL(sTmp);
      } catch (MalformedURLException ex) {
        throw new RuntimeException(ex);
      }
    }

    eTmp = def.get("version");
    if (eTmp == null) {
      throw new RestApiException("Expected 'version' in nodes JSON", def);
    }
    versionString = eTmp.getAsString();

    eTmp = def.get("otpNode");
    if (eTmp == null) {
      throw new RestApiException("Expected 'otpNode'", def);
    }
    NSOtpNode = eTmp.getAsString();

    eTmp = def.get("clusterCompatibility");
    if (eTmp != null) {
      clusterCompatVersion = eTmp.getAsInt();
    }

    eTmp = def.get("clusterMembership");
    if (eTmp != null) {
      sTmp = eTmp.getAsString();

      if (sTmp.equals("active")) {
        membership = Membership.ACTIVE;
      } else if (sTmp.equals("inactiveAdded")) {
        membership = Membership.INACTIVE_ADDED;

      } else if (sTmp.equals("inactiveFailed")) {
        membership = Membership.INACTIVE_FAILED;
      }
    }

    eTmp = def.get("status");
    if (eTmp != null) {
      sTmp = eTmp.getAsString();
      if (sTmp.equals("healthy")) {
        status = Status.HEALTHY;
      } else if (sTmp.equals("unhealthy")) {
        status = Status.UNHEALTHY;
      } else if (sTmp.equals("warmup")) {
        status = Status.WARMUP;
      }
    }
  }

  public CouchbaseAdminImpl getOwnClient(CouchbaseAdminImpl template) {
    if (selfClient == null) {
      selfClient = template.copyForHost(restUrl);
    }

    return selfClient;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
            .append(NSOtpNode)
            .append(restUrl)
            .toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (! (other instanceof Node)) {
      return false;
    }

    Node nOther = (Node) other;
    return new EqualsBuilder()
            .append(NSOtpNode, nOther.NSOtpNode)
            .append(restUrl, nOther.restUrl)
            .isEquals();
  }

  @Override
  public String toString() {
    return "<URI:"+restUrl.getAuthority()+","+NSOtpNode+">";
  }
}
