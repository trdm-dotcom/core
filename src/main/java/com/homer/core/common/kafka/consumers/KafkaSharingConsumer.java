package com.homer.core.common.kafka.consumers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class KafkaSharingConsumer<K, V> extends ThreadedKafkaConsumer<K, V> {

    private String bootStrapServer;
    private String groupId;
    private Properties properties;
    private Map<String, ConsumerHandler<K, V>> topics;
    protected ConsumerHandler<K, V> handler;
    private int maxThread;

    public KafkaSharingConsumer(String bootStrapServer, String groupId, Properties properties, Map<String, ConsumerHandler<K, V>> topics, int maxThread) {
        super();
        this.bootStrapServer = bootStrapServer;
        this.groupId = groupId;
        this.properties = properties;
        this.maxThread = maxThread;
        this.topics = topics == null ? new HashMap<>() : topics;
    }

    public void addTopic(String topic, ConsumerHandler<K, V> handler) {
        this.topics.put(topic, handler);
    }

    public void init() {
        this.createHandler();
        super.init(bootStrapServer, groupId, topics.keySet(), properties, this.handler, this.maxThread);
    }

    protected void createHandler() {
        this.handler = record -> {
            String topic = record.topic();
            ConsumerHandler<K, V> handler = topics.get(topic);
            handler.handle(record);
        };
    }
}
