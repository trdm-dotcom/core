package com.homer.core.common.kafka.producers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.homer.core.common.model.Message;
import lombok.Data;
import lombok.ToString;

@JsonDeserialize(using = RequestHandlerMessageDeserializer.class)
@Data
@ToString(callSuper = true)
public class RequestHandlerMessage<T> extends Message<T> {
}
