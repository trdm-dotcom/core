package com.homer.core.common.kafka.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.common.handler.KafkaRequestResponseHandler;
import com.homer.core.common.kafka.consumers.ConsumerHandler;
import com.homer.core.common.kafka.consumers.KafkaConsumer;
import com.homer.core.common.handler.KafkaRequestResponseHandler;
import com.homer.core.common.model.Message;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;

public class KafkaRequestSender extends KafkaRequestResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(KafkaRequestSender.class);

    private KafkaProducer<String, String> producer;
    private KafkaConsumer<String, String> consumer;
    private ConsumerHandler<String, String> handler;
    private boolean enableLogging = true;

    public KafkaRequestSender(ObjectMapper om, String kafkaServers, String clusterId, String nodeId) {
        super();
        this.init(om, kafkaServers, clusterId, nodeId, false, null, true, null);
    }

    public KafkaRequestSender(ObjectMapper om, String kafkaServers, String clusterId, String nodeId, boolean enableIdempotence) {
        super();
        this.init(om, kafkaServers, clusterId, nodeId, enableIdempotence, null, true, null);
    }

    public KafkaRequestSender(ObjectMapper om, String kafkaServers, String clusterId, String nodeId, boolean enableIdempotence, Properties producerProperties, boolean createConsumer) {
        super();
        this.init(om, kafkaServers, clusterId, nodeId, enableIdempotence, producer, createConsumer, producerProperties);
    }

    public KafkaRequestSender(ObjectMapper om, String kafkaServers, String clusterId, String nodeId, boolean enableIdempotence, KafkaProducer<String, String> producer, boolean createConsumer) {
        super();
        this.init(om, kafkaServers, clusterId, nodeId, enableIdempotence, producer, createConsumer, null);
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    private void init(
            ObjectMapper om,
            String kafkaServers,
            String clusterId,
            String nodeId,
            boolean enableIdempotence,
            KafkaProducer<String, String> producer,
            boolean createConsumer,
            Properties producerProperties
    ) {
        this.producer = producer != null ? producer : new KafkaProducer<>(kafkaServers, null,
                producerProperties == null ? new Properties(): producerProperties, enableIdempotence);
        this.responseTopic = clusterId + "." + nodeId;
        this.sendOut = pair -> {
            Message msg = pair.getRight();
            String msgKey = msg.getMessageKey();
            if (msgKey == null) {
                msgKey = this.getMessageKey() + "";
            }
            String msgString = om.writeValueAsString(msg);
            if (msg.isLog() || this.enableLogging) {
                log.warn("send {} with data '{}'", pair.getLeft(), msgString);
            }
            this.producer.send(new ProducerRecord<>(pair.getLeft(), msgKey, msgString));
        };
        this.handler = record -> {
            if (this.enableLogging) {
                log.info("receive response: {}", record);
            }
            Message msg;
            try {
                msg = om.readValue(record.value(), Message.class);
            } catch (Exception e) {
                log.error("fail to parse response: {}", record.value());
                return;
            }
            try {
                KafkaRequestSender.super.processMessage(msg);
            } catch (Exception e) {
                log.error("fail to handle response: {}", msg);
            }
        };
        if (createConsumer) {
            this.consumer = new KafkaConsumer<>(kafkaServers, this.responseTopic, Arrays.asList(this.responseTopic), new Properties(), handler);
            new Thread(this.consumer).start();
        }
    }

    public ConsumerHandler<String, String> getHandler() {
        return this.handler;
    }

    public void stop() {
        if (this.consumer != null) {
            this.consumer.stop();
        }
    }
}
