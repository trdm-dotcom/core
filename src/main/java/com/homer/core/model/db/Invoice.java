package com.homer.core.model.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.homer.core.model.InvoiceStatus;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
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
