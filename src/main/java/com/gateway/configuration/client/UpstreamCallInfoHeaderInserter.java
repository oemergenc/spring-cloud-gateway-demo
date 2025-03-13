package com.gateway.configuration.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.configuration.UpstreamCallInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;

public record UpstreamCallInfoHeaderInserter(ObjectMapper objectMapper) {

  private static final Logger LOG = LoggerFactory.getLogger(UpstreamCallInfoHeaderInserter.class);
  public static final String UPSTREAM_CALL_INFO_HEADER_NAME = "x-upstream-info";

  public void insert(UpstreamCallInfo upstreamCallInfo, ServerWebExchange serverWebExchange) {
    try {
      serverWebExchange
          .getResponse()
          .getHeaders()
          .add(UPSTREAM_CALL_INFO_HEADER_NAME, objectMapper.writeValueAsString(upstreamCallInfo));
    } catch (JsonProcessingException e) {
      LOG.warn("Can not serialize upstream call info with value: {}.", upstreamCallInfo, e);
    }
  }
}
