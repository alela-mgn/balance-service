package org.gopoints.balanceservice.mapper;

import org.gopoints.balanceservice.dto.AccountDto;
import org.gopoints.balanceservice.dto.TransactionDto;
import org.gopoints.balanceservice.model.Account;
import org.gopoints.balanceservice.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BalanceMapper {

    AccountDto accountToDto(Account account);
    Account accountDtoToEntity(AccountDto accountDto);

    TransactionDto transactionToDto(Transaction transaction);

    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now())")
    Transaction transactionDtoToEntity(TransactionDto transactionDto);
}
