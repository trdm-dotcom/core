package com.homer.core.common.handler;

import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.exceptions.UriNotFoundException;
import com.homer.core.common.model.Message;
import com.homer.core.common.model.MessageTypeEnum;
import com.homer.core.common.model.Response;
import com.homer.core.common.utils.FutureResult;
import com.homer.core.common.utils.LambdaExceptionUtil;
import com.homer.core.common.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * This class is used when you want handle request and send back response
 * To use this class. extend it and provide the sendOut
 */
public abstract class BaseRequestHandler<T extends Message> extends KafkaSendOutHandler {
    private static final Logger log = LoggerFactory.getLogger(BaseRequestHandler.class);
    private int expiredAt = 10000;

    protected BaseRequestHandler() {
    }

    public BaseRequestHandler(
            LambdaExceptionUtil.Consumer_WithExceptions<Pair<String, Message>, IOException> sendOut
            , String commonSendOut
    ) {
        this.sendOut = sendOut;
        this.commonSendOut = commonSendOut;
    }

    // need to override this method
    protected abstract Object handle(T message);

    public void process(Supplier<T> messageProvider, Consumer<Exception> logError) {
        T message = messageProvider.get();
        this.process(message, logError);
    }

    public void setExpiredAt(int expiredAt) {
        this.expiredAt = expiredAt;
    }

    public void process(T message) {
        this.process(message, null);
    }

    public void process(T message, Consumer<Exception> logError) {
        try {
            if (this.expiredAt > 0 && message.getT() != null && message.getT() > 0
                    && System.currentTimeMillis() - message.getT() > this.expiredAt) {
                log.info("ignore message {} since it's too old", message);
                return;
            }
            Object result = this.handle(message);
            try {
                if (result == null) {
                    this.response(message, new UriNotFoundException());
                    return;
                }
                if (result instanceof Observable) {
                    ((Observable<?>) result).subscribe(
                            data -> this.response(message, data),
                            err -> this.response(message, err)
                    );
                } else if (result instanceof FutureResult) {
                    try {
                        this.response(message, ((FutureResult<?>) result).getAndWait());
                    } catch (Exception e) {
                        this.response(message, e);
                    }
                } else if (result instanceof CompletionStage) {
                    ((CompletionStage<?>) result).
                            thenAcceptAsync(data -> this.response(message, data))
                            .exceptionally(err -> {
                                this.response(message, err);
                                return null;
                            });
                } else {
                    this.response(message, result);
                }
            } catch (Exception e) {
                log.error("exception while processing message", e);
                this.response(message, e);
            }
        } catch (Exception e) {
            if (logError != null) {
                try {
                    logError.accept(e);
                } catch (Exception ex) {
                    log.error("There is an message cannot read as String", e);
                }
            } else {
                this.response(message, e);
                log.error("exception while handling message", e);
            }
        }
    }

    protected void response(T message, Object data) {
        if (message.getResponseDestination() != null) {
            this.response(message, new Response(data));
        }
    }

    protected void response(T message, Throwable error) {
        if (message.getResponseDestination() != null) {
            this.response(message, getErrorMessage(error));
        } else {
            log.error("error while handle request {}", message.getUri(), error);
        }
    }

    protected void response(T message, Response response) {
        if (message.getResponseDestination() == null || message.getResponseDestination().getTopic() == null || message.getResponseDestination().getTopic().isEmpty()) {
            return;
        }
        message.setMessageType(MessageTypeEnum.RESPONSE);
        message.setData(response);
        message.setUri(message.getResponseDestination().getUri());
        try {
            this.sendOut.accept(new Pair<>(this.getTopic(message.getResponseDestination().getTopic()), message));
        } catch (Exception e) {
            log.error("error while sending out response", e);
        }
    }

    protected Response getErrorMessage(Throwable e) {
        if (e instanceof CompletionException && e.getCause() != null) {
            return this.getErrorMessage(e.getCause());
        }
        if (e instanceof GeneralException) {
            if (((GeneralException) e).getSource() != null) {
                log.error("error while processing request", ((GeneralException) e).getSource());
            } else {
                log.error("error while processing request", e);
            }
        }
        return Response.fromException(e);
    }
}
