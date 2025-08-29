package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final UserService userService;
    private final NotificationService notificationService;
    private final UserEventPublisher userEventPublisher;
    
    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "user-service-payment-group")
    public void handlePaymentEvent(@Payload Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("event_type");
            
            switch (eventType) {
                case "payment.subscription-renewal-failed" -> handleSubscriptionRenewalFailed(eventData);
                case "payment.subscription-changed" -> handleSubscriptionChanged(eventData);
                case "payment.ticket-balance-low" -> handleTicketBalanceLow(eventData);
                default -> log.debug("Unknown payment event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", eventData, e);
        }
    }
    
    private void handleSubscriptionRenewalFailed(Map<String, Object> eventData) {
        Long userId = getUserId(eventData);
        String email = (String) eventData.get("email");
        String failureReason = (String) eventData.get("failureReason");
        Integer retryCount = (Integer) eventData.get("retryAttemptCount");
        Double failedAmount = ((Number) eventData.get("failedAmount")).doubleValue();
        
        log.warn("Handling subscription renewal failed for user: {}, reason: {}, retry: {}", 
                userId, failureReason, retryCount);
        
        try {
            // 1. 사용자 정보 업데이트 - 결제 실패 상태 기록
            User user = userService.findById(userId);
            if (user != null) {
                // 결제 실패 횟수 증가 또는 상태 업데이트 로직
                log.info("Updated payment failure status for user: {}", userId);
            }
            
            // 2. 사용자에게 알림 전송
            String message = String.format(
                "구독 결제가 실패했습니다. 사유: %s (재시도: %d회). 결제 방법을 확인해주세요.",
                getFailureReasonKorean(failureReason), retryCount
            );
            notificationService.sendPaymentFailureNotification(userId, email, message, failedAmount);
            
            // 3. 최대 재시도 횟수 초과 시 서비스 제한 알림
            if (retryCount >= 3) {
                notificationService.sendServiceSuspensionNotification(userId, email);
                
                // User settings updated 이벤트 발행
                userEventPublisher.publishSettingsChanged(
                    userId, 
                    user != null ? user.getAuthUserId() : userId.toString(),
                    email,
                    "subscription_status",
                    Map.of("old", "active"),
                    Map.of("new", "suspended")
                );
            }
        } catch (Exception e) {
            log.error("Error processing subscription renewal failed event for user {}", userId, e);
        }
    }
    
    private void handleSubscriptionChanged(Map<String, Object> eventData) {
        Long userId = getUserId(eventData);
        String email = (String) eventData.get("email");
        String oldPlan = (String) eventData.get("oldPlan");
        String newPlan = (String) eventData.get("newPlan");
        String changeReason = (String) eventData.get("changeReason");
        Boolean isProratedRefund = (Boolean) eventData.get("isProratedRefund");
        Double proratedAmount = eventData.get("proratedAmount") != null ? 
            ((Number) eventData.get("proratedAmount")).doubleValue() : null;
        
        log.info("Handling subscription changed for user: {}, {} -> {}, reason: {}", 
                userId, oldPlan, newPlan, changeReason);
        
        try {
            // 1. 사용자 프로필에 새로운 구독 플랜 정보 업데이트
            User user = userService.findById(userId);
            if (user != null) {
                // 구독 플랜 정보를 사용자 프로필에 캐시
                userService.updateSubscriptionPlan(userId, newPlan);
                log.info("Updated subscription plan cache for user: {} to {}", userId, newPlan);
            }
            
            // 2. 플랜 변경 축하/안내 알림 전송
            boolean isUpgrade = isUpgrade(oldPlan, newPlan);
            String message = isUpgrade ? 
                String.format("축하합니다! %s 플랜으로 업그레이드되었습니다. 더 많은 기능을 이용해보세요!", newPlan) :
                String.format("플랜이 %s로 변경되었습니다.", newPlan);
            
            if (isProratedRefund != null && isProratedRefund && proratedAmount != null) {
                message += String.format(" 중도 변경으로 인해 %.0f원이 환불 처리됩니다.", proratedAmount);
            }
            
            notificationService.sendPlanChangeNotification(userId, email, message, isUpgrade);
            
            // 3. 업그레이드인 경우 환영 이메일 및 기능 가이드 전송
            if (isUpgrade) {
                notificationService.sendUpgradeWelcomeGuide(userId, email, newPlan);
            }
            
            // 4. User profile updated 이벤트 발행 (다른 서비스에서 권한 업데이트용)
            userEventPublisher.publishProfileUpdated(
                userId,
                user != null ? user.getAuthUserId() : userId.toString(),
                email,
                user != null ? user.getName() : "Unknown",
                user != null ? user.getPhone() : null,
                user != null ? user.getProfileImageUrl() : null,
                Map.of("subscription_plan", Map.of("old", oldPlan, "new", newPlan))
            );
            
        } catch (Exception e) {
            log.error("Error processing subscription changed event for user {}", userId, e);
        }
    }
    
    private void handleTicketBalanceLow(Map<String, Object> eventData) {
        Long userId = getUserId(eventData);
        String email = (String) eventData.get("email");
        Integer currentBalance = (Integer) eventData.get("currentBalance");
        Integer thresholdLimit = (Integer) eventData.get("thresholdLimit");
        String subscriptionPlan = (String) eventData.get("subscriptionPlan");
        Integer suggestedRechargeAmount = (Integer) eventData.get("suggestedRechargeAmount");
        
        log.warn("Handling ticket balance low for user: {}, balance: {}/{}, plan: {}", 
                userId, currentBalance, thresholdLimit, subscriptionPlan);
        
        try {
            // 1. 사용자에게 잔액 부족 알림 전송
            String message = String.format(
                "티켓 잔액이 부족합니다. 현재 잔액: %d개 (임계값: %d개). " +
                "미션을 계속 진행하려면 티켓을 충전하거나 플랜 업그레이드를 고려해보세요.",
                currentBalance, thresholdLimit
            );
            
            notificationService.sendLowBalanceNotification(userId, email, message, currentBalance);
            
            // 2. 현재 플랜이 기본 플랜인 경우 업그레이드 권유
            if (isBasicPlan(subscriptionPlan)) {
                String upgradeMessage = String.format(
                    "상위 플랜으로 업그레이드하면 더 많은 티켓과 빠른 충전이 가능합니다. " +
                    "권장 충전량: %d개", suggestedRechargeAmount
                );
                notificationService.sendUpgradeRecommendation(userId, email, upgradeMessage, subscriptionPlan);
            }
            
            // 3. 티켓 자동 충전 설정 안내 (아직 미구현 기능)
            notificationService.sendAutoRechargeInfo(userId, email);
            
            // 4. 잔액 부족 상태를 사용자 설정에 기록
            User user = userService.findById(userId);
            if (user != null) {
                // User settings updated 이벤트 발행
                userEventPublisher.publishSettingsChanged(
                    userId,
                    user.getAuthUserId(),
                    email,
                    "balance_alerts",
                    Map.of("low_balance_alert", Map.of("balance", currentBalance, "threshold", thresholdLimit)),
                    Map.of("last_low_balance_alert", Map.of("timestamp", System.currentTimeMillis()))
                );
            }
            
        } catch (Exception e) {
            log.error("Error processing ticket balance low event for user {}", userId, e);
        }
    }
    
    private Long getUserId(Map<String, Object> eventData) {
        Object userIdObj = eventData.get("user_id");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        } else {
            throw new IllegalArgumentException("Invalid user_id format: " + userIdObj);
        }
    }
    
    private String getFailureReasonKorean(String failureReason) {
        return switch (failureReason) {
            case "PAYMENT_FAILED" -> "결제 실패";
            case "CARD_EXPIRED" -> "카드 만료";
            case "INSUFFICIENT_FUNDS" -> "잔액 부족";
            case "CARD_DECLINED" -> "카드 거부";
            default -> failureReason;
        };
    }
    
    private boolean isUpgrade(String oldPlan, String newPlan) {
        // 플랜 등급: ECONOMY_CLASS < BUSINESS_CLASS < FIRST_CLASS
        int oldRank = getPlanRank(oldPlan);
        int newRank = getPlanRank(newPlan);
        return newRank > oldRank;
    }
    
    private int getPlanRank(String planName) {
        if (planName == null) return 0;
        return switch (planName.toUpperCase()) {
            case "ECONOMY_CLASS", "ECONOMY" -> 1;
            case "BUSINESS_CLASS", "BUSINESS" -> 2;
            case "FIRST_CLASS", "FIRST" -> 3;
            default -> 0;
        };
    }
    
    private boolean isBasicPlan(String planName) {
        return "ECONOMY_CLASS".equalsIgnoreCase(planName) || "ECONOMY".equalsIgnoreCase(planName);
    }
}