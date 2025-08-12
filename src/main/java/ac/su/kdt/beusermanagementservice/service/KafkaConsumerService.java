package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.dto.SubscriptionChangedEventDTO;
import ac.su.kdt.beusermanagementservice.dto.UserSignedUpEventDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
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

    // subscription-events 토픽을 구독하여 플랜 변경 처리
    @KafkaListener(topics = "subscription-events", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeSubscriptionChangedEvent(@Payload(required = false) SubscriptionChangedEventDTO event) {
        if (event == null) { // 비어있는 메시지 무시
            logger.warn("Null 또는 비어있는 SubscriptionChangedEvent 메시지를 수신하여 무시합니다.");
            return;
        }

        logger.info("Kafka로부터 SubscriptionChangedEvent 수신: {}", event); // 이벤트 수신 로그 기록
        try {
            userService.processSubscriptionChange(event); // UserService에 이벤트 처리 전달
        } catch (Exception e) {
            logger.error("SubscriptionChangedEvent 처리 중 오류 발생: {}", event, e); // 에러 발생 시 로그 기록
        }
    }
}