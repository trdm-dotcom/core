package com.homer.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.model.Message;
import com.homer.core.common.model.Response;
import com.homer.core.common.redis.RedisDao;
import com.homer.core.configurations.AppConf;
import com.homer.core.model.response.UserInfo;
import com.homer.core.services.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
@Slf4j
public class Utils {
    private static ObjectMapper objectMapper = null;
    private static RedisDao redisDao = null;
    private static KafkaProducerService kafkaProducerService = null;
    private static AppConf appConf = null;

    @Autowired
    public Utils(
            ObjectMapper objectMapper,
            RedisDao redisDao,
            KafkaProducerService kafkaProducerService,
            AppConf appConf
    ) {
        this.redisDao = redisDao;
        this.objectMapper = objectMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.appConf = appConf;
    }

    public static CompletableFuture<UserInfo> getUserInfo(String msgId, String userId){
        Map<String, String> request = new HashMap<String, String>() {{
            put("id", userId);
        }};
        CompletableFuture<Message> future = kafkaProducerService.sendAsyncRequest(appConf.getTopics().getUserInfo(), null, appConf.getClusterId(), request);
        try{
            Map<String, Object> loginResult = new HashMap<>();
            Message message = future.get();
            Response response = Message.getData(objectMapper, message, Response.class);
            if (response.getStatus() != null) {
                throw new RuntimeException(response.getStatus().getCode());
            }
            log.info("{} aaa response data {}", msgId, response.getData());
            return CompletableFuture.completedFuture(objectMapper.convertValue(response.getData(), UserInfo.class));
        }catch (Exception e){
            throw new GeneralException();
        }
    }
}
