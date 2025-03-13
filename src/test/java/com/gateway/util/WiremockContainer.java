package com.gateway.util;

import org.testcontainers.containers.GenericContainer;

public class WiremockContainer extends GenericContainer<WiremockContainer> {

  public WiremockContainer() {
    super("wiremock/wiremock:3.3.1");
    this.withEnv(
        "WIREMOCK_OPTIONS",
        /* --jetty-header-request-size 100000 --jetty-header-response-size 100000: needed for too big token testing
         * --global-response-templating: needed to use templating in the login.html
         */
        "--jetty-header-request-size 100000 --jetty-header-response-size 100000 --global-response-templating");
    this.withExposedPorts(8080);
    this.withReuse(true);
  }
}
