package com.homer.core.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingDTO {
    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private String phoneNumber;
    private String name;
}
