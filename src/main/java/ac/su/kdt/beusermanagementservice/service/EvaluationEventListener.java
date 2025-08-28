package ac.su.kdt.beusermanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationEventListener {
    
    private final NotificationService notificationService;
    private final UserEventPublisher userEventPublisher;
    
    /**
     * AI 평가 이벤트 처리 - 사용자 서비스 관점
     */
    @KafkaListener(topics = "evaluation-events", groupId = "user-service-evaluation-group")
    public void handleEvaluationEvent(Map<String, Object> eventData) {
        String eventType = (String) eventData.get("eventType");
        
        try {
            switch (eventType) {
                case "evaluation.started":
                    handleEvaluationStarted(eventData);
                    break;
                case "evaluation.failed":
                    handleEvaluationFailed(eventData);
                    break;
                case "evaluation.retry-requested":
                    handleEvaluationRetryRequested(eventData);
                    break;
                case "evaluation.retry-completed":
                    handleEvaluationRetryCompleted(eventData);
                    break;
                default:
                    log.debug("Unknown evaluation event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error handling evaluation event: {}", eventType, e);
        }
    }
    
    /**
     * 평가 시작 이벤트 처리
     * - 사용자에게 평가 시작 알림 발송
     */
    private void handleEvaluationStarted(Map<String, Object> eventData) {
        String evaluationId = (String) eventData.get("evaluationId");
        String missionId = (String) eventData.get("missionId");
        String missionTitle = (String) eventData.get("missionTitle");
        Long userId = Long.valueOf(eventData.get("userId").toString());
        String userEmail = (String) eventData.get("userEmail");
        String userName = (String) eventData.get("userName");
        
        log.info("🚀 [USER_SERVICE] Processing evaluation started event: evaluationId={}, userId={}, missionTitle={}", 
            evaluationId, userId, missionTitle);
        
        try {
            // 사용자에게 평가 시작 알림 발송
            String notificationMessage = String.format("미션 '%s'에 대한 AI 평가가 시작되었습니다.", missionTitle);
            notificationService.sendNotificationToUser(userId, "평가 시작", notificationMessage);
            
            log.info("✅ [USER_SERVICE] Evaluation started notification sent to user: userId={}, evaluationId={}", 
                userId, evaluationId);
            
        } catch (Exception e) {
            log.error("❌ [USER_SERVICE] Failed to process evaluation started event: evaluationId={}, userId={}", 
                evaluationId, userId, e);
        }
    }
    
    /**
     * 평가 실패 이벤트 처리  
     * - 사용자에게 평가 실패 알림 발송
     * - 재시도 가능 여부에 따른 안내
     */
    private void handleEvaluationFailed(Map<String, Object> eventData) {
        String evaluationId = (String) eventData.get("evaluationId");
        String missionId = (String) eventData.get("missionId");
        String missionTitle = (String) eventData.get("missionTitle");
        Long userId = Long.valueOf(eventData.get("userId").toString());
        String failureReason = (String) eventData.get("failureReason");
        Integer retryAttempt = (Integer) eventData.get("retryAttempt");
        Boolean isRetryable = (Boolean) eventData.get("isRetryable");
        
        log.warn("🚨 [USER_SERVICE] Processing evaluation failed event: evaluationId={}, userId={}, reason={}, retryAttempt={}", 
            evaluationId, userId, failureReason, retryAttempt);
        
        try {
            // 사용자에게 평가 실패 알림 발송
            String notificationMessage;
            if (Boolean.TRUE.equals(isRetryable)) {
                notificationMessage = String.format("미션 '%s'의 AI 평가가 실패했지만 자동으로 재시도됩니다. (시도 횟수: %d회)", 
                    missionTitle, retryAttempt != null ? retryAttempt : 1);
            } else {
                notificationMessage = String.format("미션 '%s'의 AI 평가가 실패했습니다. 수동 검토가 필요합니다.", missionTitle);
            }
            
            notificationService.sendNotificationToUser(userId, "평가 실패 알림", notificationMessage);
            
            // 관리자에게 알림 (재시도 불가능하거나 여러 번 실패한 경우)
            if (!Boolean.TRUE.equals(isRetryable) || (retryAttempt != null && retryAttempt >= 2)) {
                String adminMessage = String.format("사용자 %d의 미션 '%s' 평가가 반복 실패했습니다. 수동 검토 필요 (evaluationId: %s)", 
                    userId, missionTitle, evaluationId);
                notificationService.sendAdminAlert("평가 반복 실패 알림", adminMessage);
            }
            
            log.info("✅ [USER_SERVICE] Evaluation failed notifications sent: userId={}, evaluationId={}", 
                userId, evaluationId);
            
        } catch (Exception e) {
            log.error("❌ [USER_SERVICE] Failed to process evaluation failed event: evaluationId={}, userId={}", 
                evaluationId, userId, e);
        }
    }
    
    /**
     * 평가 재시도 요청 이벤트 처리
     * - 사용자에게 재시도 안내 발송
     */
    private void handleEvaluationRetryRequested(Map<String, Object> eventData) {
        String evaluationId = (String) eventData.get("evaluationId");
        String missionId = (String) eventData.get("missionId");
        String missionTitle = (String) eventData.get("missionTitle");
        Long userId = Long.valueOf(eventData.get("userId").toString());
        Integer retryAttempt = (Integer) eventData.get("retryAttempt");
        String previousFailureReason = (String) eventData.get("previousFailureReason");
        String retryStrategy = (String) eventData.get("retryStrategy");
        
        log.info("🔄 [USER_SERVICE] Processing evaluation retry requested event: evaluationId={}, userId={}, retryAttempt={}", 
            evaluationId, userId, retryAttempt);
        
        try {
            // 사용자에게 재시도 알림 발송
            String notificationMessage = String.format("미션 '%s'의 AI 평가를 다시 시도합니다. (시도 횟수: %d회)", 
                missionTitle, retryAttempt != null ? retryAttempt : 1);
            notificationService.sendNotificationToUser(userId, "평가 재시도 중", notificationMessage);
            
            log.info("✅ [USER_SERVICE] Evaluation retry notification sent to user: userId={}, evaluationId={}", 
                userId, evaluationId);
            
        } catch (Exception e) {
            log.error("❌ [USER_SERVICE] Failed to process evaluation retry requested event: evaluationId={}, userId={}", 
                evaluationId, userId, e);
        }
    }
    
    /**
     * 평가 재시도 완료 이벤트 처리
     * - 재시도 결과에 따른 사용자 알림
     */
    private void handleEvaluationRetryCompleted(Map<String, Object> eventData) {
        String evaluationId = (String) eventData.get("evaluationId");
        String missionId = (String) eventData.get("missionId");
        String missionTitle = (String) eventData.get("missionTitle");
        Long userId = Long.valueOf(eventData.get("userId").toString());
        Integer retryAttempt = (Integer) eventData.get("retryAttempt");
        String retryStatus = (String) eventData.get("retryStatus");
        Integer finalScore = (Integer) eventData.get("finalScore");
        Boolean needsHumanReview = (Boolean) eventData.get("needsHumanReview");
        
        log.info("✅ [USER_SERVICE] Processing evaluation retry completed event: evaluationId={}, userId={}, status={}, finalScore={}", 
            evaluationId, userId, retryStatus, finalScore);
        
        try {
            String notificationMessage;
            String notificationTitle;
            
            if ("SUCCESS".equals(retryStatus)) {
                notificationTitle = "평가 재시도 성공";
                notificationMessage = String.format("미션 '%s'의 AI 평가가 성공적으로 완료되었습니다! 점수: %d점 (시도 횟수: %d회)", 
                    missionTitle, finalScore != null ? finalScore : 0, retryAttempt != null ? retryAttempt : 1);
                
                // 점수가 낮은 경우 추가 안내
                if (finalScore != null && finalScore < 60) {
                    notificationMessage += " 점수가 낮아 추가 검토가 필요할 수 있습니다.";
                }
            } else {
                notificationTitle = "평가 재시도 실패";
                notificationMessage = String.format("미션 '%s'의 AI 평가 재시도가 최종 실패했습니다. 수동 검토가 진행됩니다.", missionTitle);
            }
            
            notificationService.sendNotificationToUser(userId, notificationTitle, notificationMessage);
            
            // 수동 검토 필요한 경우 관리자에게 알림
            if (Boolean.TRUE.equals(needsHumanReview)) {
                String adminMessage = String.format("사용자 %d의 미션 '%s' 평가가 수동 검토를 필요로 합니다. (evaluationId: %s, finalScore: %d)", 
                    userId, missionTitle, evaluationId, finalScore != null ? finalScore : 0);
                notificationService.sendAdminAlert("수동 검토 필요", adminMessage);
            }
            
            log.info("✅ [USER_SERVICE] Evaluation retry completed notifications sent: userId={}, evaluationId={}", 
                userId, evaluationId);
            
        } catch (Exception e) {
            log.error("❌ [USER_SERVICE] Failed to process evaluation retry completed event: evaluationId={}, userId={}", 
                evaluationId, userId, e);
        }
    }
}