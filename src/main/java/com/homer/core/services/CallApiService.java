package com.homer.core.services;

import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.exceptions.UriNotFoundException;
import com.homer.core.common.model.Status;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@Slf4j
public class CallApiService {
    private final Supplier<WebClient> webClient;

    public CallApiService() {
        this.webClient = () -> WebClient.builder()
                .filter(ExchangeFilterFunction.ofResponseProcessor(this::exchangeFilterResponseProcessor))
                .clientConnector(new JettyClientHttpConnector(new CustomHttpClient(new SslContextFactory.Client())))
                .build();
    }

    public <T> CompletableFuture<T> get(String baseUrl, Consumer<UriBuilder> queryParameterBuilder, Class<T> clazz) {
        return webClient.get().get()
                .uri(uriBuilder -> {
                    uriBuilder.path(baseUrl);
                    if (queryParameterBuilder != null) {
                        queryParameterBuilder.accept(uriBuilder);
                    }
                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-cache")
                .retrieve()
                .bodyToMono(clazz)
                .toFuture()
                ;
    }

    public <T> CompletableFuture<T> get(String baseUrl, Consumer<UriBuilder> queryParameterBuilder, ParameterizedTypeReference<T> clazz) {
        return webClient.get().get()
                .uri(uriBuilder -> {
                    uriBuilder.path(baseUrl);
                    if (queryParameterBuilder != null) {
                        queryParameterBuilder.accept(uriBuilder);
                    }
                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-cache")
                .retrieve()
                .bodyToMono(clazz)
                .toFuture()
                ;
    }

    public <T> CompletableFuture<T> post(String msgId, String url, Object body, Class<T> clazz) {
        log.info("{} post:{}:{}", msgId, url, body);
        try {
            return webClient.get().post()
                    .uri(uriBuilder -> uriBuilder.path(url).build())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Cache-Control", "no-cache")
                    .retrieve()
                    .bodyToMono(clazz)
                    .toFuture()
                    ;
        } catch (Exception e) {
            log.error("", e);
            throw e;
        }
    }

    private static class SessionExpiredException extends GeneralException {
        public SessionExpiredException() {
            super(HttpStatus.UNAUTHORIZED.name());
        }
    }

    public static class CustomHttpClient extends HttpClient {
        public CustomHttpClient(SslContextFactory sslContextFactory) {
            super(sslContextFactory);
        }

        @Override
        public Request newRequest(URI uri) {
            Request request = super.newRequest(uri);
            return enhance(request);
        }

        private Request enhance(Request request) {
            StringBuilder requestBuilder = new StringBuilder();
            StringBuilder responseBuilder = new StringBuilder();
            request.onRequestBegin(theRequest -> {
                requestBuilder.append(" request ");
                requestBuilder.append(theRequest.getMethod());
                requestBuilder.append(" ");
                requestBuilder.append(theRequest.getURI());
                requestBuilder.append(" ");
                requestBuilder.append(theRequest.getParams());
                requestBuilder.append(" +++ ");
            });
            request.onRequestHeaders(theRequest -> {
                requestBuilder.append("Header:");
                for (HttpField header : theRequest.getHeaders()) {
                    requestBuilder.append(header.getName());
                    requestBuilder.append(":");
                    requestBuilder.append(String.join(" ", header.getValues()));
                    requestBuilder.append(";");
                }
                requestBuilder.append(" +++ ");
            });
            request.onRequestContent((theRequest, content) -> {
                requestBuilder.append("Body:");
                requestBuilder.append(StandardCharsets.UTF_8.decode(content));
                requestBuilder.append(" +++ ");
            });
            request.onRequestSuccess(theRequest -> log.warn(requestBuilder.toString()));
            request.onResponseBegin(theResponse -> {
                responseBuilder.append(" response ");
                responseBuilder.append(theResponse.getStatus());
                responseBuilder.append(" ");
                responseBuilder.append(theResponse.getRequest().getURI());
                responseBuilder.append(" +++ ");
            });
            request.onResponseHeaders(theResponse -> {
                responseBuilder.append("Header:");
                for (HttpField header : theResponse.getHeaders()) {
                    responseBuilder.append(header.getName());
                    responseBuilder.append(":");
                    responseBuilder.append(String.join(" ", header.getValues()));
                    requestBuilder.append(";");
                }
                responseBuilder.append(" +++ ");
            });
            request.onResponseContent((theResponse, content) -> {
                responseBuilder.append("Body:");
                responseBuilder.append(StandardCharsets.UTF_8.decode(content));
                responseBuilder.append(" +++ ");
            });
            request.onResponseSuccess(theResponse -> log.warn(responseBuilder.toString()));
            return request;
        }
    }

    private Mono<ClientResponse> exchangeFilterResponseProcessor(ClientResponse response) {
        HttpStatus status = response.statusCode();
        log.info("get status {}", status);
        if (HttpStatus.NOT_FOUND.equals(status)) {
            return Mono.error(new UriNotFoundException());
        }
        if (HttpStatus.UNAUTHORIZED.equals(status) || HttpStatus.FORBIDDEN.equals(status)) {
            return Mono.error(new SessionExpiredException());
        }
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status) || HttpStatus.BAD_REQUEST.equals(status)) {
            return response.bodyToMono(Status.class)
                    .flatMap(body -> {
                        log.warn("error {}", body);
                        return Mono.error(body.create());
                    });
        }
        if (HttpStatus.OK.equals(status) || HttpStatus.CREATED.equals(status) || HttpStatus.ACCEPTED.equals(status)) {
            return Mono.just(response);
        }
        return Mono.error(new RuntimeException("Unhandled http status:" + status));
    }
}
