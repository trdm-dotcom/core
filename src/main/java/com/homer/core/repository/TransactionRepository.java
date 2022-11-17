package com.homer.core.repository;

import com.homer.core.model.TransactionPartner;
import com.homer.core.model.TransactionStatus;
import com.homer.core.model.db.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByInvoiceIdAndStatusAndPartner(String InvoiceId, TransactionStatus status, TransactionPartner partner);

    Optional<Transaction> findByInvoiceIdAndPartner(String InvoiceId, TransactionPartner partner);
}
