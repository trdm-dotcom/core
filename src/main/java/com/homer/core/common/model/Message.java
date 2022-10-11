package com.homer.core.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.common.handler.KafkaSendOutHandler;
import lombok.Data;

import java.io.IOException;

@Data
public class Message<T extends Object> {
    protected MessageTypeEnum messageType;
    protected String sourceId;
    protected String transactionId;
    protected String messageId;
    protected String uri;
    protected ResponseDestination responseDestination;
    protected T data;
    protected Long t; // time that message is sent
    protected Boolean stream;
    protected StreamState streamState;
    protected Integer streamIndex;

    @JsonIgnore
    protected boolean log;

    public Message<T> log() {
        this.log = true;
        return this;
    }

    public static <E> E getData(ObjectMapper objectMapper, Message message, Class<E> clazz) throws IOException {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(message.getData()), clazz);
    }

    public static <E> E getData(ObjectMapper objectMapper, Message message, TypeReference<E> typeReference) throws IOException {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(message.getData()), typeReference);
    }


    public <E> E getData(ObjectMapper objectMapper, Class<E> clazz) throws IOException {
        return Message.getData(objectMapper, this, clazz);
    }

    public <E> E getData(ObjectMapper objectMapper, TypeReference<E> typeReference) throws IOException {
        return Message.getData(objectMapper, this, typeReference);
    }

    protected static <T> void update(Message<T> message, String uri, String sourceId, T data) {
        message.setMessageType(MessageTypeEnum.MESSAGE);
        message.setSourceId(sourceId);
        message.setMessageId(String.valueOf(KafkaSendOutHandler.messageId.incrementAndGet()));
        message.setUri(uri);
        message.setResponseDestination(null);
        message.setT(System.currentTimeMillis());
        message.setData(data);
    }

    public static Message create(String uri, String sourceId, Object data) {
        Message message = new Message();
        update(message, uri, sourceId, data);
        return message;
    }

    @JsonIgnore
    public String getPartitionKey() {
        if (this.data instanceof Body) {
            return ((Body) this.data).getPartitionKey();
        }
        return null;
    }

    @JsonIgnore
    public String getMessageKey() {
        if (this.data instanceof Body) {
            return ((Body) this.data).getMessageKey();
        }
        return null;
    }

    public enum StreamState {
        NORMAL, FINSISH, ERROR;
    }
}
