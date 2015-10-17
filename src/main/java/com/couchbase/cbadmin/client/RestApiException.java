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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An exception thrown by the REST API
 * @author mnunberg
 */
public class RestApiException extends Exception {
  private final StatusLine status;
  private JsonElement json = null;
  private HttpRequestBase request = null;

  static private StatusLine defaultStatusLine(String s, int code) {
    if (s == null) {
      s = "Client Error";
    }

    return new BasicStatusLine(new ProtocolVersion("HTTP", 1, 0), code, s);
  }

  static private StatusLine defaultStatusLine() {
    return defaultStatusLine(null, 408);
  }

  // Constructors
  public RestApiException() {
    super();
    status = defaultStatusLine();
  }

  public RestApiException(String msg) {
    super(msg);
    status = defaultStatusLine(msg, 408);
  }


  public RestApiException(JsonElement js, StatusLine st) {
    status = st;
    json = js;
  }

  public RestApiException(JsonElement js, StatusLine st, HttpRequestBase req) {
    status = st;
    json = js;
    request = req;
  }

  public RestApiException(String s, JsonElement badJson) {
    super(s);
    status = defaultStatusLine();
    json = badJson;
  }

  public RestApiException(Throwable e) {
    super(e);
    status = defaultStatusLine();
  }

  public JsonElement getJson() {
    return json;
  }

  public StatusLine getStatusLine() {
    return status;
  }

  @Override
  public String getMessage() {
    List<String> msgList = new ArrayList<String>();
    String existing = super.getMessage();

    if (existing != null) {
      msgList.add(existing);
    }

    if (status.getStatusCode() != 408) {
      msgList.add(String.format("<Status=%d, Reason=%s>",
                  status.getStatusCode(), status.getReasonPhrase()));
    }

    if (json != null) {
      msgList.add("<JSON="+json+">");
    }

    if (request != null) {
      msgList.add("Request="+request+">");

      if (request instanceof HttpEntityEnclosingRequest) {
        HttpEntityEnclosingRequest eReq = (HttpEntityEnclosingRequest)request;
        HttpEntity entity = eReq.getEntity();
        try {
          if (entity != null && entity.getContent() != null) {
            InputStream strm = entity.getContent();
            strm.reset();
            msgList.add("<Request Body="+IOUtils.toString(strm)+">");
          }
        } catch (IOException ex) {
          // In exception. What do we do here?
          msgList.add("<IOException for reading request>");
        } catch (IllegalStateException ex) {
          msgList.add("<IllegalStateException while reading request>");
        }
      }
    }

    return StringUtils.join(msgList, ",");
  }
}