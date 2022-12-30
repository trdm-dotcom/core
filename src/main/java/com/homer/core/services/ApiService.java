package com.homer.core.services;

import com.ea.async.Async;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.exceptions.UriNotFoundException;
import com.homer.core.common.model.Status;
import com.homer.core.common.redis.CoordinatorService;
import com.homer.core.common.redis.RedisDao;
import com.homer.core.configurations.AppConf;
import com.homer.core.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
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
public class ApiService {
    private final RedisDao redisDao;
    private final Supplier<WebClient> webClient;
    private final CoordinatorService coordinatorService;
    public static final String TOKEN_ACQUIRE_KEY = "getOperatorToken";
    private final AppConf appConf;

    public ApiService(
            RedisDao redisDao,
            CoordinatorService coordinatorService,
            AppConf appConf
    ) {
        this.redisDao = redisDao;
        this.coordinatorService = coordinatorService;
        this.appConf = appConf;
        this.webClient = () -> WebClient.builder()
                .filter(ExchangeFilterFunction.ofResponseProcessor(this::exchangeFilterResponseProcessor))
                .clientConnector(new JettyClientHttpConnector(new CustomHttpClient(new SslContextFactory.Client())))
                .build();
    }

    public WebClient create() {
        return this.webClient.get();
    }

    public <T> CompletableFuture<T> get(String msgId, String host, String url, Consumer<UriBuilder> queryParameterBuilder, String keyToken, Class<T> clazz) {
        Token token = this.redisDao.hGet(Constants.REDIS_KEY_TOKEN, keyToken, Token.class);
        return webClient.get()
                .get()
                .uri(uriBuilder -> {
                    uriBuilder.host(host).path(url);
                    if (queryParameterBuilder != null) {
                        queryParameterBuilder.accept(uriBuilder);
                    }
                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", String.format("jwt %s", token.getAccessToken()))
                .header("Cache-Control", "no-cache")
                .retrieve()
                .bodyToMono(clazz)
                .toFuture();
    }

    public <T> CompletableFuture<T> post(String msgId, String host, String url, Object body, String keyToken, Class<T> clazz) {
        Token token = this.redisDao.hGet(Constants.REDIS_KEY_TOKEN, keyToken, Token.class);
        return webClient.get().post()
                .uri(uriBuilder -> uriBuilder.host(host).path(url).build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", String.format("jwt %s", token.getAccessToken()))
                .header("Cache-Control", "no-cache")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(clazz)
                .toFuture();
    }

    public <T> CompletableFuture<T> put(String msgId, String host, String url, Object body, String keyToken, Class<T> clazz) {
        Token token = this.redisDao.hGet(Constants.REDIS_KEY_TOKEN, keyToken, Token.class);
        return webClient.get().put()
                .uri(uriBuilder -> uriBuilder.host(host).path(url).build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", String.format("jwt %s", token.getAccessToken()))
                .header("Cache-Control", "no-cache")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(clazz)
                .toFuture();
    }

    public <T> CompletableFuture<T> delete(String msgId, String host, String url, Consumer<UriBuilder> queryParameterBuilder, String keyToken, Class<T> clazz) {
        Token token = this.redisDao.hGet(Constants.REDIS_KEY_TOKEN, keyToken, Token.class);
        return webClient.get().delete()
                .uri(uriBuilder -> {
                    uriBuilder.host(host).path(url);
                    if (queryParameterBuilder != null) {
                        queryParameterBuilder.accept(uriBuilder);
                    }
                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", String.format("jwt %s", token.getAccessToken()))
                .header("Cache-Control", "no-cache")
                .retrieve()
                .bodyToMono(clazz)
                .toFuture()
                ;
    }

    private <T> CompletableFuture<T> authentication(String host, String url, Object loginRequest, Class<T> clazz) {
        return webClient.get().post()
                .uri(uriBuilder -> uriBuilder.host(host).path(url).build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-cache")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(loginRequest))
                .retrieve()
                .bodyToMono(clazz)
                .toFuture();

    }

    public <T> CompletableFuture<T> withAuthenticated(String host, String loginUrl, String refreshUrl, Object loginRequest, Object refreshRequest, String keyToken, Class<T> clazzLogin, Class<T> clazzRefresh, Supplier<CompletableFuture<T>> supplier) {
        Token token = this.redisDao.hGet(Constants.REDIS_KEY_TOKEN, keyToken, Token.class);
        if (token == null) {
            log.info("no token yet. will get new token");
            Async.await(this.getToken(host, loginUrl, refreshUrl, keyToken, loginRequest, refreshRequest, clazzLogin, clazzRefresh));
        }
        while (true) {
            try {
                T data = Async.await(supplier.get());
                return CompletableFuture.completedFuture(data);
            } catch (Exception e) {
                if (!(e.getCause() instanceof SessionExpiredException)) {
                    log.error("error while calling api", e);
                    throw e;
                }
            }
            log.info("seem token is expired. will get new token");
            while (true) {
                try {
                    Async.await(this.getToken(host, loginUrl, refreshUrl, keyToken, loginRequest, refreshRequest, clazzLogin, clazzRefresh));
                    break;
                } catch (Exception ex) {
                    log.error("fail to get token", ex);
                }
            }
        }
    }

    private <T> CompletableFuture<T> refreshToken(String host, String url, Object refreshTokenRequest, Class<T> clazz) {
        return webClient.get().post()
                .uri(uriBuilder -> uriBuilder.host(host).path(url).build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-cache")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(refreshTokenRequest))
                .retrieve()
                .bodyToMono(clazz)
                .toFuture();
    }

    private <T> CompletableFuture<Void> getToken(String host, String loginUrl, String refreshUrl, String keyToken, Object loginRequest, Object refreshRequest, Class<T> clazzLogin, Class<T> clazzRefresh) {
        String acquireKey = this.coordinatorService.acquire(TOKEN_ACQUIRE_KEY, appConf.getNodeId(), 60);
        if (acquireKey != null) {
            log.warn("getToken got access permission");
            Token token = this.redisDao.hGet(Constants.REDIS_KEY_TOKEN, keyToken, Token.class);
            if (token == null) {
                Object loginResponse = Async.await(this.authentication(host, loginUrl, loginRequest, clazzLogin));
                log.info("login result {}", loginResponse);
            } else {
                try {
                    Object refreshTokenResponse = Async.await(this.refreshToken(host, refreshUrl, refreshRequest, clazzRefresh));
                } catch (Exception e) {
                    log.error("fail to refresh token. will login again");
                    Object loginResponse = Async.await(this.authentication(host, loginUrl, loginRequest, clazzLogin));
                    log.info("login result {}", loginResponse);
                }
            }
            this.redisDao.saveToMap(Constants.REDIS_KEY_TOKEN, keyToken, token);
            this.coordinatorService.release(TOKEN_ACQUIRE_KEY);
        } else {
            log.warn("getToken got no access permission. will wait for other thread to finish");
            Async.await(this.coordinatorService.waitForResultFuture(TOKEN_ACQUIRE_KEY));
            log.warn("new token has been issued");
        }
        return CompletableFuture.completedFuture(null);
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

    private static class SessionExpiredException extends GeneralException {
        public SessionExpiredException() {
            super(HttpStatus.UNAUTHORIZED.name());
        }
    }

    @Data
    @AllArgsConstructor
    public static class Token {
        private String accessToken;
        private String refreshToken;
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
}
