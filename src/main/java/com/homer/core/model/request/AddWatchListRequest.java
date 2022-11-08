package com.homer.core.model.request;

import com.homer.core.common.model.DataRequest;
import lombok.Data;

@Data
public class AddWatchListRequest extends DataRequest {
    private Long postId;
}
