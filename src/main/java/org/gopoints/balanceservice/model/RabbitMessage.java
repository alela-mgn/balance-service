package org.gopoints.balanceservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RabbitMessage {
    private Long accountId;
    private BigDecimal amount;
    private String operationType;
    private Long targetAccountId;
}
