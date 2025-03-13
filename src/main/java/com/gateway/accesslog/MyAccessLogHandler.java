package com.gateway.accesslog;

import static com.gateway.configuration.server.UpstreamCallInfoOutboundHandler.UPSTREAM_CALL_INFO_ATTRIBUTE_KEY;

import com.gateway.configuration.UpstreamCallInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.http.server.HttpServerRequest;

public class MyAccessLogHandler extends ChannelDuplexHandler {

  private static final Logger log = LoggerFactory.getLogger(MyAccessLogHandler.class);
  MyAccessLogArgProvider accessLogArgProvider;

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    if (msg instanceof final HttpResponse response) {
      final HttpResponseStatus status = response.status();

      if (status.code() == HttpResponseStatus.CONTINUE.code()) {
        // "FutureReturnValueIgnored" this is deliberate
        ctx.write(msg, promise);
        return;
      }

      if (accessLogArgProvider == null) {
        accessLogArgProvider = new MyAccessLogArgProvider(ctx.channel().remoteAddress());
      } else {
        accessLogArgProvider.clear();
      }

      var upstreamCallInfo = ctx.channel().attr(UPSTREAM_CALL_INFO_ATTRIBUTE_KEY).getAndSet(null);
      accessLogArgProvider.setUpstreamCallInfo(
          upstreamCallInfo != null ? upstreamCallInfo : UpstreamCallInfo.EMPTY);

      ChannelOperations<?, ?> ops = ChannelOperations.get(ctx.channel());
      if (ops instanceof HttpServerRequest) {
        accessLogArgProvider.request((HttpServerRequest) ops);
      }

      final boolean chunked = HttpUtil.isTransferEncodingChunked(response);
      accessLogArgProvider.response(response).chunked(chunked);
      if (!chunked) {
        accessLogArgProvider.contentLength(HttpUtil.getContentLength(response, -1));
      }
    }
    if (msg instanceof LastHttpContent) {
      accessLogArgProvider.increaseContentLength(((LastHttpContent) msg).content().readableBytes());
      ctx.write(msg, promise.unvoid())
          .addListener(
              future -> {
                if (future.isSuccess()) {
                  log.info(
                      "Access log",
                      net.logstash.logback.argument.StructuredArguments.entries(
                          accessLogArgProvider.toMap()));
                }
              });
      return;
    }
    if (msg instanceof ByteBuf) {
      accessLogArgProvider.increaseContentLength(((ByteBuf) msg).readableBytes());
    }
    if (msg instanceof ByteBufHolder) {
      accessLogArgProvider.increaseContentLength(((ByteBufHolder) msg).content().readableBytes());
    }
    // "FutureReturnValueIgnored" this is deliberate
    ctx.write(msg, promise);
  }
}
