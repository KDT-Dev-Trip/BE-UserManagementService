package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.dto.UserSignedUpEventDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    // 실제 비즈니스 로직을 처리할 UserService에 대한 의존성을 주입 받음
    private final UserService userService;

    @KafkaListener(topics = "auth-events", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeUserSignedUpEvent(UserSignedUpEventDTO event) {
        logger.info("Kafka로부터 UserSignedUpEvent 수신: {}", event);

        try {
            userService.registerNewUser(event);
        } catch (Exception e) {
            logger.error("UserSignedUpEvent 처리 중 오류 발생: {}", event, e);
        }
    }
}