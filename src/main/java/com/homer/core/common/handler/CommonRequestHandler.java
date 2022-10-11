package com.homer.core.common.handler;

import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.exceptions.UriNotFoundException;
import com.homer.core.common.model.Message;
import com.homer.core.common.model.MessageTypeEnum;
import com.homer.core.common.model.Response;
import com.homer.core.common.utils.LambdaExceptionUtil;
import com.homer.core.common.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CommonRequestHandler extends KafkaSendOutHandler {
    private static final Logger log = LoggerFactory.getLogger(CommonRequestHandler.class);

    protected LambdaExceptionUtil.Function_WithExceptions<Message, Observable, Exception> processWithException;
    protected Function<Message, Observable> process;

    public CommonRequestHandler() {
    }

    public CommonRequestHandler(
            LambdaExceptionUtil.Consumer_WithExceptions<Pair<String, Message>, IOException> sendOut
            , LambdaExceptionUtil.Function_WithExceptions<Message, Observable, Exception> process
            , String commonSendOut
    ) {
        this.sendOut = sendOut;
        this.processWithException = process;
        this.commonSendOut = commonSendOut;
    }

    public CommonRequestHandler(
            LambdaExceptionUtil.Consumer_WithExceptions<Pair<String, Message>, IOException> sendOut
            , Function<Message, Observable> process
            , String commonSendOut
    ) {
        this.sendOut = sendOut;
        this.process = process;
        this.commonSendOut = commonSendOut;
    }

    public void process(Message message, Consumer<Exception> logError) {
        try {
            Observable<?> observable = null;
            try {
                if (process != null) {
                    observable = process.apply(message);
                } else {
                    observable = processWithException.apply(message);
                }
                if (observable == null) {
                    this.response(message, getErrorMessage(new UriNotFoundException()));
                    return;
                }
                observable.subscribe(
                        data -> this.response(message, new Response<>(data)),
                        e -> this.response(message, getErrorMessage(e))
                );
            } catch (ForwardException e) {
                log.warn("message {} is forwarded to {}", message.getMessageId(), e.getTopic());
            } catch (Exception e) {
                log.error("exception while processing message", e);
                this.response(message, getErrorMessage(e));
            }
        } catch (Exception e) {
            try {
                logError.accept(e);
            } catch (Exception ex) {
                log.error("There is an message cannot read as String", e);
            }
        }
    }

    public void process(Supplier<Message> messageProvider, Consumer<Exception> logError) {
        Message message = messageProvider.get();
        this.process(message, logError);
    }

    private void response(Message message, Response response) {
        if (message.getResponseDestination() != null) {
            message.setMessageType(MessageTypeEnum.RESPONSE);
            message.setData(response);
            message.setUri(message.getResponseDestination().getUri());
            try {
                this.sendOut.accept(new Pair<>(this.getTopic(message.getResponseDestination().getTopic()), message));
            } catch (Exception e) {
                log.error("error while sending ou response", e);
            }
        }
    }

    private Response getErrorMessage(Throwable e) {
        if (e instanceof GeneralException) {
            if (((GeneralException) e).getSource() != null) {
                log.error("error while processing request", ((GeneralException) e).getSource());
            } else {
                log.error("error while processing request", e);
            }
        }
        return Response.fromException(e);
    }

    public static class ForwardException extends RuntimeException {
        private String topic;
        private Message msg;

        public ForwardException(String topic, Message msg) {
            this.topic = topic;
            this.msg = msg;
        }

        public ForwardException() {
        }

        public String getTopic() {
            return topic;
        }

        public Message getMsg() {
            return msg;
        }
    }
}
