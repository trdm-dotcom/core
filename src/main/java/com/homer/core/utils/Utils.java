package com.homer.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.model.Message;
import com.homer.core.common.model.Response;
import com.homer.core.common.redis.RedisDao;
import com.homer.core.configurations.AppConf;
import com.homer.core.constants.Constants;
import com.homer.core.model.FirebaseType;
import com.homer.core.model.request.PushNotificationRequest;
import com.homer.core.model.response.UserInfo;
import com.homer.core.services.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
        Utils.redisDao = redisDao;
        Utils.objectMapper = objectMapper;
        Utils.kafkaProducerService = kafkaProducerService;
        Utils.appConf = appConf;
    }

    public static UserInfo getUserInfoKafka(String msgId, String userId) {
        Map<String, String> request = new HashMap<String, String>() {{
            put("id", userId);
        }};
        CompletableFuture<Message> future = kafkaProducerService.sendAsyncRequest(appConf.getTopics().getUserInfo(), null, appConf.getClusterId(), request);
        try {
            Message message = future.get();
            Response response = Message.getData(objectMapper, message, Response.class);
            if (response.getStatus() != null) {
                throw new RuntimeException(response.getStatus().getCode());
            }
            log.info("{} aaa response data {}", msgId, response.getData());
            return objectMapper.convertValue(response.getData(), UserInfo.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static UserInfo getUserInfo(String msgId, String userId) {
        try {
            return redisDao.hGet(Constants.REDIS_KEY_USERINFO, userId, UserInfo.class);
        } catch (Exception e) {
            return getUserInfoKafka(msgId, userId);
        }
    }

    public static void sendNotification(String msgId, String userId, String titile, String content, String template, FirebaseType type, String condition) throws IOException {
        UserInfo userInfo = getUserInfo(msgId, userId);
        PushNotificationRequest request = new PushNotificationRequest();
        request.setUserId(userId);
        request.setTitle(titile);
        request.setContent(content);
        request.setTemplate(template);
        request.setIsSave(true);
        request.setType(type);
        if (type.equals(FirebaseType.TOKEN)) {
            request.setToken(userInfo.getDeviceToken());
        } else {
            request.setCondition(condition);
        }
        kafkaProducerService.sendMessage(appConf.getTopics().getPushNotification(), "", request);
    }

    public static Map<String, String> AesDecryptionHash(String hash) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, Utils.getAesKey(), Utils.getAesIv());
        String hashDecrypted = new String(cipher.doFinal(Base64.getDecoder().decode(hash)));
        Map<String, String> objectHash = new HashMap<>();
        while (true) {
            int endKey = hashDecrypted.indexOf("=");
            int endValue = hashDecrypted.contains("&") ? hashDecrypted.indexOf("&") : hashDecrypted.length();
            String key = hashDecrypted.substring(0, endKey);
            String value = hashDecrypted.substring(endKey + 1, endValue);
            objectHash.put(key, value);
            if (endValue + 1 > hashDecrypted.length()) {
                break;
            }
            hashDecrypted = hashDecrypted.substring(endValue + 1);
        }
        return objectHash;
    }

    public static void validate(String hash, String type, LocalDateTime now) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Map<String, String> objectHash = AesDecryptionHash(hash);
        LocalDateTime timeStamp = Instant.ofEpochMilli(Long.parseLong(objectHash.get("timeStamp"))).atZone(ZoneId.systemDefault()).toLocalDateTime();
        if (!objectHash.get("type").equals(type)
                || !objectHash.get("key").equals(appConf.getAes().getKeyHash())
                || now.isBefore(timeStamp)
        ) {
            throw new GeneralException("INVALID_HASH");
        }
        if (Duration.between(timeStamp, now).toMillis() > appConf.getTimeStampHash()) {
            throw new GeneralException("TO_FAST");
        }
    }

    public static Key getAesKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(appConf.getAes().getKey()), "AES");
    }

    public static IvParameterSpec getAesIv() {
        return new IvParameterSpec(Base64.getDecoder().decode(appConf.getAes().getIv().getBytes()));
    }

    public static String AesEncryptionHash(String str) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, getAesKey(), getAesIv());
        return Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes()));
    }

    public static Double haversineDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        double R = 6371.0088; // Earth's radius Km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
}
