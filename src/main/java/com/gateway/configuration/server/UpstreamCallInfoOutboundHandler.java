package com.gateway.configuration.server;

import static com.gateway.configuration.client.UpstreamCallInfoHeaderInserter.UPSTREAM_CALL_INFO_HEADER_NAME;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.configuration.UpstreamCallInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpstreamCallInfoOutboundHandler extends ChannelOutboundHandlerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(UpstreamCallInfoOutboundHandler.class);

  public static final AttributeKey<UpstreamCallInfo> UPSTREAM_CALL_INFO_ATTRIBUTE_KEY =
      AttributeKey.valueOf(UpstreamCallInfo.class.getName());

  private final ObjectMapper objectMapper;

  public UpstreamCallInfoOutboundHandler(ObjectMapper objectMapper) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof HttpResponse httpResponse) {
      var header = httpResponse.headers().get(UPSTREAM_CALL_INFO_HEADER_NAME);
      if (header != null) {
        var upstreamCallInfo = parseUpstreamCallInfo(header);
        ctx.channel().attr(UPSTREAM_CALL_INFO_ATTRIBUTE_KEY).set(upstreamCallInfo);
        httpResponse.headers().remove(UPSTREAM_CALL_INFO_HEADER_NAME);
      }
    }
    super.write(ctx, msg, promise);
  }

  private UpstreamCallInfo parseUpstreamCallInfo(String header) {
    try {
      return objectMapper.readValue(header, UpstreamCallInfo.class);
    } catch (JsonProcessingException e) {
      LOG.warn(
          "Not able to deserialize upstream call info from transport header. Header was: {}.",
          header,
          e);
      return UpstreamCallInfo.EMPTY;
    }
  }
}
