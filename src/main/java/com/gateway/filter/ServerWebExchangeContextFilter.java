package com.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ServerWebExchangeContextFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return chain
        .filter(exchange)
        .contextWrite(context -> context.put(ServerWebExchange.class, exchange));
  }

  @Override
  public int getOrder() {
    // Run before NettyRoutingFilter which has order Ordered.LOWEST_PRECEDENCE
    return Ordered.HIGHEST_PRECEDENCE + 100;
  }
}
