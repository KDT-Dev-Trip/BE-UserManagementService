package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // User Service 전용 토픽명
    private static final String USER_EVENTS_TOPIC = "user-events";
    private static final String AUTH_EVENTS_TOPIC = "auth-events"; // 팀 이벤트용
    
    /**
     * 사용자 프로필 업데이트 이벤트 발행
     */
    public void publishUserProfileUpdatedEvent(UserProfileUpdatedEventDTO event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(USER_EVENTS_TOPIC, event.userId().toString(), event);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish UserProfileUpdatedEvent for userId: {}", 
                             event.userId(), exception);
                } else {
                    log.info("Successfully published UserProfileUpdatedEvent for userId: {}", 
                             event.userId());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing UserProfileUpdatedEvent for userId: {}", 
                     event.userId(), e);
        }
    }
    
    /**
     * 사용자 프로필 이미지 변경 이벤트 발행
     */
    public void publishUserProfileImageChangedEvent(UserProfileImageChangedEventDTO event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(USER_EVENTS_TOPIC, event.userId().toString(), event);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish UserProfileImageChangedEvent for userId: {}", 
                             event.userId(), exception);
                } else {
                    log.info("Successfully published UserProfileImageChangedEvent for userId: {}", 
                             event.userId());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing UserProfileImageChangedEvent for userId: {}", 
                     event.userId(), e);
        }
    }
    
    /**
     * 사용자 설정 변경 이벤트 발행
     */
    public void publishUserSettingsChangedEvent(UserSettingsChangedEventDTO event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(USER_EVENTS_TOPIC, event.userId().toString(), event);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish UserSettingsChangedEvent for userId: {}", 
                             event.userId(), exception);
                } else {
                    log.info("Successfully published UserSettingsChangedEvent for userId: {}", 
                             event.userId());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing UserSettingsChangedEvent for userId: {}", 
                     event.userId(), e);
        }
    }
    
    /**
     * 팀 생성 이벤트 발행 (auth-events 토픽으로)
     */
    public void publishTeamCreatedEvent(TeamCreatedEventDTO event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(AUTH_EVENTS_TOPIC, event.teamId().toString(), event);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish TeamCreatedEvent for teamId: {}", 
                             event.teamId(), exception);
                } else {
                    log.info("Successfully published TeamCreatedEvent for teamId: {}", 
                             event.teamId());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing TeamCreatedEvent for teamId: {}", 
                     event.teamId(), e);
        }
    }
    
    /**
     * 팀 멤버 추가 이벤트 발행 (auth-events 토픽으로)
     */
    public void publishTeamMemberAddedEvent(TeamMemberAddedEventDTO event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(AUTH_EVENTS_TOPIC, event.authUserId(), event);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish TeamMemberAddedEvent for userId: {}", 
                             event.authUserId(), exception);
                } else {
                    log.info("Successfully published TeamMemberAddedEvent for userId: {}", 
                             event.authUserId());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing TeamMemberAddedEvent for userId: {}", 
                     event.authUserId(), e);
        }
    }
    
    // 편의 메서드들
    public void publishProfileUpdated(Long userId, String authUserId, String email, String name, String phone, String profileImageUrl, Map<String, Object> changes) {
        UserProfileUpdatedEventDTO event = UserProfileUpdatedEventDTO.createDefault(
            userId, authUserId, email, name, phone, profileImageUrl, changes
        );
        publishUserProfileUpdatedEvent(event);
    }
    
    public void publishProfileImageChanged(Long userId, String authUserId, String email, String oldImageUrl, String newImageUrl, String changeReason) {
        UserProfileImageChangedEventDTO event = UserProfileImageChangedEventDTO.createDefault(
            userId, authUserId, email, oldImageUrl, newImageUrl, changeReason
        );
        publishUserProfileImageChangedEvent(event);
    }
    
    public void publishSettingsChanged(Long userId, String authUserId, String email, String settingCategory, Map<String, Object> oldSettings, Map<String, Object> newSettings) {
        UserSettingsChangedEventDTO event = UserSettingsChangedEventDTO.createDefault(
            userId, authUserId, email, settingCategory, oldSettings, newSettings
        );
        publishUserSettingsChangedEvent(event);
    }
}