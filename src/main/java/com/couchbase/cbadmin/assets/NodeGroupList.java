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

import com.couchbase.cbadmin.client.RestApiException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

public final class NodeGroupList {
  private final Collection<NodeGroup> groups;
  private final URI assignmentUri;

  public NodeGroupList(JsonObject json) throws RestApiException {
    JsonElement e = json.get("uri");
    if (e == null || e.isJsonPrimitive() == false) {
      throw new RestApiException("Expected modification URI", json);
    }
    assignmentUri = URI.create(e.getAsString());

    e = json.get("groups");
    if (e == null || e.isJsonArray() == false) {
      throw new RestApiException("Expected 'groups'", e);
    }

    groups = new ArrayList<NodeGroup>();
    for (JsonElement groupElem : e.getAsJsonArray()) {
      if (groupElem.isJsonObject() == false) {
        throw new RestApiException("Expected object for group", groupElem);
      }
      groups.add(new NodeGroup(groupElem.getAsJsonObject()));
    }
  }

  public URI getAssignmentUri() {
    return assignmentUri;
  }

  public Collection<NodeGroup> getGroups() {
    return groups;
  }

  public NodeGroup find(String name) {
    for (NodeGroup group : groups) {
      if (group.getName().equals(name)) {
        return group;
      }
    }
    return null;
  }

}
