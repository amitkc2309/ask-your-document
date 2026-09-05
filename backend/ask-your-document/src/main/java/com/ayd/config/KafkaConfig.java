package com.ayd.config;

import lombok.Data;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration class for Kafka.
 */
@Configuration
@ConfigurationProperties(prefix = "application.kafka")
@Data
public class KafkaConfig {

    private String documentUploadTopic;

    @Bean
    public NewTopic documentUploadTopic() {
        return TopicBuilder.name(documentUploadTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}