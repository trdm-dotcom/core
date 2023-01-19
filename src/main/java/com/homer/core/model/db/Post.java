package com.homer.core.model.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.homer.core.configurations.JSONArrayConverter;
import com.homer.core.model.Category;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.json.JSONArray;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@Getter
@Setter
@Entity
@Table(name = "t_post")
@NoArgsConstructor
@AllArgsConstructor
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
    @Lob
    @Convert(converter = JSONArrayConverter.class)
    @JsonIgnore
    private JSONArray images;
    @ManyToMany()
    @JoinTable(
            name = "t_post_feature",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id"))
    @JsonIgnore
    private Collection<Feature> features = new ArrayList<>();
    @OneToMany(mappedBy = "post")
    @JsonIgnore
    private Collection<Booking> bookings = new ArrayList<>();
    @ManyToOne()
    @JoinColumn(name = "city_id", nullable = false)
    @JsonIgnore
    private City city;
    @ManyToOne()
    @JoinColumn(name = "commune_id", nullable = false)
    @JsonIgnore
    private Commune commune;
    @ManyToOne()
    @JoinColumn(name = "district_id", nullable = false)
    @JsonIgnore
    private District district;
    @OneToMany(mappedBy = "post")
    @JsonIgnore
    private Collection<Invoice> invoices = new ArrayList<>();
    @ManyToMany()
    @JoinTable(
            name = "t_post_watchlist",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "watchlist_id"))
    @JsonIgnore
    private Collection<Watchlist> Watchlist = new ArrayList<>();
    private Double latitude;
    private Double longitude;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
