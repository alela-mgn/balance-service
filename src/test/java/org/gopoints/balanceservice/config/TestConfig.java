//package org.gopoints.balanceservice.config;
//
//import org.gopoints.balanceservice.controller.BalanceController;
//import org.gopoints.balanceservice.mapper.BalanceMapper;
//import org.gopoints.balanceservice.mapper.BalanceMapperImpl;
//import org.gopoints.balanceservice.repository.AccountRepository;
//import org.gopoints.balanceservice.repository.TransactionRepository;
//import org.gopoints.balanceservice.service.BalanceService;
//import org.gopoints.balanceservice.service.RabbitMQService;
//import org.mockito.Mock;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.TestPropertySource;
//
//@TestConfiguration
//@TestPropertySource(locations = "classpath:application-test.properties")
//@Import({BalanceService.class, RabbitMQService.class, BalanceController.class})
//public class TestConfig {
//    @Mock
//    private AccountRepository accountRepository;
//
//    @Mock
//    private TransactionRepository transactionRepository;
//
//    @Mock
//    private RabbitTemplate rabbitTemplate;
//
//    @Mock
//    private BalanceService balanceService;
//
//    @Mock
//    private BalanceMapper balanceMapper;
//
//    @Bean
//    public BalanceService balanceService() {
//        return new BalanceService(accountRepository, transactionRepository, rabbitMQService());
//    }
//
//    @Bean
//    public RabbitMQService rabbitMQService() {
//        return new RabbitMQService(rabbitTemplate, balanceService());
//    }
//
//    @Bean
//    public BalanceController balanceController() {
//        return new BalanceController(balanceService(), balanceMapper());
//    }
//
//    @Bean
//    public BalanceMapper balanceMapper() {
//        return new BalanceMapperImpl();
//    }
//}
