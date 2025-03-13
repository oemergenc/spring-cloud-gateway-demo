package com.gateway.configuration.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientConfiguration {

  @Bean
  public WebClient webClient(WebClient.Builder webClientBuilder) {
    return webClientBuilder.build();
  }

  @Bean
  public UpstreamCallInfoHttpClientCustomizer upstreamCallInfoHttpClientCustomizer(
      ObjectMapper objectMapper) {
    return new UpstreamCallInfoHttpClientCustomizer(
        new UpstreamCallInfoHeaderInserter(objectMapper));
  }
}
