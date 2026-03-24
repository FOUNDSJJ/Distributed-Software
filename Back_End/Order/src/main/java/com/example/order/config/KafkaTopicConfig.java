package com.example.order.config;

import com.example.order.service.SeckillOrderProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic seckillOrderTopic() {
        return TopicBuilder.name(SeckillOrderProducer.TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
