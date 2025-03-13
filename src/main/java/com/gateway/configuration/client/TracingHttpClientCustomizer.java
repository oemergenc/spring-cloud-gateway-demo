package com.gateway.configuration.client;

import com.gateway.configuration.TracingChannelDuplexHandler;
import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

@Configuration
public class TracingHttpClientCustomizer implements HttpClientCustomizer {

  private final ContextSnapshotFactory contextSnapshotFactory;

  public TracingHttpClientCustomizer(ContextSnapshotFactory contextSnapshotFactory) {
    this.contextSnapshotFactory = contextSnapshotFactory;
  }

  @Override
  public HttpClient customize(HttpClient httpClient) {
    return httpClient.doOnConnected(
        connection -> {
          connection.addHandlerFirst(new TracingChannelDuplexHandler(contextSnapshotFactory));
        });
  }
}
