package com.gateway.configuration;

import static java.time.Duration.ZERO;

import java.time.Duration;

public record UpstreamCallInfo(String methodAndPath, Duration duration, int httpStatus) {

  public static final UpstreamCallInfo EMPTY =
      new UpstreamCallInfo("http://unknown:1337/", ZERO, -1);

  public boolean isEmpty() {
    return EMPTY.equals(this);
  }
}
