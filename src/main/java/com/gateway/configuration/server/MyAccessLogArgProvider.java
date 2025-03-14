/*
 * Copyright (c) 2020-2023 VMware, Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gateway.configuration.server;

import com.gateway.configuration.UpstreamCallInfo;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import java.net.SocketAddress;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import reactor.netty.ReactorNetty;
import reactor.netty.http.server.ConnectionInformation;
import reactor.netty.http.server.HttpServerRequest;
import reactor.util.annotation.Nullable;

class MyAccessLogArgProvider {

  HttpServerRequest request;
  HttpResponse response;

  static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
  static final String MISSING = "-";

  final SocketAddress remoteAddress;
  final String user = MISSING;
  ConnectionInformation connectionInfo;
  String zonedDateTime;
  ZonedDateTime accessDateTime;
  CharSequence method;
  CharSequence uri;
  String protocol;
  boolean chunked;
  long contentLength = -1;
  long startTime;
  private UpstreamCallInfo upstreamCallInfo;

  MyAccessLogArgProvider(@Nullable SocketAddress remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  public Integer status() {
    return response == null ? null : response.status().code();
  }

  public CharSequence requestHeader(CharSequence name) {
    Objects.requireNonNull(name, "name");
    return request == null ? null : request.requestHeaders().get(name);
  }

  public CharSequence responseHeader(CharSequence name) {
    Objects.requireNonNull(name, "name");
    return response == null ? null : response.headers().get(name);
  }

  public ZonedDateTime accessDateTime() {
    return accessDateTime;
  }

  public SocketAddress remoteAddress() {
    return remoteAddress;
  }

  public ConnectionInformation connectionInformation() {
    return connectionInfo;
  }

  public CharSequence method() {
    return method;
  }

  public CharSequence uri() {
    return uri;
  }

  public String protocol() {
    return protocol;
  }

  public long contentLength() {
    return contentLength;
  }

  public long duration() {
    return System.currentTimeMillis() - startTime;
  }

  MyAccessLogArgProvider onRequest() {
    this.accessDateTime = ZonedDateTime.now(ReactorNetty.ZONE_ID_SYSTEM);
    this.zonedDateTime = accessDateTime.format(DATE_TIME_FORMATTER);
    this.startTime = System.currentTimeMillis();
    if (request != null) {
      method = request.method().name();
      uri = request.uri();
      protocol = request.protocol();
    }
    return this;
  }

  void clear() {
    this.accessDateTime = null;
    this.zonedDateTime = null;
    this.method = null;
    this.uri = null;
    this.protocol = null;
    this.chunked = false;
    this.contentLength = -1;
    this.startTime = 0;
    this.connectionInfo = null;
    this.request = null;
    this.response = null;
  }

  MyAccessLogArgProvider chunked(boolean chunked) {
    this.chunked = chunked;
    return this;
  }

  MyAccessLogArgProvider increaseContentLength(long contentLength) {
    if (chunked
        && contentLength >= 0
        && !HttpMethod.HEAD.asciiName().contentEqualsIgnoreCase(method)) {
      if (this.contentLength == -1) {
        this.contentLength = 0;
      }
      this.contentLength += contentLength;
    }
    return this;
  }

  MyAccessLogArgProvider request(HttpServerRequest request) {
    this.request = Objects.requireNonNull(request, "request");
    onRequest();
    return this;
  }

  MyAccessLogArgProvider response(HttpResponse response) {
    this.response = Objects.requireNonNull(response, "response");
    return this;
  }

  MyAccessLogArgProvider contentLength(long contentLength) {
    this.contentLength = contentLength;
    return this;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("remoteAddress", remoteAddress);
    map.put("method", method);
    map.put("uri", uri);
    map.put("status", status());
    map.put("protocol", protocol);
    map.put("contentLength", contentLength);
    map.put("duration", duration());
    map.put("upstreamCallInfo", upstreamCallInfo);
    return map;
  }

  public void setUpstreamCallInfo(final UpstreamCallInfo upstreamCallInfo) {
    this.upstreamCallInfo = upstreamCallInfo;
  }
}
