package ac.su.kdt.beusermanagementservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    // 카프카 서버 주소
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>(); // 설정을 담을 Map을 생성
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // 접속할 카프카 서버 주소를 설정
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // 메시지 키를 문자열로 직렬화
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // 메시지 본문(DTO 객체)을 JSON 문자열로 직렬화
        return new DefaultKafkaProducerFactory<>(props); // 설정을 담아 프로듀서 팩토리를 생성하여 반환
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory()); // 위에서 만든 프로듀서 팩토리를 사용하여 KafkaTemplate을 생성
    }
}