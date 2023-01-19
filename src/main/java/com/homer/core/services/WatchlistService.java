package com.homer.core.services;

import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.redis.RedisDao;
import com.homer.core.configurations.AppConf;
import com.homer.core.constants.Constants;
import com.homer.core.model.CanalBean;
import com.homer.core.model.RedisType;
import com.homer.core.model.SyncType;
import com.homer.core.model.db.Post;
import com.homer.core.model.db.Watchlist;
import com.homer.core.model.dto.PostDTO;
import com.homer.core.model.dto.WatchlistDTO;
import com.homer.core.model.request.AddWatchListRequest;
import com.homer.core.model.request.DeleteWatchListRequest;
import com.homer.core.model.request.FilterPostRequest;
import com.homer.core.model.request.GetWatchlistRequest;
import com.homer.core.repository.PostRepository;
import com.homer.core.repository.WatchlistRepository;
import com.homer.core.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WatchlistService {
    private final WatchlistRepository watchlistRepository;

    private final AppConf appConf;

    private final PostRepository postRepository;

    private final KafkaProducerService kafkaProducerService;

    private final RedisDao redisDao;

    public final PostService postService;

    @Autowired
    public WatchlistService(
            WatchlistRepository watchlistRepository,
            AppConf appConf,
            PostRepository postRepository,
            KafkaProducerService kafkaProducerService,
            RedisDao redisDao,
            PostService postService
    ) {
        this.watchlistRepository = watchlistRepository;
        this.appConf = appConf;
        this.postRepository = postRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.redisDao = redisDao;
        this.postService = postService;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Object addWatchList(AddWatchListRequest request, String msgId) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        log.info("{} add watch list {}", msgId, request);
        request.validate();
        Utils.validate(request.getHash(), "ADD", LocalDateTime.now());
        String userId = request.getHeaders().getToken().getUserData().getId();
        Watchlist watchlist = watchlistRepository.findByUserId(userId).orElse(new Watchlist());
        if (watchlist.getPosts() != null && watchlist.getPosts().stream().map(Post::getId).collect(Collectors.toList()).contains(request.getPostId())) {
            return new HashMap<String, String>() {{
                put("message", "ALREADY_EXISTS");
            }};
        }
        watchlist.setUserId(userId);
        Post post = postRepository.findById(request.getPostId()).orElseThrow(() -> new GeneralException(Constants.OBJECT_NOT_FOUND));
        Collection<Post> posts = watchlist.getPosts();
        posts.add(post);
        WatchlistDTO result = new WatchlistDTO(watchlistRepository.save(watchlist));
        this.saveWatchToRedis(result, SyncType.UPDATE);
        return new HashMap<String, Object>() {{
            put("message", Constants.CREATE_SUCCESS);
        }};
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Object deleteWatchList(DeleteWatchListRequest request, String msgId) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        log.info("{} delete watch list {}", msgId, request);
        request.validate();
        if (CollectionUtils.isEmpty(request.getPostIds())) {
            throw new GeneralException(Constants.DELETE_FAILED);
        }
        Utils.validate(request.getHash(), "DELETE", LocalDateTime.now());
        Watchlist watchlist = watchlistRepository.findByUserId(request.getHeaders().getToken().getUserData().getId()).orElseThrow(() -> new GeneralException(Constants.OBJECT_NOT_FOUND));
        List<Post> removePosts = watchlist.getPosts().stream().filter(p -> request.getPostIds().contains(p.getId())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(removePosts)) {
            return new HashMap<>();
        }
        watchlist.getPosts().removeAll(removePosts);
        WatchlistDTO result = new WatchlistDTO(watchlistRepository.save(watchlist));
        this.saveWatchToRedis(result, SyncType.UPDATE);
        return new HashMap<String, Object>() {{
            put("message", Constants.DELETE_SUCCESS);
        }};
    }

    public List<PostDTO> getWatchList(GetWatchlistRequest request, String msgId) {
        log.info("{} get watch list {}", msgId, request);
        List<PostDTO> list = new ArrayList<>();
        WatchlistDTO watchlistDTO = null;
        try {
            watchlistDTO = this.redisDao.hGet(Constants.REDIS_KEY_WATCHLIST, request.getHeaders().getToken().getUserData().getId(), WatchlistDTO.class);
        } catch (Exception e) {
            Watchlist watchlist = watchlistRepository.findByUserId(request.getHeaders().getToken().getUserId()).orElse(null);
            if (watchlist == null) {
                return list;
            }
            watchlistDTO = new WatchlistDTO(watchlist);
        }
        FilterPostRequest filterPostRequest = new FilterPostRequest();
        filterPostRequest.setIds(watchlistDTO.getPostIds());
        return this.postService.getPost(filterPostRequest, msgId);
    }

    private void saveWatchToRedis(WatchlistDTO watchlist, SyncType type) throws IOException {
        CanalBean canalBean = new CanalBean();
        List<CanalBean.CanalBeanItem> items = new ArrayList<>();
        CanalBean.CanalBeanItem item = new CanalBean.CanalBeanItem();
        item.setRedisType(RedisType.HASH);
        item.setType(type);
        item.setData(Collections.singletonList(watchlist));
        item.setKey(Constants.REDIS_KEY_WATCHLIST);
        item.setField(watchlist.getUserId());
        items.add(item);
        canalBean.setItems(items);
        kafkaProducerService.sendMessage(appConf.getTopics().getSyncRedisMysql(), "", canalBean);
    }
}
