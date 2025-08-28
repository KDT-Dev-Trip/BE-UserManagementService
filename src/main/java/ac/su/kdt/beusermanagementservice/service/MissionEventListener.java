package ac.su.kdt.beusermanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionEventListener {
    
    private final NotificationService notificationService;
    private final UserEventPublisher userEventPublisher;
    
    /**
     * 미션 일시정지 이벤트 처리 - 사용자 서비스 관점
     */
    @KafkaListener(topics = "mission-events", groupId = "user-service-mission-group")
    public void handleMissionEvent(Map<String, Object> eventData) {
        String eventType = (String) eventData.get("eventType");
        
        try {
            switch (eventType) {
                case "mission.paused":
                    handleMissionPaused(eventData);
                    break;
                case "mission.resumed":
                    handleMissionResumed(eventData);
                    break;
                case "mission.resource-provisioning-failed":
                    handleResourceProvisioningFailed(eventData);
                    break;
                case "mission.resource-cleanup-completed":
                    handleResourceCleanupCompleted(eventData);
                    break;
                default:
                    log.debug("Unknown mission event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error handling mission event: {}", eventType, e);
        }
    }
    
    /**
     * 미션 일시정지 이벤트 처리
     */
    private void handleMissionPaused(Map<String, Object> eventData) {
        try {
            Long userId = Long.valueOf(eventData.get("userId").toString());
            String email = (String) eventData.get("email");
            String missionTitle = (String) eventData.get("missionTitle");
            String attemptId = (String) eventData.get("attemptId");
            String pauseReason = (String) eventData.get("pauseReason");
            Integer progressPercent = (Integer) eventData.get("progressPercent");
            
            log.info("🔄 [USER_SERVICE] Mission paused event received: userId={}, mission={}, reason={}", 
                    userId, missionTitle, pauseReason);
            
            // 1. 사용자에게 미션 일시정지 알림 발송
            String message = String.format("미션 '%s'이 일시정지되었습니다. 사유: %s (진행률: %d%%)", 
                    missionTitle, getReasonKorean(pauseReason), progressPercent != null ? progressPercent : 0);
            notificationService.sendMissionPauseNotification(userId, email, missionTitle, message, attemptId);
            
            // 2. 미션 저장 가이드 알림 (장시간 일시정지시)
            if ("장시간_비활성".equals(pauseReason) || "TIMEOUT".equals(pauseReason)) {
                notificationService.sendMissionSaveGuideNotification(userId, email, missionTitle, attemptId);
            }
            
            // 3. 미션 재개 안내 (24시간 내 재개 가능)
            notificationService.sendMissionResumeGuideNotification(userId, email, missionTitle, attemptId);
            
            // 4. User activity 이벤트 발행 (분석 목적)
            userEventPublisher.publishUserActivityEvent(userId, "mission_paused", Map.of(
                "missionTitle", missionTitle,
                "attemptId", attemptId,
                "pauseReason", pauseReason,
                "progressPercent", progressPercent != null ? progressPercent : 0
            ));
            
        } catch (Exception e) {
            log.error("Failed to handle mission paused event", e);
        }
    }
    
    /**
     * 미션 재개 이벤트 처리
     */
    private void handleMissionResumed(Map<String, Object> eventData) {
        try {
            Long userId = Long.valueOf(eventData.get("userId").toString());
            String email = (String) eventData.get("email");
            String missionTitle = (String) eventData.get("missionTitle");
            String attemptId = (String) eventData.get("attemptId");
            Long pauseDurationMinutes = Long.valueOf(eventData.get("pauseDurationMinutes").toString());
            String websocketUrl = (String) eventData.get("websocketUrl");
            
            log.info("▶️ [USER_SERVICE] Mission resumed event received: userId={}, mission={}, pauseTime={}min", 
                    userId, missionTitle, pauseDurationMinutes);
            
            // 1. 사용자에게 미션 재개 알림 발송
            String message = String.format("미션 '%s'이 재개되었습니다. 일시정지 시간: %d분", 
                    missionTitle, pauseDurationMinutes);
            notificationService.sendMissionResumeNotification(userId, email, missionTitle, message, websocketUrl);
            
            // 2. 진행 상황 복원 안내
            if (pauseDurationMinutes > 60) { // 1시간 이상 일시정지된 경우
                notificationService.sendProgressRestoreNotification(userId, email, missionTitle, attemptId);
            }
            
            // 3. 남은 시간 안내 (실제로는 계산 필요)
            Integer remainingMinutes = (Integer) eventData.get("remainingTimeMinutes");
            if (remainingMinutes != null && remainingMinutes < 30) {
                notificationService.sendTimeWarningNotification(userId, email, missionTitle, remainingMinutes);
            }
            
            // 4. User activity 이벤트 발행
            userEventPublisher.publishUserActivityEvent(userId, "mission_resumed", Map.of(
                "missionTitle", missionTitle,
                "attemptId", attemptId,
                "pauseDurationMinutes", pauseDurationMinutes,
                "websocketUrl", websocketUrl
            ));
            
        } catch (Exception e) {
            log.error("Failed to handle mission resumed event", e);
        }
    }
    
    /**
     * 리소스 프로비저닝 실패 이벤트 처리
     */
    private void handleResourceProvisioningFailed(Map<String, Object> eventData) {
        try {
            Long userId = Long.valueOf(eventData.get("userId").toString());
            String email = (String) eventData.get("email");
            String missionTitle = (String) eventData.get("missionTitle");
            String attemptId = (String) eventData.get("attemptId");
            String resourceType = (String) eventData.get("resourceType");
            String failureReason = (String) eventData.get("failureReason");
            Integer retryAttempt = (Integer) eventData.get("retryAttempt");
            Boolean canRetry = (Boolean) eventData.get("canRetry");
            
            log.warn("🚨 [USER_SERVICE] Resource provisioning failed: userId={}, mission={}, resource={}, reason={}", 
                    userId, missionTitle, resourceType, failureReason);
            
            // 1. 사용자에게 기술적 문제 알림 발송
            String message = String.format("미션 '%s' 환경 설정 중 문제가 발생했습니다. (%d번째 시도)", 
                    missionTitle, retryAttempt);
            notificationService.sendTechnicalIssueNotification(userId, email, missionTitle, message, canRetry);
            
            // 2. 재시도 안내 (가능한 경우)
            if (Boolean.TRUE.equals(canRetry)) {
                notificationService.sendRetryGuideNotification(userId, email, missionTitle, attemptId);
            } else {
                // 3. 대안 제시 (재시도 불가능한 경우)
                notificationService.sendAlternativeSolutionNotification(userId, email, missionTitle);
            }
            
            // 4. 기술 지원팀 알림 (심각한 문제의 경우)
            if (retryAttempt != null && retryAttempt >= 3) {
                notificationService.sendTechnicalSupportAlert(userId, email, missionTitle, failureReason, attemptId);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle resource provisioning failed event", e);
        }
    }
    
    /**
     * 리소스 정리 완료 이벤트 처리
     */
    private void handleResourceCleanupCompleted(Map<String, Object> eventData) {
        try {
            Long userId = Long.valueOf(eventData.get("userId").toString());
            String email = (String) eventData.get("email");
            String missionTitle = (String) eventData.get("missionTitle");
            String attemptId = (String) eventData.get("attemptId");
            String cleanupTrigger = (String) eventData.get("cleanupTrigger");
            Integer totalResourcesCleaned = (Integer) eventData.get("totalResourcesCleaned");
            Boolean dataBackupCreated = (Boolean) eventData.get("dataBackupCreated");
            String backupLocation = (String) eventData.get("backupLocation");
            
            log.info("🧹 [USER_SERVICE] Resource cleanup completed: userId={}, mission={}, trigger={}, resources={}", 
                    userId, missionTitle, cleanupTrigger, totalResourcesCleaned);
            
            // 1. 정리 완료 알림 발송
            String triggerKorean = getCleanupTriggerKorean(cleanupTrigger);
            String message = String.format("미션 '%s' 환경 정리가 완료되었습니다. (사유: %s)", 
                    missionTitle, triggerKorean);
            notificationService.sendCleanupCompletedNotification(userId, email, missionTitle, message);
            
            // 2. 데이터 백업 정보 안내 (백업이 생성된 경우)
            if (Boolean.TRUE.equals(dataBackupCreated) && backupLocation != null) {
                notificationService.sendDataBackupNotification(userId, email, missionTitle, backupLocation, attemptId);
            }
            
            // 3. 미션 완료 축하 (정상 완료된 경우)
            if ("MISSION_COMPLETED".equals(cleanupTrigger)) {
                notificationService.sendMissionCompletionCongratulation(userId, email, missionTitle);
                
                // 다음 미션 추천
                notificationService.sendNextMissionRecommendation(userId, email);
            }
            
            // 4. 사용자 통계 업데이트 이벤트 발행
            userEventPublisher.publishUserStatsUpdateEvent(userId, "mission_cleanup", Map.of(
                "missionTitle", missionTitle,
                "attemptId", attemptId,
                "cleanupTrigger", cleanupTrigger,
                "resourcesCleaned", totalResourcesCleaned
            ));
            
        } catch (Exception e) {
            log.error("Failed to handle resource cleanup completed event", e);
        }
    }
    
    /**
     * 일시정지 사유 한국어 변환
     */
    private String getReasonKorean(String reason) {
        if (reason == null) return "알 수 없음";
        
        switch (reason) {
            case "사용자 요청": return "사용자 요청";
            case "USER_REQUESTED": return "사용자 요청";
            case "TIMEOUT": return "시간 초과";
            case "SYSTEM_MAINTENANCE": return "시스템 점검";
            case "장시간_비활성": return "장시간 비활성";
            default: return reason;
        }
    }
    
    /**
     * 정리 트리거 한국어 변환
     */
    private String getCleanupTriggerKorean(String trigger) {
        if (trigger == null) return "알 수 없음";
        
        switch (trigger) {
            case "MISSION_COMPLETED": return "미션 완료";
            case "MISSION_TIMEOUT": return "제한 시간 초과";
            case "USER_REQUESTED": return "사용자 요청";
            case "ADMIN_CLEANUP": return "관리자 정리";
            default: return trigger;
        }
    }
}