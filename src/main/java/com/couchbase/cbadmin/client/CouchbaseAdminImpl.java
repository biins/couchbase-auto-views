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
import com.couchbase.cbadmin.assets.Node;
import com.couchbase.cbadmin.assets.NodeGroup;
import com.couchbase.cbadmin.assets.NodeGroupList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Couchbase Administrative Client
 *
 * @author mnunberg
 */
public class CouchbaseAdminImpl implements CouchbaseAdmin {
  static final Gson gs = new Gson();
  private final URL entryPoint;
  private CloseableHttpClient cli;
  private final String user;
  private final String passwd;
  private final Logger logger = LoggerFactory.getLogger(CouchbaseAdminImpl.class);
  private Node myNode = null;
  private AliasLookup aliasLookup = new AliasLookup();
  {
    // common idioms
    aliasLookup.associateAlias("127.0.0.1", "localhost");
  }

  /**
   * Known URIS.
   */
  public static final String P_BUCKETS = "/pools/default/buckets";


  // From cluster_connect
  public static final String P_SETTINGS_WEB = "/settings/web";

  // Not in api.txt, but present in manual
  public static final String P_POOL_NODES = "/pools/nodes";

  // Standard
  public static final String P_ADDNODE = "/controller/addNode";
  public static final String P_POOLS_DEFAULT = "/pools/default";
  public static final String P_POOLS = "/pools";
  public static final String P_JOINCLUSTER = "/node/controller/doJoinCluster";
  public static final String P_REBALANCE = "/controller/rebalance";
  public static final String P_REBALANCE_STOP = "/controller/stopRebalance";
  public static final String P_REBALANCE_PROGRESS = "/pools/default/rebalanceProgress";
  public static final String P_FAILOVER = "/controller/failOver";
  public static final String P_READD = "/controller/reAddNode";
  public static final String P_EJECT = "/controller/ejectNode";
  public static final String _P_NODES_SELF = "/nodes/self";

  public static final String _P_SERVERGROUPS = "/pools/default/serverGroups";


  /**
   * Constructs a new connection to the Couchbase administrative API
   *
   * @param url The URL to the server. The path is ignored
   * @param username The administrative username, usually 'Administrator'
   * @param password The administrative password
   */
  public CouchbaseAdminImpl(URL url, String username, String password) {
    entryPoint = url;
    user = username;
    passwd = password;

    BasicHeader hdr = new BasicHeader(
            HttpHeaders.AUTHORIZATION,
            "Basic " + Base64.encodeBase64String(
              (username+":"+password).getBytes()));

    List<Header> hdrList = new ArrayList<Header>();
    hdrList.add(hdr);

    cli = HttpClients.custom()
            .setDefaultHeaders(hdrList)
            .build();
  }

  private JsonElement extractResponse(
          HttpResponse res,
          HttpRequestBase req,
          int expectCode)
          throws RestApiException, IOException {

    JsonElement ret;
    HttpEntity entity;
    entity = res.getEntity();
    if (entity == null) {
      ret = new JsonObject();

    } else {
      Header contentType = entity.getContentType();
      if (contentType == null || contentType.getValue().contains("json") == false) {
        ret = new JsonObject();
        ret.getAsJsonObject().addProperty("__raw_response",
                                          IOUtils.toString(entity.getContent()));
      } else {
        JsonReader reader = new JsonReader(
                new InputStreamReader(entity.getContent()));
        ret = gs.fromJson(reader, JsonObject.class);
      }
    }
    if (res.getStatusLine().getStatusCode() != expectCode) {
      throw new RestApiException(ret, res.getStatusLine(), req);
    }

    return ret;
  }

  private JsonElement getResponseJson(HttpRequestBase req, int expectCode)
      throws RestApiException, IOException {
    logger.trace("{} {}", req.getMethod(), req.getURI());

    CloseableHttpResponse res = cli.execute(req);
    try {
      return extractResponse(res, req, expectCode);
    } finally {
      if (res.getEntity() != null) {
        // Ensure the content is completely removed from the stream,
        // so we can re-use the connection
        EntityUtils.consumeQuietly(res.getEntity());
      }
    }
  }

  private JsonElement getResponseJson(
          HttpRequestBase req, String path, int expectCode)
          throws RestApiException, IOException {

    URL url;
    try {
      url = new URL(entryPoint, path);
      req.setURI(url.toURI());
    } catch (MalformedURLException ex) {
      throw new IOException(ex);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }

    return getResponseJson(req, expectCode);
  }

  @Override
  public JsonElement getJson(String path) throws IOException, RestApiException {
    return getResponseJson(new HttpGet(), path, 200);
  }

  private static UrlEncodedFormEntity makeFormEntity(Map<String,String> params) {

    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    for (Entry<String,String> ent : params.entrySet()) {
      nvps.add(new BasicNameValuePair(ent.getKey(), ent.getValue()));
    }
    try {
      return new UrlEncodedFormEntity(nvps);

    } catch (UnsupportedEncodingException ex) {
      throw new IllegalArgumentException(ex);
    }

  }

  @Override
  public Map<String, Bucket> getBuckets() throws RestApiException {
    JsonElement e;
    Map<String,Bucket> ret = new HashMap<String, Bucket>();

    try {
      e = getJson(P_BUCKETS);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    JsonArray arr;
    if (!e.isJsonArray()) {
      throw new RestApiException("Expected JsonObject", e);
    }

    arr = e.getAsJsonArray();
    for (int i = 0; i < arr.size(); i++) {
      JsonElement tmpElem = arr.get(i);
      if (!tmpElem.isJsonObject()) {
        throw new RestApiException("Expected JsonObject", tmpElem);
      }

      Bucket bucket = new Bucket(tmpElem.getAsJsonObject());
      ret.put(bucket.getName(), bucket);
    }
    return ret;
  }

  @Override
  public NodeGroupList getGroupList() throws RestApiException {
    JsonElement e ;
    Map<String, NodeGroup> ret = new HashMap<String, NodeGroup>();
    try {
      e = getJson(_P_SERVERGROUPS);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    if (!e.isJsonObject()) {
      throw new RestApiException("Expected JSON object", e);
    }
    return new NodeGroupList(e.getAsJsonObject());
  }

  @Override
  public List<Node> getNodes() throws RestApiException {
    List<Node> ret = new ArrayList<Node>();
    JsonElement e;
    try {
      e = getJson(P_POOL_NODES);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    if (!e.isJsonObject()) {
      throw new RestApiException("Expected JsonObject", e);
    }

    JsonObject obj = e.getAsJsonObject();
    JsonArray nodesArr;
    e = obj.get("nodes");

    if (e == null) {
      throw new RestApiException("Expected 'nodes' array", obj);
    }

    nodesArr = e.getAsJsonArray();
    for (int i = 0; i < nodesArr.size(); i++) {
      e = nodesArr.get(i);
      JsonObject nObj;
      if (!e.isJsonObject()) {
        throw new RestApiException("Malformed node entry", e);
      }
      nObj = e.getAsJsonObject();
      Node n = new Node(nObj);
      ret.add(n);
    }

    return ret;
  }

  private static Inet4Address getIp4Lookup(String host) throws RestApiException {
    Inet4Address inaddr = null;
    InetAddress[] addrList;
    try {
      addrList = InetAddress.getAllByName(host);
    } catch (UnknownHostException ex) {
      throw new RestApiException(ex);
    }

    for (InetAddress addr : addrList) {
      if (addr instanceof Inet4Address) {
        inaddr = (Inet4Address) addr;
        break;
      }
    }

    if (inaddr == null) {
      throw new RestApiException("Couldn't get IPv4 address");
    }
    return inaddr;
  }

  @Override
  public void addNewNode(URL newNode) throws RestApiException {
    addNewNode(newNode, user, passwd);
  }

  @Override
  public void addNewNode(CouchbaseAdminImpl newNode) throws RestApiException {
    addNewNode(newNode.getEntryPoint());
  }

  @Override
  public void addNewNode(URL newNode, String nnUser, String nnPass)
          throws RestApiException {

    int ePort = newNode.getPort();
    if (ePort == -1) {
      ePort = entryPoint.getPort();
    }

    InetAddress inaddr = getIp4Lookup(newNode.getHost());

    if (newNode.getHost().equals(entryPoint.getHost())
            && ePort == entryPoint.getPort()) {
      throw new IllegalArgumentException("Can't join node to self");
    }

    Map<String,String> params = new HashMap<String, String>();
    params.put("user", nnUser);
    params.put("password", nnPass);
    params.put("hostname", inaddr.getHostAddress() + ":" + ePort);

    HttpPost post = new HttpPost();
    post.setEntity(makeFormEntity(params));

    try {
      getResponseJson(post, P_ADDNODE, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public Node findNode(URL node) throws RestApiException {
    Node ret = null;
    Collection<String> aliases = aliasLookup.getForAlias(node.getHost());
    List<Node> nodes = getNodes();
    for (Node n : nodes) {

      boolean hostMatches = false;
      // Not the same host

      for (String alias : aliases) {
        if (n.getRestUrl().getHost().equals(alias)) {
          hostMatches = true;
          break;
        }
      }

      if (!hostMatches) {
        continue;
      }

      if (node.getPort() != -1) {
        if (n.getRestUrl().getPort() == node.getPort()) {
          return n;
        }
      } else {
        if (ret != null) {
          throw new IllegalArgumentException(
                  "Found more than one node with the same hostname. Need port");
        }
        ret = n;
      }
    }

    if (ret == null) {
      throw new RestApiException("Couldn't find node " + node);
    }

    return ret;
  }

  @Override
  public void initNewCluster(ClusterConfig config) throws RestApiException {
    // We need two requests, one to set the memory quota, the other
    // to set the authentication params.
    HttpPost memInit = new HttpPost();
    Map<String,String> params = new HashMap<String, String>();
    params.put("memoryQuota", "" + config.memoryQuota);
    memInit.setEntity(makeFormEntity(params));
    try {
      getResponseJson(memInit, P_POOLS_DEFAULT, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    params.clear();
    HttpPost authInit = new HttpPost();
    params.put("port", "SAME");
    params.put("username", this.user);
    params.put("password", this.passwd);
    authInit.setEntity(makeFormEntity(params));
    try {
      getResponseJson(authInit, P_SETTINGS_WEB, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void joinCluster(URL clusterUrl) throws RestApiException {
    HttpPost req = new HttpPost();
    int ePort = clusterUrl.getPort();
    if (ePort == -1) {
      ePort = entryPoint.getPort();
    }

    Map<String,String> params = new HashMap<String, String>();
    params.put("clusterMemberHostIp", clusterUrl.getHost());
    params.put("clusterMemberPort", "" + ePort);
    params.put("user", user);
    params.put("password", passwd);

    req.setEntity(makeFormEntity(params));
    try {
      getResponseJson(req, P_JOINCLUSTER, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void rebalance(List<Node> remaining,
                        List<Node> failed_over,
                        List<Node> to_remove)
          throws RestApiException {

    List<String> ejectedIds = new ArrayList<String>();
    List<String> remainingIds = new ArrayList<String>();
    Map<String,String> params = new HashMap<String, String>();

    if (failed_over != null) {
      for (Node ejectedNode : failed_over) {
        ejectedIds.add(ejectedNode.getNSOtpNode());
      }
    }

    if (remaining == null) {
      remaining = getNodes();
    }

    for (Node remainingNode : remaining) {
      if (failed_over != null && failed_over.contains(remainingNode)) {
        continue;
      }
      remainingIds.add(remainingNode.getNSOtpNode());
    }

    if (to_remove != null) {
      for (Node nn : to_remove) {
        ejectedIds.add(nn.getNSOtpNode());
      }
    }

    params.put("knownNodes", StringUtils.join(remainingIds, ","));
    params.put("ejectedNodes", StringUtils.join(ejectedIds, ","));

    HttpPost req = new HttpPost();
    req.setEntity(makeFormEntity(params));
    try {
      getResponseJson(req, P_REBALANCE, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void rebalance() throws RestApiException {
    rebalance(null, null, null);
  }

  @Override
  public void createBucket(BucketConfig config) throws RestApiException {
    HttpPost req = new HttpPost();
    req.setEntity(makeFormEntity(config.makeParams()));
    try {

      // 202 Accepted
      getResponseJson(req, P_BUCKETS, 202);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void deleteBucket(String name) throws RestApiException {
    HttpDelete req = new HttpDelete();
    try {
      getResponseJson(req, P_BUCKETS + "/" +  name, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void stopRebalance() throws RestApiException {
    HttpPost post = new HttpPost();
    try {
      getResponseJson(post, P_REBALANCE_STOP, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public RebalanceInfo getRebalanceStatus() throws RestApiException {
    JsonElement js;

    try {
      js = getResponseJson(new HttpGet(), P_REBALANCE_PROGRESS, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    if (!js.isJsonObject()) {
      throw new RestApiException("Expected JSON object", js);
    }
    return new RebalanceInfo(js.getAsJsonObject());
  }


  private void otpPostCommon(Node node, String uri) throws RestApiException {
    HttpPost post = new HttpPost();
    Map<String,String> params = new HashMap<String, String>();
    params.put("otpNode", node.getNSOtpNode());
    post.setEntity(makeFormEntity(params));
    try {
      getResponseJson(post, uri, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void failoverNode(Node node) throws RestApiException {
    otpPostCommon(node, P_FAILOVER);
  }

  @Override
  public void readdNode(Node node) throws RestApiException {
    otpPostCommon(node, P_READD);
  }

  @Override
  public void ejectNode(Node node) throws RestApiException {
    otpPostCommon(node, P_EJECT);
  }

  @Override
  public ConnectionInfo getInfo() throws RestApiException {
    try {
      JsonElement js = getResponseJson(new HttpGet(), P_POOLS, 200);
      if (!js.isJsonObject()) {
        throw new RestApiException("Expected JSON Object", js);
      }

      return new ConnectionInfo(js.getAsJsonObject());

    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public Node getAsNode(boolean forceRefresh) throws RestApiException {
    if (myNode != null && forceRefresh == false) {
      return myNode;
    }

    myNode = findNode(entryPoint);
    return myNode;
  }

  @Override
  public Node getAsNode() throws RestApiException {
    return getAsNode(false);
  }


  public NodeGroup findGroup(String name) throws RestApiException {
    NodeGroup group = getGroupList().find(name);
    if (group == null) {
      throw new RestApiException("No such group");
    }
    return group;
  }

  @Override
  public void renameGroup(NodeGroup from, String to) throws RestApiException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("name", to);
    // Create a PUT request.
    HttpPut putReq = new HttpPut();
    putReq.setEntity(makeFormEntity(params));
    try {
      getResponseJson(putReq, from.getUri().toString(), 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  public void renameGroup(String from, String to) throws RestApiException {
    renameGroup(findGroup(from), to);
  }

  @Override
  public void addGroup(String name) throws RestApiException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("name", name);
    HttpPost postReq = new HttpPost();
    postReq.setEntity(makeFormEntity(params));
    try {
      getResponseJson(postReq, _P_SERVERGROUPS, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void deleteGroup(NodeGroup group) throws RestApiException {
    HttpDelete del = new HttpDelete();
    try {
      getResponseJson(del, group.getUri().toString(), 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  public void deleteGroup(String name) throws RestApiException {
    deleteGroup(findGroup(name));
  }

  @Override
  public void allocateGroups(Map<NodeGroup, Collection<Node>> config,
                             NodeGroupList existingGroups) throws RestApiException {

    Set<NodeGroup> groups;
    if (existingGroups == null) {
      existingGroups = getGroupList();
    }

    groups = new HashSet<NodeGroup>(existingGroups.getGroups());
    if (config.keySet().size() > groups.size()) {
      throw new IllegalArgumentException("Too many groups specified");
    }

    JsonObject payload = new JsonObject();
    JsonArray groupsArray = new JsonArray();
    payload.add("groups", groupsArray);

    Set<Node> changedNodes = new HashSet<Node>();
    for (Collection<Node> ll : config.values()) {
      // Sanity
      for (Node nn : ll) {
        if (changedNodes.contains(nn)) {
          throw new IllegalArgumentException("Node " + nn + " specified twice");
        }
        changedNodes.add(nn);
      }
    }

    // Now go through our existing groups and see which ones are to be modified
    for (NodeGroup group : groups) {
      JsonObject curJson = new JsonObject();
      JsonArray nodesArray = new JsonArray();
      groupsArray.add(curJson);

      curJson.addProperty("uri", group.getUri().toString());
      curJson.add("nodes", nodesArray);
      Set<Node> nodes = new HashSet<Node>(group.getNodes());
      if (config.containsKey(group)) {
        nodes.addAll(config.get(group));
      }

      for (Node node : nodes) {
        boolean nodeRemains;

        /**
         * If the node was specified in the config, it either belongs to us
         * or it belongs to a different group. If it does not belong to us then
         * we skip it, otherwise it's placed back inside the group list.
         */
        if (!changedNodes.contains(node)) {
          nodeRemains = true;
        } else if (config.containsKey(group) && config.get(group).contains(node)) {
          nodeRemains = true;
        } else {
          nodeRemains = false;
        }

        if (!nodeRemains) {
          continue;
        }

        // Is it staying with us, or is it moving?
        JsonObject curNodeJson = new JsonObject();
        curNodeJson.addProperty("otpNode", node.getNSOtpNode());
        nodesArray.add(curNodeJson);
      }
    }

    HttpPut putReq = new HttpPut();
    try {
      putReq.setEntity(new StringEntity(new Gson().toJson(payload)));
    } catch (UnsupportedEncodingException ex) {
      throw new IllegalArgumentException(ex);
    }

    try {
      getResponseJson(putReq, existingGroups.getAssignmentUri().toString(), 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  public void assignNodeToGroup(NodeGroup target, Node src) throws RestApiException {
    Map<NodeGroup, Collection<Node>> mm = new HashMap<NodeGroup, Collection<Node>>();
    mm.put(target, Collections.singleton(src));
    allocateGroups(mm, null);
  }

  /**
   * Get the existing Node object, without refreshing
   * @return null if no node, the Node otherwise
   */
  public Node getCachedNode() {
    return myNode;
  }

  public String getUsername() {
    return user;
  }

  public String getPassword() {
    return passwd;
  }

  public URL getEntryPoint() {
    return entryPoint;
  }

  public AliasLookup getAliasLookupCache() {
    return aliasLookup;
  }

  /**
   * Copy this administrative client with its credentials for a new host
   * @param newHost The new host for the new object
   * @return The new client
   */
  public CouchbaseAdminImpl copyForHost(URL newHost) {
    return new CouchbaseAdminImpl(newHost, user, passwd);
  }

  /**
   * Add a view to the bucket. This is an extension API and is not strictly
   * administrative.
   * @param ep The node to use for creating the view.
   * @param config The configuration object defining the view to be created
   * @param pollTimeout time to wait until the view becomes ready, in millis.
   * @throws RestApiException
   */
  static public void defineView(Node ep, ViewConfig config, long pollTimeout) throws RestApiException {
    // Make a 'PUT' request here..
    CouchbaseAdminImpl adm = new CouchbaseAdminImpl(
            ep.getCouchUrl(),
            config.getBucketName(),
            config.getBucketPassword());

    // Format the URI
    StringBuilder ub = new StringBuilder()
            .append('/').append(config.getBucketName())
            .append("/_design/").append(config.getDesign());

    HttpPut req = new HttpPut();
    req.setHeader("Content-Type", "application/json");
    try {
      req.setEntity(new StringEntity(config.getDefinition()));
    } catch (UnsupportedEncodingException ex) {
      throw new RestApiException(ex);
    }

    try {
      adm.getResponseJson(req, ub.toString(), 201);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    if (pollTimeout > 0) {
      Collection<String> vNames = config.getViewNames();
      if (vNames == null || vNames.isEmpty()) {
        throw new IllegalArgumentException("No views defined");
      }

      String vName = vNames.iterator().next();

      long tmo = System.currentTimeMillis() + pollTimeout;
      String vUri = String.format("%s/_design/%s/_view/%s?limit=1",
                                  config.getBucketName(),
                                  config.getDesign(),
                                  vName);
      while (System.currentTimeMillis() < tmo) {
        try {
          adm.getJson(vUri);
          return;
        } catch (IOException ex) {
          throw new RestApiException(ex);
        } catch (RestApiException ex) {
          if (ex.getStatusLine() == null) {
            throw ex;
          }
          int statusCode = ex.getStatusLine().getStatusCode();
          if (statusCode != 500 && statusCode != 404) {
            throw ex;
          }
          adm.logger.trace("While waiting for view", ex);
          // Squash
        }
        try {
          Thread.sleep(500);
        } catch (InterruptedException ex) {
          adm.logger.error("While waiting.", ex);
          break;
        }
      }
      throw new RestApiException("Timed out waiting for view");
    }
  }
}
