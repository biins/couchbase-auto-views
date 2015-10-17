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

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * On hosts with more than a single IP, the cluster will sometimes use a
 * different IP. While the cluster doesn't have problems communicating with
 * the nodes, user-facing IPs may indeed do so.
 */
public class AliasLookup {
  private final Map<String,Set<String>> dict = new HashMap<String, Set<String>>();
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

  public Collection<String> getForAlias(String alias) {
    Set<String> s = null;
    rwLock.readLock().lock();

    try {
      s = dict.get(alias);
    } finally {
      rwLock.readLock().unlock();
    }

    if (s == null) {
      s = new HashSet<String>();
      s.add(alias);
    }
    return s;
  }

  public void associateAlias(Collection<String> aliases) {
    rwLock.writeLock().lock();
    try {
      for (String alias : aliases) {
        Set<String> s = dict.get(alias);
        if (s == null) {
          s = new HashSet<String>();
          dict.put(alias, s);
        }

        s.add(alias);
        for (String other : aliases) {
          s.add(other);
        }
      }
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  public void associateAlias(String a1, String a2) {
    List<String> s = new ArrayList<String>(2);
    s.add(a1);
    s.add(a2);
    associateAlias(s);
  }

  public void merge(AliasLookup other) {
    other.rwLock.readLock().lock();
    try {
      for (Set<String> s : other.dict.values()) {
        associateAlias(s);
      }
    } finally {
      other.rwLock.readLock().unlock();
    }
  }
}