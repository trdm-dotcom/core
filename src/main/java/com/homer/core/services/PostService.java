package com.homer.core.services;

import com.ea.async.Async;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.exceptions.InvalidValueException;
import com.homer.core.common.redis.RedisDao;
import com.homer.core.configurations.AppConf;
import com.homer.core.constants.Constants;
import com.homer.core.model.CanalBean;
import com.homer.core.model.RedisType;
import com.homer.core.model.SyncType;
import com.homer.core.model.db.*;
import com.homer.core.model.dto.PostDTO;
import com.homer.core.model.request.FilterPostRequest;
import com.homer.core.model.request.PostDetailRequest;
import com.homer.core.model.request.PostRequest;
import com.homer.core.model.response.UserInfo;
import com.homer.core.repository.*;
import com.homer.core.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PostService {
    private final AppConf appConf;
    private final PostRepository postRepository;
    private final KafkaProducerService kafkaProducerService;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final CommuneRepository communeRepository;
    private final FeatureRepository featureRepository;
    private final RedisDao redisDao;


    public PostService(
            AppConf appConf,
            PostRepository postRepository,
            KafkaProducerService kafkaProducerService,
            CityRepository cityRepository,
            DistrictRepository districtRepository,
            CommuneRepository communeRepository,
            FeatureRepository featureRepository, RedisDao redisDao
    ) {
        this.appConf = appConf;
        this.postRepository = postRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
        this.communeRepository = communeRepository;
        this.featureRepository = featureRepository;
        this.redisDao = redisDao;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Object createNewPost(PostRequest request, String msgId) throws IOException {
        log.info("{} create new post {}", msgId, request);
        request.validate();
        UserInfo userInfo = Async.await(Utils.getUserInfo(msgId, request.getHeaders().getToken().getUserData().getUserId()));
        if (!userInfo.getIsVerified()) {
            throw new GeneralException(Constants.USER_HADNT_BEEN_VERIFIED);
        }
        if (request.getCity() == null && request.getDistrict() == null) {
            throw new InvalidValueException("city or district");
        }
        if (CollectionUtils.isEmpty(request.getImages())) {
            throw new InvalidValueException("images");
        }
        if (CollectionUtils.isEmpty(request.getFeatures())) {
            throw new InvalidValueException("features");
        }
        City city = null;
        District district = null;
        Commune commune = null;
        if (request.getCity() != null) {
            city = this.cityRepository.findById(request.getCity()).orElse(null);
        }
        if (request.getDistrict() != null) {
            district = this.districtRepository.findById(request.getDistrict()).orElse(null);
        }
        if (request.getCommune() != null) {
            commune = this.communeRepository.findById(request.getCommune()).orElse(null);
        }
        List<Feature> features = this.featureRepository.findAllById(request.getFeatures());
        Post post = new Post();
        post.setUserId(request.getHeaders().getToken().getUserData().getUserId());
        post.setName(request.getName());
        post.setDescription(request.getDescription());
        post.setCategory(request.getCategory());
        post.setIsPublic(request.getIsPublic() == null || request.getIsPublic());
        post.setDescription(request.getDescription());
        post.setPrice(request.getPrice());
        post.setSize(request.getSize());
        post.setAddress(request.getAddress());
        post.setCity(city);
        post.setDistrict(district);
        post.setCommune(commune);
        post.setFeatures(features);
        Collection<Image> images = request.getImages().stream().map(i -> {
            Image image = new Image();
            image.setUrl(i);
            image.setName(post.getName());
            image.setPost(post);
            return image;
        }).collect(Collectors.toSet());
        post.setImages(images);
        PostDTO result = new PostDTO(this.postRepository.save(post));
        if (request.getIsPublic() == null || request.getIsPublic()) {
            this.savePostToRedis(result, SyncType.INSERT);
        }
        return result;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Object updatePost(PostRequest request, String msgId) throws IOException {
        log.info("{} update post {}", msgId, request);
        request.validate();
        UserInfo userInfo = Async.await(Utils.getUserInfo(msgId, request.getHeaders().getToken().getUserData().getUserId()));
        if (!userInfo.getIsVerified()) {
            throw new GeneralException(Constants.USER_HADNT_BEEN_VERIFIED);
        }
        City city = null;
        District district = null;
        Commune commune = null;
        if (request.getId() == null) {
            throw new InvalidValueException("id");
        }
        if (request.getCity() == null && request.getDistrict() == null) {
            throw new InvalidValueException("city or district");
        }
        if (CollectionUtils.isEmpty(request.getImages())) {
            throw new InvalidValueException("images");
        }
        if (request.getCity() != null) {
            city = this.cityRepository.findById(request.getCity()).orElse(null);
        }
        if (request.getDistrict() != null) {
            district = this.districtRepository.findById(request.getDistrict()).orElse(null);
        }
        if (request.getCommune() != null) {
            commune = this.communeRepository.findById(request.getCommune()).orElse(null);
        }
        Post post = this.postRepository.findById(request.getId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (!Objects.equals(post.getUserId(), request.getHeaders().getToken().getUserData().getUserId())) {
            throw new GeneralException("OBJECT_NOT_FOUND");
        }
        List<Feature> features = this.featureRepository.findAllById(request.getFeatures());
        post.setName(request.getName());
        post.setDescription(request.getDescription());
        post.setCategory(request.getCategory());
        post.setIsPublic(request.getIsPublic() == null || request.getIsPublic());
        post.setDescription(request.getDescription());
        post.setPrice(request.getPrice());
        post.setSize(request.getSize());
        post.setAddress(request.getAddress());
        post.setCity(city);
        post.setDistrict(district);
        post.setCommune(commune);
        post.setFeatures(features);
        Collection<Image> images = request.getImages().stream().map(i -> {
            Image image = new Image();
            image.setUrl(i);
            image.setName(post.getName());
            image.setPost(post);
            return image;
        }).collect(Collectors.toSet());
        post.setImages(images);
        PostDTO result = new PostDTO(this.postRepository.save(post));
        if (request.getIsPublic() == null || request.getIsPublic()) {
            this.savePostToRedis(result, SyncType.UPDATE);
        } else {
            this.savePostToRedis(result, SyncType.DELETE);
        }
        return new HashMap<>();
    }

    public List<PostDTO> getPost(FilterPostRequest request, String msgId) {
        log.info("{} get post {}", msgId, request);
        int offset = request.getOffset() == null ? Constants.DEFAULT_OFFSET : Math.max(request.getOffset(), Constants.DEFAULT_OFFSET);
        int fetchCount = request.getFetchCount() == null ? Constants.DEFAULT_FETCH_COUNT : Math.max(request.getFetchCount(), Constants.DEFAULT_FETCH_COUNT);
        List<PostDTO> list = this.redisDao.hGetAll(Constants.REDIS_KEY_POST, PostDTO.class);
        log.info("list 1 {}", list);
        if (CollectionUtils.isEmpty(list)) {
            list = this.postRepository.findAll(
                            request.getIds(),
                            null,
                            request.getName(),
                            request.getCity(),
                            request.getCommune(),
                            request.getDistrict(),
                            request.getCategory(),
                            request.getStart(),
                            request.getEnd(),
                            request.getSize(),
                            request.getFeatures(),
                            PageRequest.of(offset, fetchCount))
                    .getContent()
                    .stream()
                    .map(PostDTO::new)
                    .collect(Collectors.toList());
        } else {
            list = list.stream()
                    .filter(this.buidlPredicate(null, request)
                            .stream()
                            .reduce(Predicate::and).orElse(x -> true))
                    .skip((long) offset * fetchCount)
                    .limit(fetchCount)
                    .collect(Collectors.toList());
        }
        return list;
    }

    public Object getPostDetail(PostDetailRequest request, String msgId) throws IOException {
        log.info("{} get detail post {}", msgId, request);
        Post post = this.redisDao.hGet(Constants.REDIS_KEY_POST, request.getId().toString(), Post.class);
        if (post == null) {
            post = this.postRepository.findById(request.getId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
            this.savePostToRedis(new PostDTO(post), SyncType.INSERT);
        }
        UserInfo userInfo = null;
        try {
            userInfo = Async.await(Utils.getUserInfo(msgId, post.getUserId()));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return new HashMap<>();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Object deletePost(PostRequest request, String msgId) throws IOException {
        log.info("{} delete post {}", msgId, request);
        request.validate();
        UserInfo userInfo = Async.await(Utils.getUserInfo(msgId, request.getHeaders().getToken().getUserData().getUserId()));
        if (!userInfo.getIsVerified()) {
            throw new GeneralException(Constants.USER_HADNT_BEEN_VERIFIED);
        }
        if (request.getId() == null) {
            throw new InvalidValueException("id");
        }
        Post post = this.postRepository.findById(request.getId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (!Objects.equals(post.getUserId(), request.getHeaders().getToken().getUserData().getUserId())) {
            throw new GeneralException("OBJECT_NOT_FOUND");
        }
        this.postRepository.delete(post);
        PostDTO postDTO = new PostDTO();
        postDTO.setId(post.getId());
        this.savePostToRedis(postDTO, SyncType.DELETE);
        return new HashMap<>();
    }

    private void savePostToRedis(PostDTO post, SyncType type) throws IOException {
        CanalBean canalBean = new CanalBean();
        List<CanalBean.CanalBeanItem> items = new ArrayList<>();
        CanalBean.CanalBeanItem item = new CanalBean.CanalBeanItem();
        item.setRedisType(RedisType.HASH);
        item.setType(type);
        item.setData(Collections.singletonList(post));
        item.setKey(Constants.REDIS_KEY_POST);
        item.setField(post.getId().toString());
        items.add(item);
        canalBean.setItems(items);
        kafkaProducerService.sendMessage(appConf.getTopics().getSyncRedisMysql(), "", canalBean);
    }

    private List<Predicate<PostDTO>> buidlPredicate(String userId, FilterPostRequest request) {
        List<Predicate<PostDTO>> allPredicates = new ArrayList<Predicate<PostDTO>>();
        if (!CollectionUtils.isEmpty(request.getIds())){
            allPredicates.add(i -> request.getIds().contains(i.getId()));
        }
        if (userId != null) {
            allPredicates.add(i -> i.getUserId().equals(userId));
        }
        if (request.getName() != null) {
            allPredicates.add(i -> i.getName().matches(".*" + request.getName() + ".*"));
        }
        if (request.getCity() != null) {
            allPredicates.add(i -> Objects.equals(i.getCity().getId(), request.getCity()));
        }
        if (request.getCommune() != null) {
            allPredicates.add(i -> Objects.equals(i.getCommune().getId(), request.getCommune()));
        }
        if (request.getDistrict() != null) {
            allPredicates.add(i -> Objects.equals(i.getDistrict().getId(), request.getDistrict()));
        }
        if (request.getCategory() != null) {
            allPredicates.add(i -> i.getCategory().equals(request.getCategory()));
        }
        if (request.getStart() != null) {
            allPredicates.add(i -> i.getPrice() >= request.getStart());
        }
        if (request.getEnd() != null) {
            allPredicates.add(i -> i.getPrice() <= request.getEnd());
        }
        if (request.getSize() != null) {
            allPredicates.add(i -> i.getSize() <= request.getSize());
        }
        if (!CollectionUtils.isEmpty(request.getFeatures())) {
            allPredicates.add(i -> i.getFeatures().containsAll(this.featureRepository.findAllById(request.getFeatures())));
        }
        return allPredicates;
    }
}
