package org.gopoints.balanceservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gopoints.balanceservice.dto.AccountDto;
import org.gopoints.balanceservice.dto.TransactionDto;
import org.gopoints.balanceservice.model.Account;
import org.gopoints.balanceservice.model.Transaction;
import org.gopoints.balanceservice.service.BalanceService;
import org.gopoints.balanceservice.mapper.BalanceMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class BalanceController {

    private final BalanceService balanceService;
    private final BalanceMapper balanceMapper;

    @PostMapping("/{accountId}/deposit")
    public void deposit(@PathVariable Long accountId, @RequestParam BigDecimal amount) {
        log.info("REST request: deposit, accountId={}, amount={}", accountId, amount);
        balanceService.deposit(accountId, amount);
    }

    @PostMapping("/{accountId}/withdraw")
    public void withdraw(@PathVariable Long accountId, @RequestParam BigDecimal amount) {
        log.info("REST request: withdraw, accountId={}, amount={}", accountId, amount);
        balanceService.withdraw(accountId, amount);
    }

    @PostMapping("/transfer")
    public void transfer(@RequestParam Long fromAccountId,
                         @RequestParam Long toAccountId,
                         @RequestParam BigDecimal amount) {
        log.info("REST request: transfer, fromId={}, toId={}, amount={}", fromAccountId, toAccountId, amount);
        balanceService.transfer(fromAccountId, toAccountId, amount);
    }

    @GetMapping("/{accountId}/balance")
    public AccountDto getBalance(@PathVariable Long accountId) {
        log.info("REST request: getBalance, accountId={}", accountId);
        Account account = balanceService.getAccount(accountId);
        return balanceMapper.accountToDto(account);
    }

    @GetMapping("/{accountId}/transactions")
    public List<TransactionDto> getTransactionsByPeriod(
            @PathVariable Long accountId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        log.info("REST request: getTransactionsByPeriod, accountId={}, startDate={}, endDate={}", accountId, startDate, endDate);
        List<Transaction> transactions = balanceService.getTransactionsByPeriod(accountId, start, end);
        return transactions.stream()
                .map(balanceMapper::transactionToDto)
                .toList();
    }
}
