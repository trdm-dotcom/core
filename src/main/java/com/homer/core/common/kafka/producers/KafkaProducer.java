package com.homer.core.common.kafka.producers;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.Future;

@Slf4j
public class KafkaProducer<K, V> {
    private org.apache.kafka.clients.producer.KafkaProducer<K, V> producer;

    public KafkaProducer(String bootStrapServer, String client, Properties properties) {
        this.init(bootStrapServer, client, properties, false);
    }

    public KafkaProducer(String bootStrapServer, String client, Properties properties, boolean enableIdempotence) {
        this.init(bootStrapServer, client, properties, enableIdempotence);
    }

    private void init(String bootStrapServer, String clientId, Properties properties, boolean enableIdempotence) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId != null ? clientId : "" + System.currentTimeMillis());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "0");
        props.putAll(properties);
        if (enableIdempotence) {
            props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
            props.put(ProducerConfig.ACKS_CONFIG, "-1");
        }
        log.warn("init producer {}", bootStrapServer);
        this.producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);
    }

    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        return this.producer.send(record);
    }
}
