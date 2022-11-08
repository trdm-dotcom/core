package com.homer.core.model.db;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long amount;

    private String bankCode;

    private String bankTranNo;

    private String cardType;

    private String orderInfo;

    private String payDate;

    private String responseCode;

    private String tmnCode;

    private String transactionNo;

    private String transactionStatus;

    private String vnpTxnRef;

    private String secureHash;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
