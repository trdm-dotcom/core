package com.homer.core.common.kafka.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.common.handler.BaseRequestHandler;
import com.homer.core.common.kafka.producers.KafkaProducer;
import com.homer.core.common.kafka.producers.RequestHandlerMessage;
import com.homer.core.common.kafka.producers.RequestHandlerMessageDeserializer;
import com.homer.core.common.model.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.*;
import java.util.function.BiFunction;

@Slf4j
public abstract class KafkaMessageRequestHandler extends BaseRequestHandler<RequestHandlerMessage> {
    protected KafkaProducer<String, String> producer;
    protected ThreadedKafkaConsumer<String, String> consumer;
    protected ConsumerHandler<String, String> handler;
    protected Map<String, Controller> controllerMap;


    public KafkaMessageRequestHandler(ObjectMapper om, String bootStrapServer, String clusterId, int maxThread) {
        super(null, null);
        this.init(om, bootStrapServer, clusterId, null, maxThread, null, true);
    }

    public KafkaMessageRequestHandler(ObjectMapper om, String bootStrapServer, String clusterId, List<String> topics, int maxThread, KafkaProducer<String, String> producer) {
        super(null, null);
        this.init(om, bootStrapServer, clusterId, topics, maxThread, producer, true);
    }

    public KafkaMessageRequestHandler(ObjectMapper om, String bootStrapServer, String clusterId, List<String> topics, int maxThread, KafkaProducer<String, String> producer, boolean createConsumer) {
        super(null, null);
        this.init(om, bootStrapServer, clusterId, topics, maxThread, producer, createConsumer);
    }

    protected void setControllerMap(Map<String, Controller> controllerMap) {
        this.controllerMap = controllerMap;
        Map<String, Class> classMap = new HashMap<>();
        this.controllerMap.forEach((k, v) -> classMap.put(k, v.getClazz()));
        RequestHandlerMessageDeserializer.classMap = classMap;

    }

    protected void init(ObjectMapper om, String bootStrapServer, String clusterId, List<String> topics, int maxThread, KafkaProducer<String, String> producer, boolean createConsumer) {
        RequestHandlerMessageDeserializer.objectMapper = om;
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
                RequestHandlerMessage msg = om.readValue(record.value(), RequestHandlerMessage.class);
                KafkaMessageRequestHandler.super.process(msg);
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

    @Override
    protected Object handle(RequestHandlerMessage message) {
        String requestId = message.getTransactionId() + "::" + message.getMessageId();
        RequestContext requestContext = new RequestContext(requestId, message);
        Controller controller = this.controllerMap.get(message.getUri());
        if (controller != null) {
            return controller.getController().apply(message.getData(), requestContext);
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    public static class Controller<T> {
        private Class<T> clazz;
        private BiFunction<T, RequestContext<T>, Object> controller;
    }

    @Data
    @AllArgsConstructor
    public static class RequestContext<T> {
        private String id;
        private RequestHandlerMessage<T> origin;
    }
}
