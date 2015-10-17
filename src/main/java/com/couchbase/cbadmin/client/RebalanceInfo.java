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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Object that contains information about a rebalance operation
 */
public class RebalanceInfo {
  final private Map<String,Number> details = new HashMap<String, Number>();
  private boolean completed = false;
  private boolean stopped = false;

  /**
   * Check if the rebalance is done
   * @return true if completed, false if in progress
   */
  public boolean isComplete() {
    return completed || stopped;
  }

  /**
   * Check if the rebalance was stopped (but the cluster is not balanced)
   * @return true if stopped
   */
  public boolean isStopped() {
    return stopped;
  }

  /**
   * Gets a percentage (between 0.0 and 100.0) indicating rebalance status.
   * @return
   */
  public float getProgress() {
    if (completed) {
      return 100;
    }

    float total = 0;
    for (Number f : details.values()) {
      total += f.floatValue();
    }
    return total / details.size();
  }

  public Map<String,Number> getDetails() {
    return Collections.unmodifiableMap(details);
  }

  public RebalanceInfo(JsonObject obj) throws RestApiException {
    JsonElement e = obj.get("status");
    if (e == null || e.isJsonPrimitive() == false) {
      throw new RestApiException("Expected status string", obj);
    }
    String sStatus = e.getAsString();
    if (sStatus.equals("none")) {
      completed = true;

    } else if (sStatus.equals("running")) {
      for (Entry<String,JsonElement> ent : obj.entrySet()) {
        if (ent.getKey().equals("status")) {
          continue;
        }
        JsonObject progressObj;
        if (ent.getValue().isJsonObject() == false) {
          throw new RestApiException("Expected object", ent.getValue());
        }
        progressObj = ent.getValue().getAsJsonObject();
        JsonElement progressNum = progressObj.get("progress");
        if (progressNum.isJsonPrimitive() == false ||
                progressNum.getAsJsonPrimitive().isNumber() == false) {
          throw new RestApiException("Expected 'progress' to be number",
                  progressNum);
        }
        details.put(ent.getKey(), progressNum.getAsNumber().floatValue() * 100);
      }
    }
  }
}