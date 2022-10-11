package com.homer.core.common.kafka.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.common.kafka.consumers.KafkaSharingConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class KafkaRequestHandlerAndSender extends KafkaRequestHandler {
    private KafkaRequestSender requestSender;
    private KafkaSharingConsumer<String, String> consumer;

    protected KafkaRequestHandlerAndSender() {

    }

    public KafkaRequestHandlerAndSender(ObjectMapper om, String bootStrapServer, String clusterId, String nodeId, int maxThread, boolean enableIdempotence) {
        List<String> requestHandlerTopics = new ArrayList<>();
        requestHandlerTopics.add(clusterId);
        this.init(om, bootStrapServer, clusterId, nodeId, requestHandlerTopics, maxThread, enableIdempotence);
    }

    public KafkaRequestHandlerAndSender(ObjectMapper om, String bootStrapServer, String clusterId, String nodeId, List<String> requestHandlerTopic, int maxThread, boolean enableIdempotence) {
        this.init(om, bootStrapServer, clusterId, nodeId, requestHandlerTopic, maxThread, enableIdempotence);
    }

    protected void init(ObjectMapper om, String bootStrapServer, String clusterId, String nodeId, List<String> requestHandlerTopic, int maxThread, boolean enableIdempotence) {
        this.init(om, bootStrapServer, clusterId, requestHandlerTopic, maxThread, new KafkaProducer<>(bootStrapServer, clusterId, new Properties(), enableIdempotence), false);
        this.requestSender = new KafkaRequestSender(om, bootStrapServer, clusterId, nodeId, enableIdempotence, this.producer, false);
        this.consumer = new KafkaSharingConsumer<>(bootStrapServer, clusterId, new Properties(), null, maxThread);
        requestHandlerTopic.forEach(topic -> this.consumer.addTopic(topic, this.handler));
        this.consumer.addTopic(requestSender.getResponseTopic(), this.requestSender.getHandler());
        this.consumer.init();
    }

    public KafkaRequestSender sender() {
        return requestSender;
    }
}
