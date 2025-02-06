package org.gopoints.balanceservice.dto;

import java.math.BigDecimal;

public record AccountDto(
        Long id,
        Integer userId,
        BigDecimal balance
) {
}
