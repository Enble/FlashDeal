package enble.flashdeal.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_CREATED = "order-created";
    public static final String ORDER_CREATED_DLT = "order-created-dlt";
    public static final String ANOMALY_DETECTED = "anomaly-detected";

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedRetry0Topic() {
        return TopicBuilder.name(ORDER_CREATED + "-retry-0")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedRetry1Topic() {
        return TopicBuilder.name(ORDER_CREATED + "-retry-1")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedDltTopic() {
        return TopicBuilder.name(ORDER_CREATED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic anomalyDetectedTopic() {
        return TopicBuilder.name(ANOMALY_DETECTED)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
