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

@Getter
@Setter
@Entity
@Table(name = "t_booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty
    private Long id;
    @JsonProperty
    private String userId;
    @JsonProperty
    private String userIdSideB;
    @JsonProperty
    private String reason;
    @ManyToOne()
    @JoinColumn(name = "post_id", nullable = false)
    @JsonIgnore
    private Post post;
    private Boolean active;
    @JsonProperty
    private LocalDateTime fromTime;
    @JsonProperty
    private LocalDateTime toTime;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
