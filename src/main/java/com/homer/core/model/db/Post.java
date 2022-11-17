package com.homer.core.model.db;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.homer.core.model.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@Data
@Entity
@Table(name = "t_posts")
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Post implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty
    private Long id;
    @JsonProperty
    private String userId;
    @JsonProperty
    private String name;
    @Enumerated(EnumType.STRING)
    private Category category;
    @JsonProperty
    private Boolean isPublic;
    @JsonProperty
    private Double price;
    @JsonProperty
    private Integer minMonth;
    @JsonProperty
    private String address;
    @JsonProperty
    private String description;
    @JsonProperty
    private Double size;
    @OneToMany(mappedBy = "post")
    @JsonProperty
    private Collection<Image> images;

    @ManyToMany()
    @JoinTable(
            name = "feature_posts",
            joinColumns = @JoinColumn(name = "feature_id"),
            inverseJoinColumns = @JoinColumn(name = "post_id"))
    @JsonProperty
    private Collection<Feature> features = new ArrayList<>();
    @OneToMany(mappedBy = "post")
    @JsonIgnore
    private Collection<Booking> bookings = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private City city;
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Commune commune;
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private District district;
    @OneToMany(mappedBy = "post")
    @JsonIgnore
    private Collection<Invoice> invoices;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
