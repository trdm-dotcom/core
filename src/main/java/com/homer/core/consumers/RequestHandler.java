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
import com.homer.core.model.request.AddressRequest;
import com.homer.core.services.AddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rx.Observable;

@Service
@Slf4j
public class RequestHandler extends KafkaRequestHandler {
    private final ObjectMapper objectMapper;
    private final AddressService addressService;

    public RequestHandler(
            ObjectMapper objectMapper,
            AppConf appConf,
            AddressService addressService
    ) {
        super(objectMapper, appConf.getKafkaBootstraps(), appConf.getClusterId(), appConf.getMaxThread());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        objectMapper.coercionConfigFor(LogicalType.Enum).setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        this.addressService = addressService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected Object handle(Message message) {
        try {
            log.info("message: {}", message);
            switch (message.getUri()) {
                case "get:/api/v1/core/cities":
                    return this.addressService.getCities(message.getTransactionId());
                case "get:/api/v1/core/district":
                    AddressRequest districtRequest = Message.getData(this.objectMapper, message, AddressRequest.class);
                    return this.addressService.getDistrictsByCity(districtRequest, message.getTransactionId());
                case "get:/api/v1/core/commune":
                    AddressRequest communeRequest = Message.getData(this.objectMapper, message, AddressRequest.class);
                    return this.addressService.getCommuneByDistrict(communeRequest, message.getTransactionId());

            }
            return true;
        } catch (IllegalArgumentException e) {
            return Observable.error(new GeneralException(e.getMessage()));
        } catch (Exception e) {
            log.error("Error: ", e);
            return Observable.error(e);
        }
    }
}
