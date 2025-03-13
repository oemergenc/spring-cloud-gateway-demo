package com.gateway.configuration;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.Metrics;

@Configuration
public class TracingConfiguration {

  private final ObservationRegistry observationRegistry;

  public TracingConfiguration(ObservationRegistry observationRegistry) {
    this.observationRegistry = observationRegistry;
  }

  @PostConstruct
  public void postConstruct() {
    ContextRegistry.getInstance().registerThreadLocalAccessor(new Slf4jThreadLocalAccessor());
    ObservationThreadLocalAccessor.getInstance().setObservationRegistry(observationRegistry);
    Metrics.observationRegistry(observationRegistry);
  }

  @Bean
  public ContextSnapshotFactory contextSnapshotFactory() {
    return ContextSnapshotFactory.builder().build();
  }
}
