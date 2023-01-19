package com.homer.core.model.request;

import com.homer.core.model.Category;
import lombok.Data;

import java.util.List;

@Data
public class FilterPostRequest {
    private List<Long> ids;
    private String name;
    private Long city;
    private Long commune;
    private Long district;
    private Category category;
    private Double start;
    private Double end;
    private Double size;
    private List<Long> features;
    private Integer offset;
    private Integer fetchCount;
    private Double latitude;
    private Double longitude;
}
