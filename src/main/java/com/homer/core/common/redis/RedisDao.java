package com.homer.core.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.model.dto.DistrictDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RedisDao {
    private static final Logger log = LoggerFactory.getLogger(RedisDao.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDao(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private <E> String objectToString(E data) throws JsonProcessingException {
        if (data == null) {
            return RedisDataTypeEnum.NULL.getType();
        } else if (data instanceof Boolean) {
            return RedisDataTypeEnum.BOOLEAN.getType() + data;
        } else if (data instanceof String) {
            return RedisDataTypeEnum.STRING.getType() + data;
        } else if (data instanceof Number) {
            return RedisDataTypeEnum.NUMBER.getType() + data;
        } else {
            return RedisDataTypeEnum.OBJECT.getType() + objectMapper.writeValueAsString(data);
        }
    }

    private <E> E stringToObject(String data, Class<E> clazz) {
        String type = data.substring(0, 1);
        if (type.equals(RedisDataTypeEnum.UNDEFINED.getType())) {
            return null;
        }
        if (type.equals(RedisDataTypeEnum.NULL.getType())) {
            return null;
        }
        String value = data.substring(1);
        try {
            if (type.equals(RedisDataTypeEnum.BOOLEAN.getType())) {
                return (E) Boolean.valueOf(value);
            } else if (type.equals(RedisDataTypeEnum.STRING.getType())) {
                return (E) value;
            } else if (type.equals(RedisDataTypeEnum.NUMBER.getType())) {
                return (E) Double.valueOf(value);
            } else if (type.equals(RedisDataTypeEnum.OBJECT.getType())) {
                return objectMapper.readValue(value, clazz);
            } else {
                throw new IllegalStateException("invalid data");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        return stringToObject(redisTemplate.opsForValue().get(key), clazz);
    }

    public <T> T get(String key, TypeReference<T> typeReference){
        return stringToObject(redisTemplate.opsForValue().get(key), typeReference);
    }

    public <T> void set(String key, T value) {
        try {
            redisTemplate.opsForValue().set(key, objectToString(value));
        } catch (JsonProcessingException e) {
            log.error("error while set: {0}", e);
        }
    }

    public <T> void set(String key, T value, long ttlInMs) {
        try {
            redisTemplate.opsForValue().set(key, objectToString(value), ttlInMs, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            log.error("error while set: {0}", e);
        }
    }

    public void setKeyTtl(String key, long ttlInMs) {
        redisTemplate.expire(key, ttlInMs, TimeUnit.MILLISECONDS);
    }

    public <T> T hGet(String key, String subKey, Class<T> clazz) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return stringToObject(hashOperations.get(key, subKey), clazz);
    }

    public <T> T hGet(String key, String subKey, TypeReference<T> typeReference) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return stringToObject(hashOperations.get(key, subKey), typeReference);
    }

    public <T> List<T> hGetAll(String key, Class<T> clazz) {
        return this.hGetAllStream(key, clazz).collect(Collectors.toList());
    }

    public <T> List<T> hGetAll(String key, TypeReference<T> typeReference) {
        return this.hGetAllStream(key, typeReference).collect(Collectors.toList());
    }

    public <T> Stream<T> hGetAllStream(String key, Class<T> clazz) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return hashOperations.values(key).stream().map(it -> stringToObject(it, clazz));
    }

    public <T> Stream<T> hGetAllStream(String key, TypeReference<T> typeReference) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return hashOperations.values(key).stream().map(it -> stringToObject(it, typeReference));
    }

    public void deleteAKey(String redisKey) {
        redisTemplate.delete(redisKey);
    }

    public void saveToMap(String redisKey, String key, Object data) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        try {
            hashOperations.put(redisKey, key, objectToString(data));
        } catch (JsonProcessingException e) {
            log.error("error while hSet: {0}", e);
        }
    }

    public <T> void saveToMap(String redisKey, Collection<T> listData, Function<T, String> getKey) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        listData.forEach(it -> {
            try {
                hashOperations.put(redisKey, getKey.apply(it), objectToString(it));
            } catch (JsonProcessingException e) {
                log.error("error while hSet: {0}", e);
            }
        });
    }

    public void deleteFromMap(String redisKey, String key){
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.delete(redisKey, key);
    }

    public <T> List<T> listAll(String key, Class<T> clazz) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        List<String> data = listOperations.range(key, 0, -1);
        return data.stream().map(it -> stringToObject(it, clazz)).collect(Collectors.toList());
    }

    public <T> T leftPop(String key, Class<T> clazz) {
        List<T> list = this.leftPop(key, 1, clazz);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public <T> T rightPop(String key, Class<T> clazz) {
        List<T> list = this.rightPop(key, 1, clazz);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public <T> List<T> leftPop(String key, long count, Class<T> clazz) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        List<String> data = listOperations.leftPop(key, count);
        return data == null ? null : data.stream().map(it -> stringToObject(it, clazz)).collect(Collectors.toList());
    }

    public <T> List<T> rightPop(String key, long count, Class<T> clazz) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        List<String> data = listOperations.rightPop(key, count);
        return data == null ? null : data.stream().map(it -> stringToObject(it, clazz)).collect(Collectors.toList());
    }

    public <T> void leftPush(String key, T... data) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        for (T t : data) {
            try {
                listOperations.leftPush(key, objectToString(t));
            } catch (JsonProcessingException e) {
                log.error("fail to format to json {}", t, e);
                throw new RuntimeException(e);
            }
        }
    }

    public <T> void rightPush(String key, T... data) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        for (T t : data) {
            try {
                listOperations.rightPush(key, objectToString(t));
            } catch (JsonProcessingException e) {
                log.error("fail to format to json {}", t, e);
                throw new RuntimeException(e);
            }
        }
    }

    private <T> T stringToObject(String data, TypeReference<T> typeReference) {
        String type = data.substring(0, 1);
        if (type.equals(RedisDataTypeEnum.UNDEFINED.getType())) {
            return null;
        }
        if (type.equals(RedisDataTypeEnum.NULL.getType())) {
            return null;
        }
        String value = data.substring(1);
        try {
            if (type.equals(RedisDataTypeEnum.BOOLEAN.getType())) {
                return (T) Boolean.valueOf(value);
            } else if (type.equals(RedisDataTypeEnum.STRING.getType())) {
                return (T) value;
            } else if (type.equals(RedisDataTypeEnum.NUMBER.getType())) {
                return (T) Double.valueOf(value);
            } else if (type.equals(RedisDataTypeEnum.OBJECT.getType())) {
                return objectMapper.readValue(value, typeReference);
            } else {
                throw new IllegalStateException("invalid data");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
