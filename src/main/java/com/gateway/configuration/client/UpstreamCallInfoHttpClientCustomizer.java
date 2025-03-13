package com.gateway.configuration.client;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.core.NestedExceptionUtils.getRootCause;

import com.gateway.configuration.UpstreamCallInfo;
import io.netty.handler.timeout.TimeoutException;
import java.net.ConnectException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.web.server.ServerWebExchange;
import reactor.netty.http.HttpInfos;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientInfos;
import reactor.util.context.ContextView;

public record UpstreamCallInfoHttpClientCustomizer(
    UpstreamCallInfoHeaderInserter upstreamCallInfoHeaderInserter) implements HttpClientCustomizer {

  private static final Logger LOG =
      LoggerFactory.getLogger(UpstreamCallInfoHttpClientCustomizer.class);
  public static final String UPSTREAM_CALL_NANO_TIME_ATTR_NAME =
      "upstream-call-request-start-nano-time";

  @Override
  public HttpClient customize(HttpClient httpClient) {
    return httpClient
        .mapConnect(
            mono ->
                mono.contextWrite(
                    context -> context.put(UPSTREAM_CALL_NANO_TIME_ATTR_NAME, System.nanoTime())))
        .doOnError(
            (httpClientRequest, throwable) -> {
              LOG.warn("Upstream caused an error without response.", throwable);
              addUpstreamCallInfoToContextHeader(httpClientRequest, -1);
              handleExceptionAndReRaise(throwable);
            },
            (httpClientResponse, throwable) -> {
              LOG.warn("Upstream caused an error with response.", throwable);
              addUpstreamCallInfoToContextHeader(
                  httpClientResponse, httpClientResponse.status().code());
              handleExceptionAndReRaise(throwable);
            })
        .doOnResponse(
            (httpClientResponse, connection) ->
                addUpstreamCallInfoToContextHeader(
                    httpClientResponse, httpClientResponse.status().code()));
  }

  private void addUpstreamCallInfoToContextHeader(
      HttpClientInfos httpClientInfos, int httpStatusCode) {

    var upstreamCallInfo = buildUpstreamCallInfo(httpClientInfos, httpStatusCode);

    httpClientInfos
        .currentContextView()
        .<ServerWebExchange>getOrEmpty(ServerWebExchange.class)
        .ifPresentOrElse(
            exchange -> upstreamCallInfoHeaderInserter.insert(upstreamCallInfo, exchange),
            () -> {
              var message =
                  "Can not add upstream call info to web exchange: Exchange not found"
                      + " in subscriber context. Upstream call info was: {}.";
              LOG.warn(message, upstreamCallInfo);
            });
  }

  private UpstreamCallInfo buildUpstreamCallInfo(
      HttpClientInfos httpClientInfos, int httpStatusCode) {
    var context = httpClientInfos.currentContextView();
    var methodAndPath = getUpstreamMethodAndPath(context, httpClientInfos);
    var duration = getDuration(context);
    return new UpstreamCallInfo(methodAndPath, duration, httpStatusCode);
  }

  private Duration getDuration(ContextView context) {
    return context
        .<Long>getOrEmpty(UPSTREAM_CALL_NANO_TIME_ATTR_NAME)
        .map(this::getNanosChecked)
        .orElse(Duration.ZERO);
  }

  private Duration getNanosChecked(Long timer) {
    var now = System.nanoTime();
    try {
      return Duration.ofNanos(Math.subtractExact(now, timer));
    } catch (ArithmeticException e) {
      LOG.warn("Duration calculation caused an overflow. Factors were: {} - {}.", now, timer);
    }
    return Duration.ZERO;
  }

  private String getUpstreamMethodAndPath(ContextView context, HttpInfos httpInfos) {
    return context
        .<ServerWebExchange>getOrEmpty(ServerWebExchange.class)
        .map(exchange -> exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR))
        .map(url -> httpInfos.method() + " " + url)
        .orElseGet(httpInfos::toString);
  }

  private void handleExceptionAndReRaise(Throwable throwable) {
    final Throwable rootCause = getRootCause(throwable);
    if (rootCause instanceof TimeoutException) {
      throw new UpstreamCallTimeoutException(throwable);
    } else if (rootCause instanceof ConnectException) {
      throw new UpstreamConnectionRefusedException(throwable);
    }
    throw new UpstreamCallException(throwable);
  }

  public static class UpstreamCallException extends RuntimeException {
    public UpstreamCallException(Throwable cause) {
      super(cause);
    }
  }

  public static class UpstreamCallTimeoutException extends UpstreamCallException {
    public UpstreamCallTimeoutException(Throwable cause) {
      super(cause);
    }
  }

  public static class UpstreamConnectionRefusedException extends UpstreamCallException {
    public UpstreamConnectionRefusedException(Throwable cause) {
      super(cause);
    }
  }
}
