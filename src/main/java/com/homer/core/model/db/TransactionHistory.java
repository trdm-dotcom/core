package com.homer.core.model.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.homer.core.model.TransactionHistoryStatus;
import com.homer.core.model.TransactionPartner;
import com.homer.core.model.TransactionType;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_transaction_histories")
public class TransactionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty
    private Long id;
    private TransactionPartner partner;
    @JsonProperty
    private String userId;
    @JsonProperty
    private String bankCode;
    @JsonProperty
    private String bankTranNo;
    @JsonProperty
    private String cardType;
    @JsonProperty
    private String responseCode;
    @JsonProperty
    private String transactionNo;
    @JsonProperty
    private String transactionStatus;
    @JsonProperty
    private String transactionCode;
    @JsonProperty
    private String requestCode;
    @JsonProperty
    private Long amount;
    @JsonProperty
    private TransactionType type;
    @ManyToOne()
    @JsonIgnore
    private Transaction transaction;
    @JsonProperty
    private TransactionHistoryStatus status;
    @JsonProperty
    private String failReason;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
