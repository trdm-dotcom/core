package com.homer.core.model.request;

import com.homer.core.common.model.DataRequest;
import lombok.Data;

import java.util.List;

@Data
public class DeleteWatchListRequest extends DataRequest {
    private List<Long> postIds;
}
