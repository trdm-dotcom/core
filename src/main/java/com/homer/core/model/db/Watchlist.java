package com.homer.core.model.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@Getter
@Setter
@Entity
@Table(name = "t_watchlist")
public class Watchlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty
    private String userId;
    @ManyToMany()
    @JoinTable(
            name = "t_post_watchlist",
            joinColumns = @JoinColumn(name = "watchlist_id"),
            inverseJoinColumns = @JoinColumn(name = "post_id"))
    @JsonIgnore
    private Collection<Post> posts = new ArrayList<>();
    @CreationTimestamp
    @JsonProperty
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @JsonProperty
    private LocalDateTime updatedAt;
}
