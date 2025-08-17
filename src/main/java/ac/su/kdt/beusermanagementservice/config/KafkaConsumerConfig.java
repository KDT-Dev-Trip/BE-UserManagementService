package ac.su.kdt.beusermanagementservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        // Kafka Consumer의 설정을 담을 Map 객체를 생성
        Map<String, Object> props = new HashMap<>();
        // Kafka 브로커 주소 설정
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // 컨슈머 그룹 ID 설정
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // 메시지 키 역직렬화 클래스 설정
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // 메시지 값 역직렬화 클래스 설정 (JSON)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // 기본 타입을 지정
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "ac.su.kdt.beusermanagementservice.dto.UserSignedUpEventDTO");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, "false");

        // 설정 정보를 담은 DefaultKafkaConsumerFactory 객체를 생성하여 반환
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        // KafkaListener가 메시지를 수신할 때 사용될 컨테이너를 생성하는 팩토리
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}