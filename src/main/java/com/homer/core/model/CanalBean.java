package com.homer.core.model;

import lombok.Data;

import java.util.List;

@Data
public class CanalBean {
    private List<CanalBeanItem> items;

    @Data
    public static class CanalBeanItem{
        private List<Object> data;
        private SyncType type;
        private String key;
        private String field;
        private RedisType redisType;
    }
}
