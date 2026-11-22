package com.payroute.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroute.dto.event.PaymentEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payroute.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${payroute.kafka.topic:payroute.payment.events}", groupId = "${spring.kafka.consumer.group-id:payroute-group}", autoStartup = "${payroute.kafka.consumer.auto-startup:false}")
    public void consume(String messagePayload) {
        try {
            PaymentEventMessage event = objectMapper.readValue(messagePayload, PaymentEventMessage.class);
            log.info(">>> [Kafka Consumer] Received event: type={}, txId={}, status={}, provider={}",
                    event.getEventType(), event.getTransactionId(), event.getTransactionStatus(), event.getProviderCode());
            // Simulate downstream async actions (e.g. merchant webhook dispatch, fraud telemetry, analytics ingestion)
        } catch (Exception e) {
            log.error(">>> [Kafka Consumer] Failed to process message payload: {}", messagePayload, e);
        }
    }
}
