package org.gopoints.balanceservice.service;

import org.gopoints.balanceservice.model.RabbitMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RabbitMQServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private RabbitMQService rabbitMQService;


    @Test
    void sendDepositMessageToQueue() {
        RabbitMessage depositMessage = new RabbitMessage(1L, BigDecimal.valueOf(100), "deposit", null);

        rabbitMQService.sendMessage(depositMessage);

        ArgumentCaptor<RabbitMessage> captor = ArgumentCaptor.forClass(RabbitMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(eq("balanceQueue"), captor.capture());
        assertEquals(depositMessage, captor.getValue());
    }

    @Test
    void sendWithdrawMessageToQueue() {
        RabbitMessage withdrawMessage = new RabbitMessage(1L, BigDecimal.valueOf(50), "withdraw", null);

        rabbitMQService.sendMessage(withdrawMessage);

        ArgumentCaptor<RabbitMessage> captor = ArgumentCaptor.forClass(RabbitMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(eq("balanceQueue"), captor.capture());
        assertEquals(withdrawMessage, captor.getValue());
    }

    @Test
    void sendTransaction_shouldLogMessageBeforeSending() {
        RabbitMessage message = new RabbitMessage(1L, BigDecimal.valueOf(100), "deposit", null);

        rabbitMQService.sendMessage(message);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("balanceQueue"), eq(message));
    }

    @Test
    void sendTransaction_shouldHandleExceptionWhenSendingFails() {
        RabbitMessage message = new RabbitMessage(1L, BigDecimal.valueOf(100), "deposit", null);

        doThrow(new RuntimeException("Sending failed"))
                .when(rabbitTemplate)
                .convertAndSend(eq("balanceQueue"), any(RabbitMessage.class));

        assertThrows(RuntimeException.class, () -> rabbitMQService.sendMessage(message));

        verify(rabbitTemplate, times(1)).convertAndSend(eq("balanceQueue"), eq(message));
    }

    @Test
    void testMessageConversion() {
        RabbitMessage message = new RabbitMessage(2L, BigDecimal.valueOf(500), "deposit", null);
        MessageConverter converter = new Jackson2JsonMessageConverter();
        Message amqpMessage = converter.toMessage(message, new MessageProperties());
        RabbitMessage result = (RabbitMessage) converter.fromMessage(amqpMessage);
        assertEquals(message, result);
    }
}
