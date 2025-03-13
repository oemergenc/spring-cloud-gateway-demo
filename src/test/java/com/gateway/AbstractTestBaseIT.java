package com.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;

import com.gateway.util.WiremockContainer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
public class AbstractTestBaseIT {

  @Autowired WebTestClient testClient;

  protected static WiremockContainer wiremock = startedWiremockContainer();

  static WiremockContainer startedWiremockContainer() {
    wiremock = new WiremockContainer();
    wiremock.start();
    wiremock.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger(AbstractTestBaseIT.class)));
    configureFor(wiremock.getHost(), wiremock.getFirstMappedPort());
    return wiremock;
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    final String wiremockUrl = "http://" + wiremock.getHost() + ":" + wiremock.getFirstMappedPort();
    System.setProperty("MOCK_BACKEND", wiremockUrl);
  }

  @BeforeEach
  public void beforeEach() {
    WireMock.reset();
  }
}
