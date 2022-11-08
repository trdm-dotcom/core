package com.homer.core.model.request;

import com.homer.core.common.model.DataRequest;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest extends DataRequest {
    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private Long postId;
}
