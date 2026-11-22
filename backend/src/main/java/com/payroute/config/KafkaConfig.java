package com.payroute.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${payroute.kafka.topic:payroute.payment.events}")
    private String topicName;

    @Bean
    @ConditionalOnProperty(name = "payroute.kafka.topic-creation.enabled", havingValue = "true", matchIfMissing = false)
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
