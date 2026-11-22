package com.payroute.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroute.dto.event.PaymentEventMessage;
import com.payroute.entity.PaymentEvent;
import com.payroute.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final PaymentEventRepository paymentEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${payroute.kafka.topic:payroute.payment.events}")
    private String topic;

    @Value("${payroute.kafka.enabled:true}")
    private boolean kafkaEnabled;

    /**
     * Records the event persistently to the database and dispatches it asynchronously to Kafka.
     */
    public PaymentEvent publish(PaymentEventMessage message) {
        if (message.getEventId() == null) {
            message.setEventId(UUID.randomUUID());
        }

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            jsonPayload = "{\"error\":\"Serialization error: " + e.getMessage() + "\"}";
        }

        PaymentEvent event = PaymentEvent.builder()
                .id(message.getEventId())
                .transactionId(message.getTransactionId())
                .eventType(message.getEventType())
                .payload(jsonPayload)
                .publishedToKafka(false)
                .build();

        PaymentEvent saved = paymentEventRepository.save(event);
        log.info("Persisted event to Event Store: type={}, txId={}", message.getEventType(), message.getTransactionId());

        dispatchToKafkaAsync(saved, message.getTransactionId().toString(), jsonPayload);
        return saved;
    }

    @Async
    public void dispatchToKafkaAsync(PaymentEvent savedEvent, String key, String jsonPayload) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.debug("Kafka dispatch skipped (disabled or template not present). Event logged in DB.");
            return;
        }

        try {
            kafkaTemplate.send(topic, key, jsonPayload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka dispatch SUCCESS: topic={}, offset={}",
                                    topic, result.getRecordMetadata().offset());
                            savedEvent.setPublishedToKafka(true);
                            paymentEventRepository.save(savedEvent);
                        } else {
                            log.warn("Kafka dispatch FAILED (event stored in DB for replay): {}", ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.warn("Kafka dispatch exception: {}", ex.getMessage());
        }
    }
}
