package org.gopoints.balanceservice.service;

import org.gopoints.balanceservice.BalanceServiceApplication;
import org.gopoints.balanceservice.model.Account;
import org.gopoints.balanceservice.model.Transaction;
import org.gopoints.balanceservice.model.exceptions.AccountNotFoundException;
import org.gopoints.balanceservice.model.exceptions.InsufficientFundsException;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
    @Transactional
    void testDeposit() {
        BigDecimal depositAmount = BigDecimal.valueOf(500);

        balanceService.deposit(accountId, depositAmount);

        Account updatedAccount = accountRepository.findByIdWithLock(accountId).orElseThrow();
        Assertions.assertEquals(new BigDecimal("1500"), updatedAccount.getBalance());
    }

    @Test
    @Transactional
    void testDepositNegativeAmount() {
        BigDecimal depositAmount = BigDecimal.valueOf(-500);  // Негативный депозит

        assertThrows(IllegalArgumentException.class, () -> {
            balanceService.deposit(accountId, depositAmount);  // Метод должен выбросить исключение
        });
    }

    @Test
    @Transactional
    void testDepositAccountNotFound() {
        Long invalidAccountId = 999L;
        BigDecimal depositAmount = BigDecimal.valueOf(500);

        assertThrows(AccountNotFoundException.class, () -> {
            balanceService.deposit(invalidAccountId, depositAmount);  // Метод должен выбросить исключение
        });
    }

    @Test
    @Transactional
    void testWithdraw() {
        BigDecimal withdrawAmount = BigDecimal.valueOf(200);

        balanceService.withdraw(accountId, withdrawAmount);

        Account updatedAccount = accountRepository.findByIdWithLock(accountId).orElseThrow();
        Assertions.assertEquals(new BigDecimal("800"), updatedAccount.getBalance());
    }

    @Test
    @Transactional
    void testWithdrawNegativeAmount() {
        BigDecimal withdrawAmount = BigDecimal.valueOf(-200);  // Негативный вывод

        assertThrows(IllegalArgumentException.class, () -> {
            balanceService.withdraw(accountId, withdrawAmount);  // Метод должен выбросить исключение
        });
    }

    @Test
    @Transactional
    void testWithdrawInsufficientFunds() {
        BigDecimal withdrawAmount = BigDecimal.valueOf(10000);  // Сумма превышает баланс

        assertThrows(InsufficientFundsException.class, () -> {
            balanceService.withdraw(accountId, withdrawAmount);  // Метод должен выбросить исключение
        });
    }

    @Test
    @Transactional
    void testWithdrawAccountNotFound() {
        Long invalidAccountId = 999L;  // Некорректный ID счета
        BigDecimal withdrawAmount = BigDecimal.valueOf(200);

        assertThrows(AccountNotFoundException.class, () -> {
            balanceService.withdraw(invalidAccountId, withdrawAmount);  // Метод должен выбросить исключение
        });
    }

    @Test
    @Transactional
    void testTransfer() {
        BigDecimal amount = BigDecimal.valueOf(200);

        // Выполняем перевод
        balanceService.transfer(accountId, toAccountId, amount);

        // Проверяем, что баланс на исходном и целевом аккаунте обновился корректно
        Account updatedFromAccount = accountRepository.findByIdWithLock(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        Account updatedToAccount = accountRepository.findByIdWithLock(toAccountId).orElseThrow(() -> new AccountNotFoundException("Account not found: " + toAccountId));

        Assertions.assertEquals(new BigDecimal("800"), updatedFromAccount.getBalance());
        Assertions.assertEquals(new BigDecimal("700"), updatedToAccount.getBalance());
    }

    @Test
    @Transactional
    void testTransferNegativeAmount() {
        BigDecimal amount = BigDecimal.valueOf(-200);  // Негативный перевод

        assertThrows(IllegalArgumentException.class, () -> {
            balanceService.transfer(accountId, toAccountId, amount);  // Метод должен выбросить исключение
        });
    }

    @Test
    @Transactional
    void testTransferInsufficientFunds() {
        BigDecimal amount = BigDecimal.valueOf(10000);  // Сумма превышает баланс

        assertThrows(InsufficientFundsException.class, () -> {
            balanceService.transfer(accountId, toAccountId, amount);  // Метод должен выбросить исключение
        });
    }

    @Test
    @Transactional
    void testTransferAccountNotFound() {
        Long invalidAccountId = 999L;  // Некорректный ID счета
        BigDecimal amount = BigDecimal.valueOf(200);

        assertThrows(AccountNotFoundException.class, () -> {
            balanceService.transfer(invalidAccountId, toAccountId, amount);  // Метод должен выбросить исключение
        });
    }

    @Test
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