package com.example.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic stockDeductTopic() {
        return TopicBuilder.name(com.example.order.service.SeckillOrderProducer.STOCK_DEDUCT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockResultTopic() {
        return TopicBuilder.name(com.example.order.service.SeckillOrderProducer.STOCK_RESULT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockRollbackTopic() {
        return TopicBuilder.name(com.example.order.service.SeckillOrderProducer.STOCK_ROLLBACK_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentRequestTopic() {
        return TopicBuilder.name(com.example.order.service.SeckillOrderProducer.PAYMENT_REQUEST_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentResultTopic() {
        return TopicBuilder.name(com.example.order.service.SeckillOrderProducer.PAYMENT_RESULT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
