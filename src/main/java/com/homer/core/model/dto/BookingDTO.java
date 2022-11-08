package com.homer.core.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingDTO {
    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private String phoneNumberA;
    private String nameA;
    private String nameB;
    private String phoneNumberB;
    private Boolean side;
}
