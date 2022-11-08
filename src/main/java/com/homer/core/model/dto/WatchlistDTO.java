package com.homer.core.model.dto;

import com.homer.core.model.db.Post;
import com.homer.core.model.db.Watchlist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistDTO {
    private String userId;
    private List<Long> postIds;

    public WatchlistDTO(Watchlist watchlist){
        this.userId = watchlist.getUserId();
        this.postIds = watchlist.getPosts().stream().map(Post::getId).collect(Collectors.toList());
    }
}
