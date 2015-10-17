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

import com.google.gson.JsonObject;

/**
 * Class representing a bucket as it exists within the cluster.
 */
public class Bucket {
  public enum BucketType { COUCHBASE, MEMCACHED }
  public enum AuthType {SASL, NONE }

  private final String name;
  private final JsonObject rawJson;
  private final int replicas;
  private final BucketType type;

  public Bucket(JsonObject def) {
    rawJson = def;
    name = rawJson.get("name").getAsString();
    replicas = rawJson.get("replicaNumber").getAsInt();
    String sType = rawJson.get("bucketType").getAsString();

    if (sType.equals("membase") || sType.equals("couchbase")) {
      type = BucketType.COUCHBASE;
    } else {
      type = BucketType.MEMCACHED;
    }
  }

  public String getName() {
    return name;
  }

  public int getReplicaCount() {
    return replicas;
  }

  public JsonObject getRawJson() {
    return rawJson;
  }

  public BucketType getType() {
    return type;
  }

  @Override
  public String toString() {
    return String.format("Bucket type=%s Name=%s Replicas=%d", type, name, replicas);
  }
}
