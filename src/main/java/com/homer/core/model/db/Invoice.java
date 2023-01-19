package com.homer.core.model.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.homer.core.model.InvoiceStatus;
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
@Table(name = "t_invoice")
public class Invoice {
    @Id
    private String id;
    @JsonProperty
    private String userId;
    @JsonProperty
    private String userIdSideB;
    @JsonProperty
    private Double price;
    @ManyToOne()
    @JoinColumn(name = "post_id", nullable = false)
    @JsonIgnore
    private Post post;
    @OneToOne(mappedBy = "invoice")
    @PrimaryKeyJoinColumn
    @JsonIgnore
    private Transaction transaction;
    @JsonProperty
    private String description;
    @JsonProperty
    private InvoiceStatus status;
    @CreationTimestamp
    @JsonProperty
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @JsonProperty
    private LocalDateTime updatedAt;
}
