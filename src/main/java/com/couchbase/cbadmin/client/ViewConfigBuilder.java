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

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mnunberg
 */
public class ViewConfigBuilder implements ViewConfig {
  private final JsonObject jObj = new JsonObject();
  private final String dName;
  private final String bucket;
  private final List<String> viewNames = new ArrayList<String>();
  private String bucketPassword;

  public ViewConfigBuilder(String design, String bkt) {
    dName = design;
    bucket = bkt;

    jObj.addProperty("_id", "_design/" + design);
    jObj.addProperty("language", "javascript");
    jObj.add("views", new JsonObject());
  }

  public ViewConfigBuilder view(String name, String map, String reduce) {
    JsonObject def = new JsonObject();
    def.addProperty("map", map);
    if (reduce != null) {
      def.addProperty("reduce", reduce);
    }
    jObj.get("views").getAsJsonObject().add(name, def);
    viewNames.add(name);
    return this;
  }

  public ViewConfigBuilder view(String name, String map) {
    view(name, map, null);
    return this;
  }

  public ViewConfigBuilder password(String pass) {
    bucketPassword = pass;
    return this;
  }

  public ViewConfig build() {
    return this;
  }

  @Override
  public String getBucketName() {
    return bucket;
  }

  @Override
  public String getDesign() {
    return dName;
  }

  @Override
  public String getDefinition() {
    return jObj.toString();
  }

  @Override
  public String getBucketPassword() {
    return bucketPassword;
  }

  @Override
  public List<String> getViewNames() {
    return viewNames;
  }
}
