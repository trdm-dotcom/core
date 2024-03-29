package com.homer.core.model.dto;

import com.homer.core.model.Category;
import com.homer.core.model.db.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Long id;
    private String userId;
    private String name;
    private Double price;
    private String address;
    private Double size;
    private City city;
    private District district;
    private Commune commune;
    private Collection<Object> image = new ArrayList<>();
    private Collection<Feature> features = new ArrayList<>();
    private String description;
    private Category category;
    private Integer minMonth;
    private Double latitude;
    private Double longitude;

    public PostDTO(Post post) {
        this.id = post.getId();
        this.name = post.getName();
        this.price = post.getPrice();
        this.address = post.getAddress();
        this.size = post.getSize();
        this.city = post.getCity();
        this.district = post.getDistrict();
        this.commune = post.getCommune();
        this.image = post.getImages().toList();
        this.features = post.getFeatures();
        this.userId = post.getUserId();
        this.category = post.getCategory();
        this.minMonth = post.getMinMonth();
        this.latitude = post.getLatitude();
        this.longitude = post.getLongitude();
    }
}
