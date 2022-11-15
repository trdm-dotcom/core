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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

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
    public Object addWatchList(AddWatchListRequest request, String msgId) throws IOException {
        log.info("{} add watch list {}", msgId, request);
        Watchlist watchlist = watchlistRepository.findByUserId(request.getHeaders().getToken().getUserData().getUserId()).orElse(new Watchlist());
        Post post = postRepository.findById(request.getPostId()).orElseThrow(() -> new GeneralException(Constants.OBJECT_NOT_FOUND));
        Collection<Post> posts = watchlist.getPosts();
        posts.add(post);
        WatchlistDTO result = new WatchlistDTO(watchlistRepository.save(watchlist));
        this.saveWatchToRedis(result, SyncType.UPDATE);
        return new HashMap<>();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Object deleteWatchList(DeleteWatchListRequest request, String msgId) throws IOException {
        log.info("{} delete watch list {}", msgId, request);
        Watchlist watchlist = watchlistRepository.findByUserId(request.getHeaders().getToken().getUserData().getUserId()).orElseThrow(() -> new GeneralException(Constants.OBJECT_NOT_FOUND));
        List<Post> posts = postRepository.findByIdIn(request.getPostIds());
        if (!CollectionUtils.isEmpty(posts)) {
            watchlist.getPosts().removeAll(posts);
        }
        WatchlistDTO result = new WatchlistDTO(watchlistRepository.save(watchlist));
        this.saveWatchToRedis(result, SyncType.UPDATE);
        return new HashMap<>();
    }

    public List<PostDTO> getWatchList(GetWatchlistRequest request, String msgId){
        log.info("{} get watch list {}", msgId, request);
        List<PostDTO> list = new ArrayList<>();
        WatchlistDTO watchlistDTO = null;
        try {
            watchlistDTO = this.redisDao.hGet(Constants.REDIS_KEY_WATCHLIST, request.getHeaders().getToken().getUserData().getUserId(), WatchlistDTO.class);
        }
        catch (Exception e){
            Watchlist watchlist = watchlistRepository.findByUserId(request.getHeaders().getToken().getUserId()).orElse(null);
            if (watchlist == null){
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
