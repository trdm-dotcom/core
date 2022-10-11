package com.homer.core.common.kafka.consumers;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadedKafkaConsumer<K, V> implements ConsumerHandler<K, V> {
    private final static Logger log = LoggerFactory.getLogger(ThreadedKafkaConsumer.class);

    private KafkaConsumer<K, V> consumer;
    private ConsumerHandler<K, V> handler;
    private BlockingQueue<ConsumerRecord<K, V>> queue;
    private int maxThread;
    private Thread permanentThread;
    private AtomicInteger totalThread = new AtomicInteger(0);
    private AtomicInteger numberOfProcessingRecord = new AtomicInteger(0);

    protected ThreadedKafkaConsumer() {

    }

    public ThreadedKafkaConsumer(String bootStrapServer, String groupId, List<String> topics, Properties properties, ConsumerHandler<K, V> handler, int maxThread) {
        this.init(bootStrapServer, groupId, topics, properties, handler, maxThread);
    }

    protected void init(String bootStrapServer, String groupId, Collection<String> topics, Properties properties, ConsumerHandler<K, V> handler, int maxThread) {
        this.queue = new LinkedBlockingQueue<>();
        this.handler = handler;
        this.consumer = new KafkaConsumer<>(bootStrapServer, groupId, topics, properties, this);
        this.maxThread = maxThread;
        new Thread(consumer).start();
    }

    @Override
    public void handle(ConsumerRecord<K, V> record) {
        if (this.numberOfProcessingRecord.get() >= totalThread.get() && totalThread.get() < maxThread) {
            boolean isPermanent = permanentThread == null;
            Thread thread = new Thread(new HandleWorker<>(this, isPermanent));
            if (isPermanent) {
                this.permanentThread = thread;
            }
            thread.start();
            totalThread.incrementAndGet();
        }
        try {
            this.queue.put(record);
        } catch (Exception e) {
            log.error("fail to put record to queue", e);
        }
    }

    public static class HandleWorker<K, V> implements Runnable {
        private ThreadedKafkaConsumer<K, V> consumer;
        private ConsumerRecord<K, V> consumerRecord;
        private boolean isPermanent;

        public HandleWorker(ThreadedKafkaConsumer<K, V> consumer, boolean isPermanent) {
            this.consumer = consumer;
            this.isPermanent = isPermanent;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    this.consumerRecord = this.consumer.queue.poll(1000, TimeUnit.MILLISECONDS);
                    if (consumerRecord != null) {
                        this.consumer.numberOfProcessingRecord.incrementAndGet();
                        try {
                            this.consumer.handler.handle(consumerRecord);
                        } catch (Exception e) {
                            log.error("fail to handle record {} {}", consumerRecord, this.consumer.handler, e);
                        }
                        this.consumer.numberOfProcessingRecord.decrementAndGet();
                        continue;
                    }
                } catch (Exception e) {
                    log.error("fail to poll record from queue", e);
                }
                if (!this.isPermanent) {
                    this.consumer.totalThread.decrementAndGet();
                    return;
                }
            }
        }
    }

    public void stop() {
        this.consumer.stop();
    }


    public boolean isMarkAsStop() {
        return this.consumer.isMarkAsStop();
    }

    public boolean isRealStop() {
        return this.consumer.isRealStop();
    }
}
