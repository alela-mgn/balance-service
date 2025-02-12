package org.gopoints.balanceservice.controller;

import org.gopoints.balanceservice.BalanceServiceApplication;
import org.gopoints.balanceservice.repository.AccountRepository;
import org.gopoints.balanceservice.repository.TransactionRepository;
import org.gopoints.balanceservice.model.Account;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = BalanceServiceApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

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
        toAccount.setUserId(222);
        toAccount.setBalance(BigDecimal.valueOf(500));
        toAccount = accountRepository.save(toAccount);
        toAccountId = toAccount.getId();
    }

    @Test
    void testDeposit() throws Exception {
        BigDecimal amount = BigDecimal.valueOf(500);

        mockMvc.perform(post("/accounts/{accountId}/deposit", accountId)
                        .param("amount", amount.toString()))
                .andExpect(status().isOk());

        Account updatedAccount = accountRepository.findById(accountId).orElseThrow();
        Assertions.assertEquals(new BigDecimal("1500.00"), updatedAccount.getBalance());
    }

    @Test
    void testDepositNegativeAmount() throws Exception {
        BigDecimal amount = BigDecimal.valueOf(-100); // Невозможный депозит

        mockMvc.perform(post("/accounts/{accountId}/deposit", accountId)
                        .param("amount", amount.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(result -> Assertions.assertInstanceOf(ResponseStatusException.class, result.getResolvedException()));
    }


    @Test
    void testWithdraw() throws Exception {
        BigDecimal amount = BigDecimal.valueOf(100);

        mockMvc.perform(post("/accounts/{accountId}/withdraw", accountId)
                        .param("amount", amount.toString()))
                .andExpect(status().isOk());

        Account updatedAccount = accountRepository.findById(accountId).orElseThrow();
        Assertions.assertEquals(new BigDecimal("900.00"), updatedAccount.getBalance());
    }

    @Test
    void testTransfer() throws Exception {
        BigDecimal amount = BigDecimal.valueOf(200);

        mockMvc.perform(post("/accounts/transfer")
                        .param("fromAccountId", accountId.toString())
                        .param("toAccountId", toAccountId.toString())
                        .param("amount", amount.toString()))
                .andExpect(status().isOk());

        // Проверяем, что баланс на исходном и целевом аккаунте обновился корректно
        Account updatedFromAccount = accountRepository.findById(accountId).orElseThrow();
        Account updatedToAccount = accountRepository.findById(toAccountId).orElseThrow();

        Assertions.assertEquals(new BigDecimal("800.00"), updatedFromAccount.getBalance());
        Assertions.assertEquals(new BigDecimal("700.00"), updatedToAccount.getBalance());
    }

    @Test
    void testGetBalance() throws Exception {
        // Выполняем запрос на получение баланса
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.userId").value(111))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void testGetTransactionsByPeriod() throws Exception {
        // Даты для фильтрации транзакций
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // Запрос на получение транзакций за период
        mockMvc.perform(get("/accounts/{accountId}/transactions", accountId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk());
    }
}
