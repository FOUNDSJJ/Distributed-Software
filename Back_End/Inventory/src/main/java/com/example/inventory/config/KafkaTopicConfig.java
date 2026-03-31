package com.example.inventory.config;

import com.example.inventory.service.InventoryMessageProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic stockDeductTopic() {
        return TopicBuilder.name(InventoryMessageProducer.STOCK_DEDUCT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockResultTopic() {
        return TopicBuilder.name(InventoryMessageProducer.STOCK_RESULT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockRollbackTopic() {
        return TopicBuilder.name(InventoryMessageProducer.STOCK_ROLLBACK_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
