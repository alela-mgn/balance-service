package org.gopoints.balanceservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
        Long id,
        Long accountId,
        BigDecimal amount,
        String operationType,
        LocalDateTime timestamp
) {
}
