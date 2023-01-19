package com.homer.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.model.dto.PostDTO;
import com.homer.core.model.request.AddWatchListRequest;
import com.homer.core.model.request.DeleteWatchListRequest;
import com.homer.core.model.request.GetWatchlistRequest;
import com.homer.core.utils.Utils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@SpringBootTest
public class WatchlistServiceTest {
    @Autowired
    WatchlistService watchlistService;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    public void addWatchListTest() throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String str = "{\"headers\":{\"token\":{\"userData\":{\"username\":\"test\",\"id\":525}}},\"postId\": 4}";
        AddWatchListRequest request = objectMapper.readValue(str, AddWatchListRequest.class);
        request.setHash(Utils.AesEncryptionHash(String.format("type=ADD&timeStamp=%d&key=wfyxb3sR1O", System.currentTimeMillis())));
        watchlistService.addWatchList(request, "1");
    }

    @Test
    public void deleteWatchListTest() throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String str = "{\"headers\":{\"token\":{\"userData\":{\"username\":\"test\",\"id\":525}}},\"postIds\": [4]}";
        DeleteWatchListRequest request = objectMapper.readValue(str, DeleteWatchListRequest.class);
        request.setHash(Utils.AesEncryptionHash(String.format("type=DELETE&timeStamp=%d&key=wfyxb3sR1O", System.currentTimeMillis())));
        watchlistService.deleteWatchList(request, "1");
    }

    @Test
    public void getWatchListTest() throws IOException {
        String str = "{\"headers\":{\"token\":{\"userData\":{\"username\":\"test\",\"id\":525}}}}";
        GetWatchlistRequest request = objectMapper.readValue(str, GetWatchlistRequest.class);
        List<PostDTO> list = watchlistService.getWatchList(request, "1");
        System.out.println(list);
    }
}
