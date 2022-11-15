package com.homer.core.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.homer.core.common.model.DataRequest;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GetBookingRequest extends DataRequest {
    private Boolean side;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDateTime fromTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDateTime toTime;
    private Integer offset;
    private Integer fetchCount;
}
