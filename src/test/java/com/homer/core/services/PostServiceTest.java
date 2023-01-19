package com.homer.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homer.core.model.db.Post;
import com.homer.core.model.dto.PostDTO;
import com.homer.core.model.request.FilterPostRequest;
import com.homer.core.model.request.PostRequest;
import com.homer.core.repository.PostRepository;
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
public class PostServiceTest {
    @Autowired
    PostService postService;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PostRepository postRepository;

    @Test
    public void testQuery() throws JsonProcessingException {
        String str = "{\"headers\":{\"token\":{\"userData\":{\"username\":\"test\",\"id\":525}}},\"category\":\"APARTMENT\",\"city\":1, \"size\": 30.0}";
        FilterPostRequest request = objectMapper.readValue(str, FilterPostRequest.class);
        List<PostDTO> list = postService.getPost(request, "1");
        System.out.println(list);
    }

    @Test
    public void testCreateNewPost() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String str = "{\"headers\":{\"token\":{\"userData\":{\"username\":\"test\",\"id\":525}}},\"name\":\"test create\",\"category\":\"APARTMENT\",\"isPublic\":true,\"price\":10.1,\"address\":\"address\",\"description\":\"description\",\"city\":1,\"commune\":1,\"district\":1,\"size\":10.1,\"minMonth\":3,\"images\":[\"1\",\"2\",\"3\",\"4\"],\"features\":[2,3,0],\"latitude\":\"21.027763\",\"longitude\":\"105.834160\"}";
        PostRequest request = objectMapper.readValue(str, PostRequest.class);
        request.setHash(Utils.AesEncryptionHash(String.format("type=CREATE&timeStamp=%d&key=wfyxb3sR1O", System.currentTimeMillis())));
        System.out.println(request);
        postService.createNewPost(request, "1");
    }

    @Test
    public void testUpdatePost() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String str = "{\"headers\":{\"token\":{\"userData\":{\"username\":\"test\",\"id\":525}}},\"id\":4,\"name\":\"name\",\"category\":\"HOUSE\",\"isPublic\":true,\"price\":12.1,\"address\":\"address\",\"description\":\"description\",\"city\":1,\"commune\":1,\"district\":1,\"size\":14.1,\"minMonth\":4,\"images\":[\"1\",\"2\",\"3\",\"4\"],\"features\":[4,2,0],\"latitude\":\"21.027763\",\"longitude\":\"105.834160\"}}";
        PostRequest request = objectMapper.readValue(str, PostRequest.class);
        request.setHash(Utils.AesEncryptionHash(String.format("type=UPDATE&timeStamp=%d&key=wfyxb3sR1O", System.currentTimeMillis())));
        System.out.println(request);
        postService.updatePost(request, "1");
    }

    @Test
    public void testDeletePost() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String str = "{\"headers\":{\"token\":{\"userData\":{\"username\":\"test\",\"id\":525}}},\"id\":6}";
        PostRequest request = objectMapper.readValue(str, PostRequest.class);
        request.setHash(Utils.AesEncryptionHash(String.format("type=DELETE&timeStamp=%d&key=wfyxb3sR1O", System.currentTimeMillis())));
        postService.deletePost(request, "1");
    }
}
