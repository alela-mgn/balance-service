package org.gopoints.balanceservice.service;

import lombok.extern.slf4j.Slf4j;
import org.gopoints.balanceservice.model.RabbitMessage;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RabbitMQService {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(RabbitMessage message) {
        rabbitTemplate.convertAndSend("balanceQueue", message);
    }

    @RabbitListener(queues = "balanceQueue")
    public void process(@Payload RabbitMessage message) {

        log.info("Received message from queue: {}", message);
        if ("deposit".equals(message.getOperationType())) {
            log.info("Processing deposit operation for accountId={}, amount={}", message.getAccountId(), message.getAmount());
            //service notification
        } else if ("withdraw".equals(message.getOperationType())) {
            log.info("Processing withdraw operation for accountId={}, amount={}", message.getAccountId(), message.getAmount());
            //service notification
        } else if ("transfer".equals(message.getOperationType()) && message.getTargetAccountId() != null) {
            log.info("Processing transfer operation from accountId={} to targetAccountId={}, amount={}",
                    message.getAccountId(), message.getTargetAccountId(), message.getAmount());
            //service notification
        } else {
            log.warn("Unknown operation type: {}", message.getOperationType());
        }
        log.info("Message processing complete for accountId={}, operationType={}", message.getAccountId(), message.getOperationType());
    }
}
