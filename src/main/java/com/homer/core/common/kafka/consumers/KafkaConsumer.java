package com.homer.core.common.kafka.consumers;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collection;
import java.util.Properties;

@Slf4j
public class KafkaConsumer<K, V> implements Runnable{
    protected Consumer<K, V> consumer;
    protected ConsumerHandler<K, V> handler;
    private boolean markAsStop = false;
    private boolean realStop = false;
    private String key;

    public KafkaConsumer(String bootStrapServer, String groupId, Collection<String> topics, Properties properties, ConsumerHandler<K, V> handler){
        this.init(bootStrapServer, groupId, topics, properties, handler);
    }

    private void init(String bootStrapServer, String groupId, Collection<String> topics, Properties properties, ConsumerHandler<K, V> handler){
        this.handler = handler;
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.putAll(properties);
        this.key = bootStrapServer + "::" + topics.toString();
        log.warn("init consume kafka {} _ group {}", this.key, groupId);
        this.consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<K, V>(props);
        this.consumer.subscribe(topics);
    }

    @Override
    public void run() {
        log.warn("start consume kafka {}", this.key);
        while (true) {
            if (this.markAsStop) {
                this.realStop = true;
                break;
            }
            ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<K, V> record : records) {
                handler.handle(record);
            }
        }
    }

    public void stop() {
        this.markAsStop = true;
    }

    public boolean isMarkAsStop() {
        return markAsStop;
    }

    public boolean isRealStop() {
        return realStop;
    }
}
