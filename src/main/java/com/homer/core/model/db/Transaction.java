package com.homer.core.model.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.homer.core.model.TransactionPartner;
import com.homer.core.model.TransactionStatus;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;

@Data
@Entity
@Table(name = "t_transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty
    private Long id;
    @JsonProperty
    private Double amount;
    @JsonProperty
    private TransactionPartner partner;
    @JsonProperty
    private TransactionStatus status;
    @JsonProperty
    private String requestCode;
    @OneToMany(mappedBy = "transaction")
    @JsonIgnore
    private Collection<TransactionHistory> transactionHistories;
    @OneToOne
    @MapsId
    @JoinColumn(name = "invoice_id")
    @JsonIgnore
    private Invoice invoice;
    @CreationTimestamp
    @JsonProperty
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @JsonProperty
    private LocalDateTime updatedAt;
}
