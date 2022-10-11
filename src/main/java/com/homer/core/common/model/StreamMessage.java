package com.homer.core.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class StreamMessage<T> extends Message<List<T>> {
    @JsonIgnore
    private String messageKey;
    @JsonIgnore
    private String partitionKey;

    public static <T> StreamMessage<T> create(String uri, String sourceId) {
        return create(uri, sourceId, 0);
    }

    private static <T> StreamMessage<T> create(String uri, String sourceId, int index) {
        StreamMessage<T> message = new StreamMessage<>();
        update(message, uri, sourceId, new ArrayList<>());
        message.setStream(true);
        message.setStreamIndex(index);
        return message;
    }

    public StreamMessage<T> add(T element) {
        if (element != null && messageKey == null) {
            if (element instanceof Body) {
                messageKey = ((Body) element).getMessageKey();
                partitionKey = ((Body) element).getPartitionKey();
            } else {
                messageKey = Math.random() + "";
                partitionKey = messageKey;
            }
        }
        this.data.add(element);
        return this;
    }

    public StreamMessage<T> end() {
        this.setStreamState(StreamState.FINSISH);
        return this;
    }

    public StreamMessage<T> endWithError() {
        this.setStreamState(StreamState.ERROR);
        return this;
    }

    public StreamMessage<T> split() {
        return create(this.uri, this.sourceId, this.streamIndex + 1);
    }

    public StreamMessage<T> split(T element) {
        return this.split().add(element);
    }

    public int size() {
        return this.data.size();
    }


    @JsonIgnore
    public String getPartitionKey() {
        return this.partitionKey;
    }

    @JsonIgnore
    public String getMessageKey() {
        return this.messageKey;
    }
}
