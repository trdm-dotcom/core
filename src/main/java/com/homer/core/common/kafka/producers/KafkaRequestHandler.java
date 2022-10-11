package com.homer.core.common.kafka.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.common.handler.BaseRequestHandler;
import com.homer.core.common.kafka.consumers.ConsumerHandler;
import com.homer.core.common.kafka.consumers.ThreadedKafkaConsumer;
import com.homer.core.common.model.Message;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public abstract class KafkaRequestHandler extends BaseRequestHandler<Message> {
    private static final Logger log = LoggerFactory.getLogger(KafkaRequestHandler.class);

    protected KafkaProducer<String, String> producer;
    protected ThreadedKafkaConsumer<String, String> consumer;
    protected ConsumerHandler<String, String> handler;

    protected KafkaRequestHandler() {

    }

    public KafkaRequestHandler(ObjectMapper om, String bootStrapServer, String clusterId, int maxThread) {
        super(null, null);
        this.init(om, bootStrapServer, clusterId, null, maxThread, null, true);
    }

    public KafkaRequestHandler(ObjectMapper om, String bootStrapServer, String clusterId, List<String> topics, int maxThread, KafkaProducer<String, String> producer) {
        super(null, null);
        this.init(om, bootStrapServer, clusterId, topics, maxThread, producer, true);
    }

    public KafkaRequestHandler(ObjectMapper om, String bootStrapServer, String clusterId, List<String> topics, int maxThread, KafkaProducer<String, String> producer, boolean createConsumer) {
        super(null, null);
        this.init(om, bootStrapServer, clusterId, topics, maxThread, producer, createConsumer);
    }

    protected void init(ObjectMapper om, String bootStrapServer, String clusterId, List<String> topics, int maxThread, KafkaProducer<String, String> producer, boolean createConsumer) {
        this.producer = producer == null ? new KafkaProducer<>(bootStrapServer, null, new Properties()) : producer;
        this.sendOut = pair -> {
            Message msg = pair.getRight();
            String messageKey = msg.getMessageKey();
            if (messageKey == null) {
                messageKey = this.getMessageKey() + "";
            }
            String responseMsg = om.writeValueAsString(pair.getRight());
            log.info("response msg: {} with key {}", responseMsg, messageKey);
            this.producer.send(new ProducerRecord<>(pair.getLeft(), messageKey, responseMsg));
        };
        this.handler = record -> {
            log.info("receive msg: {} with key {}", record.value(), record.key());
            try {
                Message msg = om.readValue(record.value(), Message.class);
                KafkaRequestHandler.super.process(msg);
            } catch (Exception e) {
                log.error("fail to parse message {}", record, e);
            }
        };
        List<String> consumeTopics = topics == null ? Arrays.asList(clusterId) : topics;
        if (createConsumer) {
            this.consumer = new ThreadedKafkaConsumer<>(bootStrapServer, clusterId, consumeTopics, new Properties(), handler, maxThread);
        }
    }

    public ConsumerHandler<String, String> getHandler() {
        return handler;
    }

    public void stop() {
        if (this.consumer != null) {
            this.consumer.stop();
        }
    }
}
