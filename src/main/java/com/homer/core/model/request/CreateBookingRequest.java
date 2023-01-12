package com.homer.core.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.homer.core.common.model.DataRequest;
import com.homer.core.common.utils.validator.CombineValidator;
import com.homer.core.common.utils.validator.EnumValidator;
import com.homer.core.common.utils.validator.NumberValidator;
import com.homer.core.common.utils.validator.StringValidator;
import com.homer.core.model.Category;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest extends DataRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDateTime fromTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDateTime toTime;
    private Long postId;
    private String hash;
}
