package com.gateway.configuration.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.accesslog.MyAccessLogHandler;
import com.gateway.configuration.TracingChannelDuplexHandler;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.function.Function;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfiguration {

  private final ObjectMapper objectMapper;

  public ServerConfiguration(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  public NettyServerCustomizer defaultNettyServerCustomizer(
      ContextSnapshotFactory snapshotFactory) {
    return server ->
        server
            .accessLog(false)
            .metrics(true, Function.identity())
            .doOnConnection(
                connection -> {
                  connection.addHandlerFirst(
                      UpstreamCallInfoOutboundHandler.class.getSimpleName(),
                      new UpstreamCallInfoOutboundHandler(objectMapper));
                  connection.addHandlerFirst(new TracingChannelDuplexHandler(snapshotFactory));
                  connection.addHandlerFirst(
                      MyAccessLogHandler.class.getSimpleName(), new MyAccessLogHandler());
                });
  }
}
