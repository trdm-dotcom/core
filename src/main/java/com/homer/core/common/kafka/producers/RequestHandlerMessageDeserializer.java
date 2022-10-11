package com.homer.core.common.kafka.producers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.homer.core.common.model.MessageTypeEnum;
import com.homer.core.common.model.ResponseDestination;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestHandlerMessageDeserializer  extends StdDeserializer<RequestHandlerMessage> {
    public static Map<String, Class> classMap = new HashMap<>();
    public static ObjectMapper objectMapper;

    protected RequestHandlerMessageDeserializer() {
        super(RequestHandlerMessage.class);
    }

    @Override
    public RequestHandlerMessage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        jsonParser.nextToken();
        RequestHandlerMessage message = new RequestHandlerMessage();
        String currentFieldName = null;
        JsonToken currentToken = jsonParser.nextToken();
        boolean setData = false;

        while (!jsonParser.isClosed()) {
            if (currentToken == JsonToken.FIELD_NAME) {
                currentFieldName = jsonParser.getCurrentName();
                jsonParser.nextToken();
                if ("messageType".equals(currentFieldName)) {
                    message.setMessageType(MessageTypeEnum.valueOf(this._parseString(jsonParser, deserializationContext)));
                } else if ("sourceId".equals(currentFieldName)) {
                    message.setSourceId(this._parseString(jsonParser, deserializationContext));
                } else if ("transactionId".equals(currentFieldName)) {
                    message.setTransactionId(this._parseString(jsonParser, deserializationContext));
                } else if ("messageId".equals(currentFieldName)) {
                    message.setMessageId(this._parseString(jsonParser, deserializationContext));
                } else if ("uri".equals(currentFieldName)) {
                    message.setUri(this._parseString(jsonParser, deserializationContext));
                    if (setData) {
                        Class clazz = classMap.get(message.getUri());
                        if (clazz != null) {
                            message.setData(objectMapper.readValue(objectMapper.writeValueAsString(message.getData()), clazz));
                        } else {
                            throw new RuntimeException("no mapping for uri: " + message.getUri());
                        }
                    }
                } else if ("responseDestination".equals(currentFieldName)) {
                    message.setResponseDestination(deserializationContext.readValue(jsonParser, ResponseDestination.class));
                } else if ("t".equals(currentFieldName)) {
                    message.setT(this._parseLongPrimitive(jsonParser, deserializationContext));
                } else if ("data".equals(currentFieldName)) {
                    if (message.getUri() != null) {
                        Class clazz = classMap.get(message.getUri());
                        if (clazz != null) {
                            message.setData(deserializationContext.readValue(jsonParser, clazz));
                        } else {
                            throw new RuntimeException("no mapping for uri: " + message.getUri());
                        }
                    } else {
                        setData = true;
                        message.setData(deserializationContext.readValue(jsonParser, Object.class));
                    }
                }
            }
            currentToken = jsonParser.nextToken();
        }
        return message;
    }
}
