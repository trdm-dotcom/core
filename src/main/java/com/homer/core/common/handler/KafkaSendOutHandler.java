package com.homer.core.common.handler;

import com.homer.core.common.model.Message;
import com.homer.core.common.model.MessageTypeEnum;
import com.homer.core.common.model.ResponseDestination;
import com.homer.core.common.model.StreamMessage;
import com.homer.core.common.utils.LambdaExceptionUtil;
import com.homer.core.common.utils.Pair;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import rx.subjects.Subject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class KafkaSendOutHandler {
    public static final AtomicLong messageId = new AtomicLong(System.currentTimeMillis());
    private static final Logger log = LoggerFactory.getLogger(KafkaSendOutHandler.class);
    private static final Integer MAX_MESSAGE_KEY = Integer.MAX_VALUE;


    protected ConcurrentHashMap<String, Subject<Message, Message>> pendingRequests = new ConcurrentHashMap<>();
    protected AtomicInteger messageKey = new AtomicInteger(0);
    @Getter
    protected LambdaExceptionUtil.Consumer_WithExceptions<Pair<String, Message>, IOException> sendOut;
    @Setter
    protected String sourceId;
    @Getter
    protected String commonSendOut;

    protected int getMessageKey() {
        return this.messageKey.getAndUpdate(current -> current == MAX_MESSAGE_KEY ? 0 : current + 1);
    }

    public <T> void sendStream(String topic, String uri, Stream<T> content) throws IOException {
        this.sendStream(topic, uri, content, 50);
    }

    public <T> void sendStream(String topic, String uri, Stream<T> content, int numberOfItemPerPackage) throws IOException {
        AtomicInteger index = new AtomicInteger(0);
        StreamMessage<T> message = StreamMessage.create(uri, this.sourceId);
        Iterator<T> it = content.iterator();
        try {
            while (it.hasNext()) {
                if (index.incrementAndGet() % numberOfItemPerPackage == 0) {
                    sendOut.accept(new Pair<>(this.getTopic(topic), message));
                    message = message.split();
                }
                message.add(it.next());
            }
        } catch (Exception e) {
            message.setStreamState(Message.StreamState.ERROR);
        }
        if (message.getStreamState() != Message.StreamState.ERROR) {
            message.end();
        }
        sendOut.accept(new Pair<>(this.getTopic(topic), message.end()));
    }

    public Message sendMessageSafe(String topic, String uri, Object content, String responseTopic) {
        try {
            return sendMessage(topic, uri, content, responseTopic);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public Message sendMessageSafeAndLog(String topic, String uri, Object content) {
        try {
            return sendMessage(topic, uri, sourceId, content, null, true);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public void sendMiniMessageSafeNoResponse(String topic, String uri, Object content) {
        try {
            Message message = createMessage(null, uri, null, null, sourceId, content, false, null);
            sendOut.accept(new Pair<>(this.getTopic(topic), message));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void sendMessageNoResponse(String topic, String uri, Object content) throws IOException {
        Message message = createMessage(MessageTypeEnum.MESSAGE, uri, null, null, sourceId, content, false);
        sendOut.accept(new Pair<>(this.getTopic(topic), message));
    }

    public Message sendMessage(String topic, String uri, Object content, String responseTopic) throws IOException {
        return this.sendMessage(topic, uri, this.sourceId, content, responseTopic);
    }

    public Message sendMessage(String topic, String uri, String sourceId, Object content, String responseTopic) throws IOException {
        Message message = createMessage(MessageTypeEnum.MESSAGE, uri, responseTopic, sourceId, content);
        sendOut.accept(new Pair<>(this.getTopic(topic), message));
        return message;
    }

    public Message sendMessage(String topic, String uri, String sourceId, Object content, String responseTopic, boolean log) throws IOException {
        Message message = createMessage(MessageTypeEnum.MESSAGE, uri, responseTopic, sourceId, content);
        if (log) {
            message.log();
        }
        sendOut.accept(new Pair<>(this.getTopic(topic), message));
        return message;
    }

    protected Message createMessage(MessageTypeEnum type, String uri, String responseTopic, String sourceId, Object content) {
        return createMessage(type, uri, responseTopic, uri, sourceId, content, true);
    }

    protected Message createMessage(MessageTypeEnum type, String uri, String responseTopic
            , String responseUri, String sourceId, Object content, boolean requireResponse) {
        Message message = new Message();
        message.setData(content);
        message.setUri(uri);
        message.setMessageType(type);
        message.setSourceId(sourceId);
        if (requireResponse && responseTopic != null && !responseTopic.isEmpty()) {
            message.setResponseDestination(new ResponseDestination(responseTopic, responseUri));
        }
        message.setMessageId(String.valueOf(messageId.incrementAndGet()));
        return message;
    }

    protected Message createMessage(MessageTypeEnum type, String uri, String responseTopic
            , String responseUri, String sourceId, Object content, boolean requireResponse, String messageId) {
        Message message = new Message();
        message.setData(content);
        message.setMessageType(type);
        message.setUri(uri);
        message.setSourceId(sourceId);
        if (requireResponse) {
            message.setResponseDestination(new ResponseDestination(responseTopic, responseUri));
        }
        message.setMessageId(messageId);
        return message;
    }

    public String getTopic(String topic) {
        if (StringUtils.isEmpty(commonSendOut)) {
            return topic;
        }
        return commonSendOut.replace("{topic}", topic);
    }


    /**
     * see {@link https://www.quora.com/In-Kafka-when-a-producer-publishes-multiple-messages-to-the-same-topic-do-they-go-to-the-same-partitions}
     * <p>
     * Let’s assume you have created a topic with 3 partitions (could be N).
     * First, let me stress that you SHOULDN’T have to specify the partition you send data to, only the topic. All that magic is handled by the Kafka Client for a very good reason. Let’s consider a few use cases.
     * if you publish multiple messages without providing any key, then the messages are randomly assigned to the partitions, therefore they are pretty even.
     * If you publish a key with your message (think for example, user_id if your topic is about users), then the key will be hashed, and the messages that have the same key will always go to the same partition (this really helps for partial ordering)
     * If your key is diverse enough (a lot of values), then again, you end up with even partitions.
     * If your key is not diverse (think only a few values), then you end up with very uneven partitions
     * In the case you really want to have control over where your keys go, you can write our own Partitioner. I don’t recommend this unless you know what you’re doing.
     * </p>
     * <p>
     * if messages have the same message key, then they are forwarded to the same partition
     * and send to node which is connect to the same partition.
     * If messages have the same partition key, then they are forwarded to very specific partition??? not sure
     * . need to research more
     * </p>
     * For now: no need partition key yet.
     * <ul>
     * <li>implement DefaultPartitionKey and override method getMessageKey
     * , if you want to control al message from the same category cme to a processor</li>
     * <li>if you don't this class gonna assign a round robin number for message key</li>
     * </ul>
     *
     * @param partitionKey
     * @param messageKey
     * @param message
     * @return
     */
    public Map<String, Object> getHeader(String partitionKey, String messageKey, Message message) {
        Map<String, Object> header = new HashMap<>();
        String pKey = message.getPartitionKey();
        String mKey = message.getMessageKey();
        if (mKey == null) {
            mKey = String.valueOf(getMessageKey());
        }
        header.put(messageKey, mKey);
        if (pKey != null && !pKey.isEmpty()) {
            header.put(partitionKey, pKey);
        }
        return header;
    }
}
