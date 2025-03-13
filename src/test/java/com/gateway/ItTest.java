package com.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.http.HttpStatus;

public class ItTest extends AbstractTestBaseIT {

  @Test
  void accessLogWritten(CapturedOutput capturedOutput) {
    stubFor(get("/question").willReturn(aResponse()));

    var result =
        this.testClient
            .get()
            .uri("/question-route")
            .exchange()
            .expectBody(String.class)
            .returnResult();

    assertThat(result.getStatus().value()).isEqualTo(HttpStatus.OK.value());

    var loggedLines = extractLogs(capturedOutput, this::isAccessLog);

    assertThat(result.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(loggedLines)
        .isNotEmpty()
        .anySatisfy(
            logLine ->
                assertThatJson(logLine)
                    .node("@timestamp")
                    .isPresent()
                    .node("method")
                    .isEqualTo("GET")
                    .node("protocol")
                    .isEqualTo("HTTP/2.0")
                    .node("upstreamCallInfo.httpStatus")
                    .isEqualTo(HttpStatus.OK.value())
                    .node("upstreamCallInfo.methodAndPath")
                    .matches(new MatchesPattern(Pattern.compile("GET http://.*/question$")))
                    .node("upstreamCallInfo.duration")
                    .isPresent());
    verify(1, getRequestedFor(urlPathMatching("/question")));
  }

  private boolean isAccessLog(String logLine) {
    return logLine.contains("\"logger_name\":\"com.gateway.accesslog.MyAccessLogHandler\"");
  }

  private List<String> extractLogs(CapturedOutput capturedOutput, Predicate<String> p) {
    return await().until(() -> extractOutputFor(capturedOutput, p), logs -> !logs.isEmpty());
  }

  private List<String> extractOutputFor(CapturedOutput output, Predicate<String> p) {
    return new BufferedReader(new StringReader(output.getOut()))
        .lines()
        .filter(s -> s.startsWith("{") && p.test(s))
        .collect(Collectors.toList());
  }
}
