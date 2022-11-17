package com.homer.core.model.request;

import com.homer.core.common.model.DataRequest;
import com.homer.core.common.utils.validator.CombineValidator;
import com.homer.core.common.utils.validator.EnumValidator;
import com.homer.core.common.utils.validator.NumberValidator;
import com.homer.core.common.utils.validator.StringValidator;
import com.homer.core.model.Category;
import lombok.Data;

import java.util.List;

@Data
public class PostRequest extends DataRequest {
    private Long id;
    private String name;
    private Category category;
    private Boolean isPublic;
    private Double price;
    private String address;
    private String description;
    private Long city;
    private Long commune;
    private Long district;
    private Double size;
    private String hash;
    private List<String> images;
    private List<Long> features;
    private Integer minMonth;

    public void validate(){
        new CombineValidator()
                .add(new StringValidator("name", this.name).empty())
                .add(new StringValidator("address", this.address).empty())
                .add(new StringValidator("hash", this.hash).empty())
                .add(new EnumValidator("category", this.category.name(), Category.class))
                .add(new NumberValidator("price", this.price).min(0.0))
                .add(new NumberValidator("size", this.size).min(1.0))
                .add(new NumberValidator("minMonth", this.minMonth).min(1.0)).check();
    }
}
