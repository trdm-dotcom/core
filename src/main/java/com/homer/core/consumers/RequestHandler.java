package com.homer.core.consumers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.kafka.producers.KafkaRequestHandler;
import com.homer.core.common.model.Message;
import com.homer.core.configurations.AppConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rx.Observable;

@Service
@Slf4j
public class RequestHandler extends KafkaRequestHandler {
    private final ObjectMapper objectMapper;

    public RequestHandler(
            ObjectMapper objectMapper,
            AppConf appConf
    ) {
        super(objectMapper, appConf.getKafkaBootstraps(), appConf.getClusterId(), appConf.getMaxThread());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        objectMapper.coercionConfigFor(LogicalType.Enum).setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);

        this.objectMapper = objectMapper;
    }

    @Override
    protected Object handle(Message message) {
        try {
            log.info("message: {}", message);
            return true;
        }
        catch (IllegalArgumentException e) {
            return Observable.error( new GeneralException(e.getMessage()));
        } catch (Exception e) {
            log.error("Error: ", e);
            return Observable.error(e);
        }
    }
}
