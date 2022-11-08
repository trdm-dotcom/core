package com.homer.core.model.dto;

import com.homer.core.model.Category;
import com.homer.core.model.db.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Collection<Image> image;
    private Collection<Feature> features;
    private String description;
    private Category category;

    public PostDTO(Post post) {
        this.id = post.getId();
        this.name = post.getName();
        this.price = post.getPrice();
        this.address = post.getAddress();
        this.size = post.getSize();
        this.city = post.getCity();
        this.district = post.getDistrict();
        this.commune = post.getCommune();
        this.image = post.getImages();
        this.features = post.getFeatures();
        this.userId = post.getUserId();
        this.category = post.getCategory();
    }
}
