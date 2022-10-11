package com.homer.core.common.handler;

import com.homer.core.common.exceptions.QueryTimeoutException;
import com.homer.core.common.model.Message;
import com.homer.core.common.model.MessageTypeEnum;
import com.homer.core.common.model.Response;
import com.homer.core.common.utils.FutureResult;
import com.homer.core.common.utils.LambdaExceptionUtil;
import com.homer.core.common.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.exceptions.OnErrorNotImplementedException;
import rx.subjects.AsyncSubject;
import rx.subjects.Subject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * This class is used when you want to send a request and receive response
 * To use this class. extend it and provide the sendOut
 */
public class KafkaRequestResponseHandler extends KafkaSendOutHandler {
    private static final Logger log = LoggerFactory.getLogger(KafkaRequestResponseHandler.class);
    /**
     * expiredAt in seconds
     */
    protected int defaultTimeout = 180000;
    protected String responseTopic;
    protected ConcurrentHashMap<String, Subject<Message, Message>> pendingSyncRequests = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<String, CompletableFuture<Message>> pendingAsyncRequests = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<String, TimeoutData> timeoutChecks = new ConcurrentHashMap<>();
    protected Timer timeoutTimer = new Timer();

    public String getResponseTopic() {
        return responseTopic;
    }

    protected KafkaRequestResponseHandler() {

    }

    public KafkaRequestResponseHandler(
            LambdaExceptionUtil.Consumer_WithExceptions<Pair<String, Message>, IOException> sendOut
            , String responseTopic
            , String commonSendOut
    ) {
        this.sendOut = sendOut;
        this.responseTopic = responseTopic;
        this.commonSendOut = commonSendOut;
    }

    public void forward(String topic, String uri, Message msg) throws IOException {
        msg.setUri(uri);
        msg.setSourceId(msg.getSourceId() + "-" + this.sourceId);
        this.sendOut.accept(new Pair<>(topic, msg));
    }

    public Observable<Message> sendRequest(String topic, String uri, String srcId, Object content) throws IOException {
        return this.sendRequest(topic, uri, srcId, content, null);
    }

    public Observable<Message> sendRequest(String topic, String uri, String srcId, Object content, Integer timeout) throws IOException {
        int timeOut = timeout == null ? defaultTimeout : timeout;
        AsyncSubject subject = AsyncSubject.create();
        Message message = createMessage(MessageTypeEnum.REQUEST, uri, responseTopic, srcId, content);
        TimeoutData timeoutData = new TimeoutData(message.getMessageId(), timeOut);
        pendingRequests.put(message.getMessageId(), subject);
        timeoutChecks.put(message.getMessageId(), timeoutData);
        sendOut.accept(new Pair<>(this.getTopic(topic), message));
        this.checkTimeout(message.getMessageId(), timeout);
        return subject;
    }

    public CompletableFuture<Message> sendAsyncRequest(String topic, String uri, String srcId, Object content) {
        return this.sendAsyncRequest(topic, uri, srcId, content, null);
    }

    public CompletableFuture<Message> sendAsyncRequest(String topic, String uri, String srcId, Object content, Integer timeout) {
        int timeOut = timeout == null ? defaultTimeout : timeout;
        CompletableFuture<Message> future = new CompletableFuture<>();
        Message message = createMessage(MessageTypeEnum.REQUEST, uri, responseTopic, srcId, content);
        TimeoutData timeoutData = new TimeoutData(message.getMessageId(), timeOut);
        pendingAsyncRequests.put(message.getMessageId(), future);
        timeoutChecks.put(message.getMessageId(), timeoutData);
        try {
            sendOut.accept(new Pair<>(this.getTopic(topic), message));
            this.checkTimeout(message.getMessageId(), timeout, 2);
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public Message sendSyncRequest(String topic, String uri, String srcId, Object content) throws IOException, InterruptedException, ExecutionException {
        return this.sendSyncRequest(topic, uri, srcId, content, null);
    }

    public Message sendSyncRequest(String topic, String uri, String srcId, Object content, Integer timeout) throws IOException, InterruptedException, ExecutionException {
        FutureResult<Message> result = new FutureResult();
        AsyncSubject<Message> subject = AsyncSubject.create();
        subject.subscribe(result::setResult, result::setException);
        Message message = createMessage(MessageTypeEnum.REQUEST, uri, responseTopic, srcId, content);
        pendingSyncRequests.put(message.getMessageId(), subject);
        sendOut.accept(new Pair<>(this.getTopic(topic), message));
        this.checkTimeout(message.getMessageId(), timeout, 1);
        return result.getAndWait();
    }

    public Observable<Message> sendRequest(String topic, String uri, Object content) throws IOException {
        return this.sendRequest(topic, uri, this.sourceId, content);
    }

    public void sendRequestNoResponse(String topic, String uri, Object content) throws IOException {
        this.sendRequestNoResponse(topic, uri, this.sourceId, content);
    }

    public void sendRequestNoResponse(String topic, String uri, String sourceId, Object content) throws IOException {
        Message message = createMessage(MessageTypeEnum.REQUEST, uri, responseTopic, uri, sourceId, content, false);
        sendOut.accept(new Pair<>(this.getTopic(topic), message));
    }

    public void sendRequestNoResponseSafe(String topic, String uri, Object content) {
        try {
            this.sendRequestNoResponse(topic, uri, content);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public Message sendMessageSafe(String topic, String uri, Object content) {
        return this.sendMessageSafe(topic, uri, content, null);
    }

    public Message sendMessage(String topic, String uri, Object content) throws IOException {
        return this.sendMessage(topic, uri, content, null);
    }

    public Message sendMessage(String topic, String uri, String sourceId, Object content) throws IOException {
        return this.sendMessage(topic, uri, sourceId, content, null);
    }

    public Observable sendRequestSafe(String topic, String uri, Object content) {
        try {
            return this.sendRequest(topic, uri, content);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public void processMessage(Message message) {
        if (message.getMessageType() == MessageTypeEnum.RESPONSE) {
            if (this.pendingRequests.containsKey(message.getMessageId())) {
                response(message, this.pendingRequests);
            } else if (this.pendingSyncRequests.containsKey(message.getMessageId())) {
                response(message, this.pendingSyncRequests);
            } else if (this.pendingAsyncRequests.containsKey(message.getMessageId())) {
                responseAsync(message, this.pendingAsyncRequests);
            }
        }
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    private void responseAsync(Message message, ConcurrentHashMap<String, CompletableFuture<Message>> pendingRequests) {
        this.responseAsync(null, message, null, pendingRequests);
    }

    private void response(Message message, ConcurrentHashMap<String, Subject<Message, Message>> pendingRequests) {
        this.response(null, message, null, pendingRequests);
    }

    private void responseError(String messageId, Throwable e, ConcurrentHashMap<String, Subject<Message, Message>> pendingRequests) {
        this.response(messageId, null, e, pendingRequests);
    }

    private void responseErrorAsync(String messageId, Throwable e, ConcurrentHashMap<String, CompletableFuture<Message>> pendingRequests) {
        this.responseAsync(messageId, null, e, pendingRequests);
    }

    private void response(String messageId, Message message, Throwable e, ConcurrentHashMap<String, Subject<Message, Message>> pendingRequests) {
        String msgId = messageId == null ? message.getMessageId() : messageId;
        Subject<Message, Message> subject = pendingRequests.get(msgId);
        if (subject != null) {
            pendingRequests.remove(msgId);
            if (message != null) {
                subject.onNext(message);
                subject.onCompleted();
            } else {
                try {
                    subject.onError(e);
                } catch (OnErrorNotImplementedException err) {
                    subject.onNext(createMessage(MessageTypeEnum.RESPONSE, "", null, "ERROR_UNHANDLED", Response.fromException(e)));
                    subject.onCompleted();
                }
            }
        }
        TimeoutData timeoutData = timeoutChecks.get(msgId);
        if (timeoutData != null) {
            timeoutData.cancel();
            timeoutChecks.remove(msgId);
        }
    }

    private void responseAsync(String messageId, Message message, Throwable e, ConcurrentHashMap<String, CompletableFuture<Message>> pendingRequests) {
        String msgId = messageId == null ? message.getMessageId() : messageId;
        CompletableFuture<Message> subject = pendingRequests.get(msgId);
        if (subject != null) {
            pendingRequests.remove(msgId);
            if (message != null) {
                subject.complete(message);
            } else {
                subject.completeExceptionally(e);
            }
        }
        TimeoutData timeoutData = timeoutChecks.get(msgId);
        if (timeoutData != null) {
            timeoutData.cancel();
            timeoutChecks.remove(msgId);
        }
    }

    private void checkTimeout(String messageId, Integer timeout) {
        this.checkTimeout(messageId, timeout, 1);
    }

    private void checkTimeout(String messageId, Integer timeout, Integer mode) {
        int timeOut = timeout == null ? defaultTimeout : timeout;
        TimeoutData timeoutData = new TimeoutData(messageId, timeOut);
        if (mode == 1) {
            timeoutData.sync();
        } else if (mode == 2) {
            timeoutData.async();
        }
        timeoutChecks.put(messageId, timeoutData);
        this.timeoutTimer.schedule(timeoutData, timeOut);
    }


    private class TimeoutData extends TimerTask {
        boolean isSynchronization = false;
        boolean isAsync = false;
        String messageId;

        public TimeoutData(String messageId, int timeout) {
            this.messageId = messageId;
        }

        public TimeoutData sync() {
            this.isSynchronization = true;
            return this;
        }

        public TimeoutData async() {
            this.isAsync = true;
            return this;
        }

        @Override
        public void run() {
            if (isSynchronization) {
                responseError(messageId, new QueryTimeoutException(), pendingSyncRequests);
            } else if (isAsync) {
                responseErrorAsync(messageId, new QueryTimeoutException(), pendingAsyncRequests);
            } else {
                responseError(messageId, new QueryTimeoutException(), pendingRequests);
            }
        }
    }
}
