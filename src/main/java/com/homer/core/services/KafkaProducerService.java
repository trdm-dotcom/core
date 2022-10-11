package com.homer.core.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.common.kafka.producers.KafkaRequestSender;
import com.homer.core.configurations.AppConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducerService extends KafkaRequestSender {

    @Autowired
    public KafkaProducerService(ObjectMapper objectMapper, AppConf appConf) {
        super(objectMapper, appConf.getKafkaBootstraps(), appConf.getClusterId(), "1", false);
    }
}
