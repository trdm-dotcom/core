package com.homer.core.consumers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.homer.core.common.kafka.producers.KafkaRequestHandler;
import com.homer.core.common.model.Message;
import com.homer.core.common.redis.RedisDao;
import com.homer.core.configurations.AppConf;
import com.homer.core.model.CanalBean;
import com.homer.core.model.RedisType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rx.Observable;

@Slf4j
@Service
public class SyncRedisMysqlHandler extends KafkaRequestHandler {
    private final ObjectMapper objectMapper;

    private final RedisDao redisDao;

    public SyncRedisMysqlHandler(
            ObjectMapper objectMapper,
            AppConf appConf,
            RedisDao redisDao
    ) {
        super(objectMapper, appConf.getKafkaBootstraps(), appConf.getTopics().getSyncRedisMysql(), 4);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        objectMapper.coercionConfigFor(LogicalType.Enum).setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        this.objectMapper = objectMapper;
        this.redisDao = redisDao;
    }

    @Override
    protected Object handle(Message message) {
        if (message == null || message.getData() == null) {
            log.error("Invalid data");
            return true;
        }
        try {
            log.warn("hander data {}", message.getData());
            CanalBean canalBean = Message.getData(this.objectMapper, message, CanalBean.class);
            canalBean.getItems().forEach(i -> {
                switch (i.getType()) {
                    case DELETE:
                        i.getData().forEach(o -> {
                            if (i.getRedisType().equals(RedisType.HASH)) {
                                this.redisDao.deleteFromMap(i.getKey(), i.getField());
                            }
                            else{
                                this.redisDao.deleteAKey(i.getKey());
                            }
                        });
                        break;
                    default:
                        i.getData().forEach(o -> {
                            if (i.getRedisType().equals(RedisType.HASH)) {
                                this.redisDao.saveToMap(i.getKey(), i.getField(), o);
                            } else if (i.getRedisType().equals(RedisType.LIST)) {
                                this.redisDao.rightPush(i.getKey(), o);
                            } else {
                                this.redisDao.set(i.getKey(), o);
                            }
                        });
                        break;
                }
            });
            return true;
        } catch (Exception e) {
            log.error("Error: ", e);
            return Observable.error(e);
        }
    }
}
