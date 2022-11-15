package com.homer.core.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.homer.core.common.model.DataRequest;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateBookingRequest extends DataRequest {
    private Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDateTime fromTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDateTime toTime;
    private String reason;
}
