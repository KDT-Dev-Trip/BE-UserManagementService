package ac.su.kdt.beusermanagementservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
    
    /**
     * 결제 실패 알림 전송
     */
    public void sendPaymentFailureNotification(Long userId, String email, String message, Double failedAmount) {
        log.info("Sending payment failure notification to user: {}, email: {}, amount: {}", userId, email, failedAmount);
        log.info("Message: {}", message);
        
        // 실제 구현에서는 다음과 같은 방식으로 알림을 보낼 수 있습니다:
        // 1. 이메일 발송
        // 2. SMS 발송  
        // 3. 푸시 알림
        // 4. 인앱 알림
        
        // 현재는 로그만 출력
        simulateNotificationSend("PAYMENT_FAILURE", userId, email, message);
    }
    
    /**
     * 서비스 정지 알림 전송
     */
    public void sendServiceSuspensionNotification(Long userId, String email) {
        String message = "결제 실패로 인해 서비스 이용이 일시 중단되었습니다. 결제 방법을 확인하고 다시 시도해주세요.";
        log.warn("Sending service suspension notification to user: {}, email: {}", userId, email);
        
        simulateNotificationSend("SERVICE_SUSPENSION", userId, email, message);
    }
    
    /**
     * 플랜 변경 알림 전송
     */
    public void sendPlanChangeNotification(Long userId, String email, String message, boolean isUpgrade) {
        String notificationType = isUpgrade ? "PLAN_UPGRADE" : "PLAN_CHANGE";
        log.info("Sending plan change notification to user: {}, email: {}, isUpgrade: {}", userId, email, isUpgrade);
        
        simulateNotificationSend(notificationType, userId, email, message);
    }
    
    /**
     * 업그레이드 환영 가이드 전송
     */
    public void sendUpgradeWelcomeGuide(Long userId, String email, String newPlan) {
        String message = String.format("환영합니다! %s 플랜의 새로운 기능들을 확인해보세요:", newPlan);
        log.info("Sending upgrade welcome guide to user: {}, email: {}, plan: {}", userId, email, newPlan);
        
        simulateNotificationSend("UPGRADE_WELCOME", userId, email, message);
    }
    
    /**
     * 잔액 부족 알림 전송
     */
    public void sendLowBalanceNotification(Long userId, String email, String message, Integer currentBalance) {
        log.warn("Sending low balance notification to user: {}, email: {}, balance: {}", userId, email, currentBalance);
        
        simulateNotificationSend("LOW_BALANCE", userId, email, message);
    }
    
    /**
     * 업그레이드 추천 알림 전송
     */
    public void sendUpgradeRecommendation(Long userId, String email, String message, String currentPlan) {
        log.info("Sending upgrade recommendation to user: {}, email: {}, currentPlan: {}", userId, email, currentPlan);
        
        simulateNotificationSend("UPGRADE_RECOMMENDATION", userId, email, message);
    }
    
    /**
     * 자동 충전 설정 안내 전송
     */
    public void sendAutoRechargeInfo(Long userId, String email) {
        String message = "티켓 자동 충전 기능을 활성화하면 잔액이 부족할 때 자동으로 충전됩니다. 설정에서 확인해보세요.";
        log.info("Sending auto recharge info to user: {}, email: {}", userId, email);
        
        simulateNotificationSend("AUTO_RECHARGE_INFO", userId, email, message);
    }

    // === Mission 관련 알림 메서드들 ===
    
    /**
     * 미션 일시정지 알림 전송
     */
    public void sendMissionPauseNotification(Long userId, String email, String missionTitle, String message, String attemptId) {
        log.info("Sending mission pause notification to user: {}, email: {}, mission: {}", userId, email, missionTitle);
        simulateNotificationSend("MISSION_PAUSE", userId, email, message);
    }
    
    /**
     * 미션 저장 가이드 알림 전송
     */
    public void sendMissionSaveGuideNotification(Long userId, String email, String missionTitle, String attemptId) {
        String message = String.format("미션 '%s'이 장시간 일시정지되었습니다. 작업 내용을 저장하는 것을 권장합니다.", missionTitle);
        log.info("Sending mission save guide to user: {}, email: {}, mission: {}", userId, email, missionTitle);
        simulateNotificationSend("MISSION_SAVE_GUIDE", userId, email, message);
    }
    
    /**
     * 미션 재개 안내 알림 전송
     */
    public void sendMissionResumeGuideNotification(Long userId, String email, String missionTitle, String attemptId) {
        String message = String.format("미션 '%s'을 24시간 내에 재개할 수 있습니다. 이후에는 새로 시작해야 합니다.", missionTitle);
        log.info("Sending mission resume guide to user: {}, email: {}, mission: {}", userId, email, missionTitle);
        simulateNotificationSend("MISSION_RESUME_GUIDE", userId, email, message);
    }
    
    /**
     * 미션 재개 알림 전송
     */
    public void sendMissionResumeNotification(Long userId, String email, String missionTitle, String message, String websocketUrl) {
        log.info("Sending mission resume notification to user: {}, email: {}, mission: {}, websocket: {}", 
                userId, email, missionTitle, websocketUrl);
        simulateNotificationSend("MISSION_RESUME", userId, email, message);
    }
    
    /**
     * 진행 상황 복원 안내 알림 전송
     */
    public void sendProgressRestoreNotification(Long userId, String email, String missionTitle, String attemptId) {
        String message = String.format("미션 '%s'의 이전 진행 상황을 확인하고 복원할 수 있습니다.", missionTitle);
        log.info("Sending progress restore notification to user: {}, email: {}, mission: {}", userId, email, missionTitle);
        simulateNotificationSend("PROGRESS_RESTORE", userId, email, message);
    }
    
    /**
     * 시간 경고 알림 전송
     */
    public void sendTimeWarningNotification(Long userId, String email, String missionTitle, Integer remainingMinutes) {
        String message = String.format("미션 '%s' 남은 시간: %d분", missionTitle, remainingMinutes);
        log.warn("Sending time warning notification to user: {}, email: {}, mission: {}, remaining: {}min", 
                userId, email, missionTitle, remainingMinutes);
        simulateNotificationSend("TIME_WARNING", userId, email, message);
    }
    
    /**
     * 기술적 문제 알림 전송
     */
    public void sendTechnicalIssueNotification(Long userId, String email, String missionTitle, String message, Boolean canRetry) {
        log.warn("Sending technical issue notification to user: {}, email: {}, mission: {}, canRetry: {}", 
                userId, email, missionTitle, canRetry);
        simulateNotificationSend("TECHNICAL_ISSUE", userId, email, message);
    }
    
    /**
     * 재시도 안내 알림 전송
     */
    public void sendRetryGuideNotification(Long userId, String email, String missionTitle, String attemptId) {
        String message = String.format("미션 '%s' 환경 설정을 다시 시도할 수 있습니다.", missionTitle);
        log.info("Sending retry guide notification to user: {}, email: {}, mission: {}", userId, email, missionTitle);
        simulateNotificationSend("RETRY_GUIDE", userId, email, message);
    }
    
    /**
     * 대안 솔루션 알림 전송
     */
    public void sendAlternativeSolutionNotification(Long userId, String email, String missionTitle) {
        String message = String.format("미션 '%s'에 문제가 발생했습니다. 다른 미션을 시도하거나 지원팀에 문의해주세요.", missionTitle);
        log.info("Sending alternative solution notification to user: {}, email: {}, mission: {}", userId, email, missionTitle);
        simulateNotificationSend("ALTERNATIVE_SOLUTION", userId, email, message);
    }
    
    /**
     * 기술 지원팀 알림 전송
     */
    public void sendTechnicalSupportAlert(Long userId, String email, String missionTitle, String failureReason, String attemptId) {
        String message = String.format("미션 '%s'에서 반복적인 문제가 발생했습니다. 기술 지원팀이 검토 중입니다.", missionTitle);
        log.warn("Sending technical support alert for user: {}, email: {}, mission: {}, reason: {}, attempt: {}", 
                userId, email, missionTitle, failureReason, attemptId);
        simulateNotificationSend("TECHNICAL_SUPPORT_ALERT", userId, email, message);
    }
    
    /**
     * 정리 완료 알림 전송
     */
    public void sendCleanupCompletedNotification(Long userId, String email, String missionTitle, String message) {
        log.info("Sending cleanup completed notification to user: {}, email: {}, mission: {}", userId, email, missionTitle);
        simulateNotificationSend("CLEANUP_COMPLETED", userId, email, message);
    }
    
    /**
     * 데이터 백업 알림 전송
     */
    public void sendDataBackupNotification(Long userId, String email, String missionTitle, String backupLocation, String attemptId) {
        String message = String.format("미션 '%s'의 데이터가 백업되었습니다. 위치: %s", missionTitle, backupLocation);
        log.info("Sending data backup notification to user: {}, email: {}, mission: {}, backup: {}", 
                userId, email, missionTitle, backupLocation);
        simulateNotificationSend("DATA_BACKUP", userId, email, message);
    }
    
    /**
     * 미션 완료 축하 알림 전송
     */
    public void sendMissionCompletionCongratulation(Long userId, String email, String missionTitle) {
        String message = String.format("🎉 축하합니다! 미션 '%s'를 성공적으로 완료했습니다!", missionTitle);
        log.info("Sending mission completion congratulation to user: {}, email: {}, mission: {}", userId, email, missionTitle);
        simulateNotificationSend("MISSION_COMPLETION", userId, email, message);
    }
    
    /**
     * 다음 미션 추천 알림 전송
     */
    public void sendNextMissionRecommendation(Long userId, String email) {
        String message = "새로운 미션에 도전해보세요! 당신의 실력에 맞는 추천 미션을 확인할 수 있습니다.";
        log.info("Sending next mission recommendation to user: {}, email: {}", userId, email);
        simulateNotificationSend("NEXT_MISSION_RECOMMENDATION", userId, email, message);
    }
    
    /**
     * 알림 발송 시뮬레이션
     */
    private void simulateNotificationSend(String type, Long userId, String email, String message) {
        // 실제 환경에서는 여기서 다음과 같은 작업을 수행합니다:
        // 1. 외부 알림 서비스 API 호출 (FCM, APNs, 이메일 서비스 등)
        // 2. 알림 발송 결과 처리
        // 3. 알림 발송 이력 저장
        // 4. 실패 시 재시도 로직
        
        log.info("📧 [NOTIFICATION_SENT] Type: {}, UserId: {}, Email: {}, Message: {}", 
                type, userId, maskEmail(email), message);
        
        // 실제 발송 지연 시뮬레이션
        try {
            Thread.sleep(100); // 100ms 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 사용자에게 알림 전송 (일반적인 알림)
     */
    public void sendNotificationToUser(Long userId, String title, String message) {
        log.info("Sending notification to user: {}, title: {}, message: {}", userId, title, message);
        simulateNotificationSend("USER_NOTIFICATION", userId, null, title + ": " + message);
    }
    
    /**
     * 관리자 경고 알림 전송
     */
    public void sendAdminAlert(String title, String message) {
        log.warn("Sending admin alert - title: {}, message: {}", title, message);
        simulateNotificationSend("ADMIN_ALERT", null, "admin@system.com", title + ": " + message);
    }

    /**
     * 이메일 마스킹 (개인정보 보호)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];
        
        if (username.length() <= 2) {
            return username + "@" + domain;
        }
        
        String maskedUsername = username.substring(0, 2) + "*".repeat(username.length() - 2);
        return maskedUsername + "@" + domain;
    }
}