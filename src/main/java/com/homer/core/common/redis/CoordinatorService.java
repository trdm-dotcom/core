package com.homer.core.common.redis;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CoordinatorService {
    private static final String KEY_PREFIX = "coordinator";
    private static final Integer INDEX_RANGE = Integer.MAX_VALUE - 1;

    private final RedisTemplate<String, String> redisTemplate;
    private final AtomicInteger increment = new AtomicInteger(0);

    public CoordinatorService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String acquire(String key, String nodeId, int expireInSeconds) {
        String realKey = KEY_PREFIX + key;
        ListOperations<String, String> operations = redisTemplate.opsForList();
        Long count = operations.size(realKey);
        if (count != null && count > 0) {
            return null;
        }
        int index = increment.getAndUpdate(
                v -> {
                    if (v >= INDEX_RANGE) {
                        return 0;
                    }
                    return v + 1;
                }
        );
        String value = nodeId + index;
        operations.rightPush(realKey, value);
        redisTemplate.expire(realKey, expireInSeconds, TimeUnit.MILLISECONDS);
        List<String> firstInsertedValues = operations.range(realKey, 0, 0);
        if (firstInsertedValues != null && !firstInsertedValues.isEmpty() && value.equals(firstInsertedValues.get(0))) {
            return value;
        }
        return null;
    }

    public void waitForResult(String key) {
        String realKey = KEY_PREFIX + key;
        while (Boolean.TRUE.equals(redisTemplate.hasKey(realKey))) {
            try {
                Thread.sleep(300);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    public CompletableFuture<Void> waitForResultFuture(String key) {
        CompletableFuture future = new CompletableFuture();
        Thread checkingThread = new Thread(() -> {
            this.waitForResult(key);
            future.complete(null);
        });
        checkingThread.start();
        return  future;
    }

    public void release(String key) {
        String realKey = KEY_PREFIX + key;
        redisTemplate.delete(realKey);
    }
}
