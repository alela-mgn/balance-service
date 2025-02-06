package org.gopoints.balanceservice.repository;

import org.gopoints.balanceservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdAndTimestampBetween(Long accountId, LocalDateTime startDate, LocalDateTime endDate);

}
