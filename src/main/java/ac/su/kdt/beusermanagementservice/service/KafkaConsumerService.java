package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.dto.AccountLockedEventDTO;
import ac.su.kdt.beusermanagementservice.dto.LoginFailedEventDTO;
import ac.su.kdt.beusermanagementservice.dto.SubscriptionChangedEventDTO;
import ac.su.kdt.beusermanagementservice.dto.UserLoggedOutEventDTO;
import ac.su.kdt.beusermanagementservice.dto.UserSignedUpEventDTO;
import ac.su.kdt.beusermanagementservice.dto.UserSyncEventDTO;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    // 실제 비즈니스 로직을 처리할 UserService에 대한 의존성을 주입 받음
    private final UserService userService;

    @KafkaListener(topics = "auth-events", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAuthEvents(ConsumerRecord<String, Object> record) {
        logger.info("=== Kafka 이벤트 수신 시작 ===");
        logger.info("ConsumerRecord: topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());
        
        Object rawValue = record.value();
        logger.info("Raw value: {}", rawValue);
        logger.info("Raw value 타입: {}", rawValue != null ? rawValue.getClass().getName() : "null");

        try {
            // null 체크
            if (rawValue == null) {
                logger.warn("수신된 이벤트가 null입니다");
                return;
            }
            
            // ErrorHandlingDeserializer로 인한 역직렬화 실패 체크
            if (rawValue instanceof org.springframework.kafka.support.serializer.DeserializationException) {
                logger.error("역직렬화 실패 이벤트 수신: {}", rawValue);
                return;
            }
            
            // Map 형태로 받아온 경우 (가장 가능성 높음)
            if (rawValue instanceof java.util.Map<?, ?> map) {
                logger.info("Map 형태의 이벤트를 DTO로 변환 시도: {}", map);
                handleMapEvent(map);
            }
            // 직접 DTO로 받아온 경우
            else if (rawValue instanceof UserSignedUpEventDTO signedUpEvent) {
                logger.info("직접 UserSignedUpEventDTO 처리: {}", signedUpEvent);
                userService.registerNewUser(signedUpEvent);
            }
            else if (rawValue instanceof UserSyncEventDTO syncEvent) {
                logger.info("직접 UserSyncEventDTO 처리: {}", syncEvent);
                userService.processSyncEvent(syncEvent);
            }
            // String으로 받아온 경우 JSON 파싱
            else if (rawValue instanceof String jsonString) {
                logger.info("String 형태의 JSON 파싱 시도: {}", jsonString);
                // 향후 ObjectMapper 파싱 구현 가능
                logger.warn("String JSON 파싱은 아직 구현되지 않았습니다");
            }
            else {
                logger.warn("처리할 수 없는 이벤트 타입: {}", rawValue.getClass().getSimpleName());
                logger.warn("이벤트 내용: {}", rawValue);
            }
            
        } catch (Exception e) {
            logger.error("auth-events 처리 중 예외 발생", e);
            logger.error("문제가 된 이벤트: {}", rawValue);
        }
        
        logger.info("=== Kafka 이벤트 수신 완료 ===");
    }
    
    /**
     * Map 형태의 이벤트를 처리하는 헬퍼 메서드
     */
    private void handleMapEvent(Map<?, ?> map) {
        try {
            // event_type 필드로 이벤트 타입 확인
            String eventType = (String) map.get("event_type");
            if (eventType != null) {
                logger.info("Auth 이벤트 타입 확인: {}", eventType);
                
                switch (eventType) {
                    case "auth.user-signed-up" -> {
                        UserSignedUpEventDTO convertedEvent = convertMapToUserSignedUpEvent(map);
                        if (convertedEvent != null && convertedEvent.authUserId() != null) {
                            logger.info("사용자 가입 이벤트 처리: {}", convertedEvent.email());
                            userService.registerNewUser(convertedEvent);
                        }
                    }
                    case "auth.user-logged-out" -> {
                        UserLoggedOutEventDTO logoutEvent = convertMapToUserLoggedOutEvent(map);
                        if (logoutEvent != null) {
                            logger.info("사용자 로그아웃 이벤트 처리: {}", logoutEvent.email());
                            userService.processUserLogoutEvent(logoutEvent);
                        }
                    }
                    case "auth.login-failed" -> {
                        LoginFailedEventDTO loginFailedEvent = convertMapToLoginFailedEvent(map);
                        if (loginFailedEvent != null) {
                            logger.info("로그인 실패 이벤트 처리: {}", loginFailedEvent.email());
                            userService.processLoginFailedEvent(loginFailedEvent);
                        }
                    }
                    case "auth.account-locked" -> {
                        AccountLockedEventDTO accountLockedEvent = convertMapToAccountLockedEvent(map);
                        if (accountLockedEvent != null) {
                            logger.info("계정 잠금 이벤트 처리: {}", accountLockedEvent.email());
                            userService.processAccountLockedEvent(accountLockedEvent);
                        }
                    }
                    default -> logger.warn("처리할 수 없는 Auth 이벤트 타입: {}", eventType);
                }
            } 
            // 기존 UserSyncEvent 처리 (eventType 필드 사용)
            else if (map.containsKey("eventType")) {
                String legacyEventType = (String) map.get("eventType");
                logger.info("Legacy UserSyncEvent 타입으로 처리 시도: eventType={}", legacyEventType);
                
                if ("FULL_SYNC".equals(legacyEventType)) {
                    handleFullSyncEvent(map);
                } else {
                    logger.warn("알 수 없는 eventType: {}", legacyEventType);
                }
            } 
            // 기본적으로 UserSignedUpEvent로 처리 (하위 호환성)
            else {
                UserSignedUpEventDTO convertedEvent = convertMapToUserSignedUpEvent(map);
                if (convertedEvent != null && convertedEvent.authUserId() != null) {
                    logger.info("Map을 UserSignedUpEventDTO로 변환 성공: {}", convertedEvent);
                    userService.registerNewUser(convertedEvent);
                } else {
                    logger.error("변환 실패 또는 authUserId가 null: {}", convertedEvent);
                }
            }
        } catch (Exception e) {
            logger.error("Map 이벤트 처리 중 오류", e);
        }
    }

    /**
     * FULL_SYNC 이벤트를 처리하는 헬퍼 메서드
     */
    private void handleFullSyncEvent(Map<?, ?> map) {
        try {
            Object usersObject = map.get("users");
            if (usersObject instanceof java.util.List<?> usersList) {
                logger.info("FULL_SYNC 이벤트 처리 시작: {} 명의 사용자", usersList.size());
                
                int syncCount = 0;
                for (Object userObj : usersList) {
                    if (userObj instanceof Map<?, ?> userMap) {
                        try {
                            UserSignedUpEventDTO userEvent = convertMapToUserSignedUpEvent(userMap);
                            if (userEvent != null && userEvent.authUserId() != null) {
                                userService.registerNewUser(userEvent);
                                syncCount++;
                                logger.debug("사용자 동기화 성공: authUserId={}, email={}", userEvent.authUserId(), userEvent.email());
                            } else {
                                logger.warn("사용자 변환 실패: {}", userMap);
                            }
                        } catch (Exception e) {
                            logger.error("개별 사용자 처리 중 오류: {}", userMap, e);
                        }
                    }
                }
                
                logger.info("FULL_SYNC 이벤트 처리 완료: {}/{}명의 사용자 동기화", syncCount, usersList.size());
            } else {
                logger.error("users 필드가 List 타입이 아닙니다: {}", usersObject);
            }
        } catch (Exception e) {
            logger.error("FULL_SYNC 이벤트 처리 중 오류", e);
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
    
    /**
     * Map을 UserLoggedOutEventDTO로 변환하는 헬퍼 메서드
     */
    private UserLoggedOutEventDTO convertMapToUserLoggedOutEvent(Map<?, ?> map) {
        try {
            String eventType = (String) map.get("event_type");
            String eventId = (String) map.get("event_id");
            String authUserId = (String) map.get("auth_user_id");
            String email = (String) map.get("email");
            String sessionId = (String) map.get("sessionId");
            String ipAddress = (String) map.get("ipAddress");
            String userAgent = (String) map.get("userAgent");
            String logoutReason = (String) map.get("logoutReason");
            
            // timestamp 처리
            Long timestampLong = null;
            Object timestampObj = map.get("timestamp");
            if (timestampObj instanceof Number) {
                timestampLong = ((Number) timestampObj).longValue();
            }
            
            // logoutTime 처리
            LocalDateTime logoutTime = LocalDateTime.now();
            Object logoutTimeObj = map.get("logoutTime");
            if (logoutTimeObj instanceof String) {
                try {
                    logoutTime = LocalDateTime.parse((String) logoutTimeObj);
                } catch (Exception e) {
                    logger.warn("logoutTime 파싱 실패: {}", logoutTimeObj);
                }
            }
            
            return new UserLoggedOutEventDTO(
                eventType,
                eventId,
                authUserId,
                email,
                sessionId,
                ipAddress,
                userAgent,
                logoutReason,
                logoutTime,
                timestampLong != null ? timestampLong : System.currentTimeMillis()
            );
        } catch (Exception e) {
            logger.error("Map을 UserLoggedOutEventDTO로 변환 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * Map을 LoginFailedEventDTO로 변환하는 헬퍼 메서드
     */
    private LoginFailedEventDTO convertMapToLoginFailedEvent(Map<?, ?> map) {
        try {
            String eventType = (String) map.get("event_type");
            String eventId = (String) map.get("event_id");
            String email = (String) map.get("email");
            String failureReason = (String) map.get("failureReason");
            String ipAddress = (String) map.get("ipAddress");
            String userAgent = (String) map.get("userAgent");
            
            // attemptCount 처리
            Integer attemptCount = null;
            Object attemptCountObj = map.get("attemptCount");
            if (attemptCountObj instanceof Number) {
                attemptCount = ((Number) attemptCountObj).intValue();
            }
            
            // timestamp 처리
            Long timestampLong = null;
            Object timestampObj = map.get("timestamp");
            if (timestampObj instanceof Number) {
                timestampLong = ((Number) timestampObj).longValue();
            }
            
            // failedAt 처리
            LocalDateTime failedAt = LocalDateTime.now();
            Object failedAtObj = map.get("failedAt");
            if (failedAtObj instanceof String) {
                try {
                    failedAt = LocalDateTime.parse((String) failedAtObj);
                } catch (Exception e) {
                    logger.warn("failedAt 파싱 실패: {}", failedAtObj);
                }
            }
            
            return new LoginFailedEventDTO(
                eventType,
                eventId,
                email,
                failureReason,
                ipAddress,
                userAgent,
                attemptCount,
                failedAt,
                timestampLong != null ? timestampLong : System.currentTimeMillis()
            );
        } catch (Exception e) {
            logger.error("Map을 LoginFailedEventDTO로 변환 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * Map을 AccountLockedEventDTO로 변환하는 헬퍼 메서드
     */
    private AccountLockedEventDTO convertMapToAccountLockedEvent(Map<?, ?> map) {
        try {
            String eventType = (String) map.get("event_type");
            String eventId = (String) map.get("event_id");
            String authUserId = (String) map.get("auth_user_id");
            String email = (String) map.get("email");
            String lockReason = (String) map.get("lockReason");
            String ipAddress = (String) map.get("ipAddress");
            
            // lockDurationMinutes 처리
            Integer lockDurationMinutes = null;
            Object lockDurationObj = map.get("lockDurationMinutes");
            if (lockDurationObj instanceof Number) {
                lockDurationMinutes = ((Number) lockDurationObj).intValue();
            }
            
            // timestamp 처리
            Long timestampLong = null;
            Object timestampObj = map.get("timestamp");
            if (timestampObj instanceof Number) {
                timestampLong = ((Number) timestampObj).longValue();
            }
            
            // lockedAt, unlocksAt 처리
            LocalDateTime lockedAt = LocalDateTime.now();
            LocalDateTime unlocksAt = LocalDateTime.now().plusMinutes(lockDurationMinutes != null ? lockDurationMinutes : 60);
            
            Object lockedAtObj = map.get("lockedAt");
            if (lockedAtObj instanceof String) {
                try {
                    lockedAt = LocalDateTime.parse((String) lockedAtObj);
                } catch (Exception e) {
                    logger.warn("lockedAt 파싱 실패: {}", lockedAtObj);
                }
            }
            
            Object unlocksAtObj = map.get("unlocksAt");
            if (unlocksAtObj instanceof String) {
                try {
                    unlocksAt = LocalDateTime.parse((String) unlocksAtObj);
                } catch (Exception e) {
                    logger.warn("unlocksAt 파싱 실패: {}", unlocksAtObj);
                }
            }
            
            return new AccountLockedEventDTO(
                eventType,
                eventId,
                authUserId,
                email,
                lockReason,
                lockDurationMinutes,
                lockedAt,
                unlocksAt,
                ipAddress,
                timestampLong != null ? timestampLong : System.currentTimeMillis()
            );
        } catch (Exception e) {
            logger.error("Map을 AccountLockedEventDTO로 변환 중 오류 발생", e);
            return null;
        }
    }

    /**
     * Map을 UserSignedUpEventDTO로 변환하는 헬퍼 메서드
     */
    private UserSignedUpEventDTO convertMapToUserSignedUpEvent(Map<?, ?> map) {
        try {
            String authUserId = (String) map.get("authUserId");
            String email = (String) map.get("email");
            String name = (String) map.get("name");
            String planType = (String) map.get("planType");
            String source = (String) map.get("source");
            String socialProvider = (String) map.get("socialProvider");
            String ipAddress = (String) map.get("ipAddress");
            
            // timestamp 처리
            Long timestampLong = null;
            Object timestampObj = map.get("timestamp");
            if (timestampObj instanceof Number) {
                timestampLong = ((Number) timestampObj).longValue();
            }
            
            // signupTimestamp 처리
            LocalDateTime signupTimestamp = null;
            Object signupTimestampObj = map.get("signupTimestamp");
            if (signupTimestampObj instanceof String) {
                try {
                    signupTimestamp = LocalDateTime.parse((String) signupTimestampObj);
                } catch (Exception e) {
                    logger.warn("signupTimestamp 파싱 실패: {}", signupTimestampObj);
                    signupTimestamp = LocalDateTime.now();
                }
            } else if (signupTimestampObj instanceof java.util.List<?>) {
                // [2025, 8, 28, 10, 8, 9, 119378000] 형태의 배열 처리
                try {
                    java.util.List<?> timestampArray = (java.util.List<?>) signupTimestampObj;
                    if (timestampArray.size() >= 6) {
                        int year = ((Number) timestampArray.get(0)).intValue();
                        int month = ((Number) timestampArray.get(1)).intValue();
                        int day = ((Number) timestampArray.get(2)).intValue();
                        int hour = ((Number) timestampArray.get(3)).intValue();
                        int minute = ((Number) timestampArray.get(4)).intValue();
                        int second = ((Number) timestampArray.get(5)).intValue();
                        int nano = timestampArray.size() > 6 ? ((Number) timestampArray.get(6)).intValue() : 0;
                        
                        signupTimestamp = LocalDateTime.of(year, month, day, hour, minute, second, nano);
                        logger.debug("배열 형태 signupTimestamp 파싱 성공: {}", signupTimestamp);
                    } else {
                        logger.warn("signupTimestamp 배열 크기 부족: {}", timestampArray);
                        signupTimestamp = LocalDateTime.now();
                    }
                } catch (Exception e) {
                    logger.warn("signupTimestamp 배열 파싱 실패: {}", signupTimestampObj, e);
                    signupTimestamp = LocalDateTime.now();
                }
            } else {
                logger.warn("signupTimestamp 타입을 처리할 수 없습니다: {} (타입: {})", 
                    signupTimestampObj, signupTimestampObj != null ? signupTimestampObj.getClass() : "null");
                signupTimestamp = LocalDateTime.now();
            }
            
            return new UserSignedUpEventDTO(
                authUserId,
                email, 
                name,
                planType,
                signupTimestamp,
                source,
                socialProvider,
                ipAddress,
                timestampLong != null ? timestampLong : System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error("Map을 UserSignedUpEventDTO로 변환 중 오류 발생", e);
            return null;
        }
    }
}