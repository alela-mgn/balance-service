package org.gopoints.balanceservice.service;

import org.gopoints.balanceservice.BalanceServiceApplication;
import org.gopoints.balanceservice.model.Account;
import org.gopoints.balanceservice.model.Transaction;
import org.gopoints.balanceservice.model.exceptions.AccountNotFoundException;
import org.gopoints.balanceservice.repository.AccountRepository;
import org.gopoints.balanceservice.repository.TransactionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Testcontainers
@SpringBootTest(classes = BalanceServiceApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
public class BalanceServiceTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Mock
    private RabbitMQServiceTest rabbitMqService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private AccountRepository accountRepository;

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.2")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private Long accountId;
    private Long toAccountId;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        Account account = new Account();
        account.setUserId(111);
        account.setBalance(BigDecimal.valueOf(1000));
        account = accountRepository.save(account);
        accountId = account.getId();


        Account toAccount = new Account();
        toAccount.setUserId(222); // ID целевого аккаунта
        toAccount.setBalance(BigDecimal.valueOf(500));
        toAccount = accountRepository.save(toAccount);
        toAccountId = toAccount.getId();

        // Создание транзакций для тестов
        Transaction depositTransaction = new Transaction();
        depositTransaction.setAccountId(accountId);
        depositTransaction.setAmount(BigDecimal.valueOf(1000));
        depositTransaction.setOperationType("DEPOSIT");
        depositTransaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(depositTransaction);

        Transaction withdrawTransaction = new Transaction();
        withdrawTransaction.setAccountId(accountId);
        withdrawTransaction.setAmount(BigDecimal.valueOf(200));
        withdrawTransaction.setOperationType("WITHDRAW");
        withdrawTransaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(withdrawTransaction);
    }

    @Test
    @Order(1)
    void testDeposit() {
        BigDecimal depositAmount = BigDecimal.valueOf(500);

        balanceService.deposit(accountId, depositAmount);

        Account updatedAccount = accountRepository.findById(accountId).orElseThrow();
        Assertions.assertEquals(new BigDecimal("1500.00"), updatedAccount.getBalance());
    }

    @Test
    @Order(2)
    void testWithdraw() {
        BigDecimal withdrawAmount = BigDecimal.valueOf(200);

        balanceService.withdraw(accountId, withdrawAmount);

        Account updatedAccount = accountRepository.findById(accountId).orElseThrow();
        Assertions.assertEquals(new BigDecimal("800.00"), updatedAccount.getBalance());

    }

    @Test
    @Order(3)
    void testTransfer() {

        BigDecimal amount = BigDecimal.valueOf(200);

        // Выполняем перевод
        balanceService.transfer(accountId, toAccountId, amount);

        // Проверяем, что баланс на исходном и целевом аккаунте обновился корректно
        Account updatedFromAccount = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        Account updatedToAccount = accountRepository.findById(toAccountId).orElseThrow(() -> new AccountNotFoundException("Account not found: " + toAccountId));

        Assertions.assertEquals(new BigDecimal("800.00"), updatedFromAccount.getBalance());
        Assertions.assertEquals(new BigDecimal("700.00"), updatedToAccount.getBalance());
    }

    @Test
    @Order(4)
    void testGetTransactionsByPeriod() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // Выполняем депозит или вывод, чтобы создать транзакцию
        BigDecimal depositAmount = BigDecimal.valueOf(500);
        balanceService.deposit(accountId, depositAmount);

        // Получаем транзакции за период
        List<Transaction> transactions = balanceService.getTransactionsByPeriod(accountId, startDate, endDate);

        Assertions.assertNotNull(transactions);
        Assertions.assertFalse(transactions.isEmpty());
        Assertions.assertTrue(transactions.stream().anyMatch(t -> "DEPOSIT".equals(t.getOperationType())));
    }

}