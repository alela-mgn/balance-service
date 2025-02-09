package org.gopoints.balanceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gopoints.balanceservice.model.Account;
import org.gopoints.balanceservice.model.RabbitMessage;
import org.gopoints.balanceservice.model.Transaction;
import org.gopoints.balanceservice.model.exceptions.AccountNotFoundException;
import org.gopoints.balanceservice.model.exceptions.InsufficientFundsException;
import org.gopoints.balanceservice.repository.AccountRepository;
import org.gopoints.balanceservice.repository.TransactionRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = {"balance"})
public class BalanceService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RabbitMQService rabbitMqService;

    @Transactional(readOnly = true)
    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    @Transactional
    public void deposit(Long accountId, BigDecimal amount) {
        log.info("Depositing {} to account {}", amount, accountId);

        Account account = getAccount(accountId);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .operationType("DEPOSIT")
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        log.debug("Deposit complete. Account {} new balance: {}", accountId, account.getBalance());

        rabbitMqService.sendMessage(new RabbitMessage(accountId, amount, "deposit", null));
    }

    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        log.info("Withdrawing {} from account {}", amount, accountId);

        Account account = getAccount(accountId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough balance on account " + accountId);
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .operationType("WITHDRAW")
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        log.debug("Withdraw complete. Account {} new balance: {}", accountId, account.getBalance());

        rabbitMqService.sendMessage(new RabbitMessage(accountId, amount, "withdraw", null));
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        log.info("Transferring {} from account {} to account {}", amount, fromAccountId, toAccountId);
        withdraw(fromAccountId, amount);
        deposit(toAccountId, amount);

        Transaction transaction = Transaction.builder()
                .accountId(fromAccountId)
                .amount(amount)
                .operationType("TRANSFER")
                .timestamp(LocalDateTime.now())
                .build();

        // Сохраняем транзакцию в базе данных
        transactionRepository.save(transaction);
        log.info("Transfer complete from {} to {}", fromAccountId, toAccountId);

        rabbitMqService.sendMessage(new RabbitMessage(fromAccountId, amount, "transfer", toAccountId));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByPeriod(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByAccountIdAndTimestampBetween(accountId, startDate, endDate);
    }
}
