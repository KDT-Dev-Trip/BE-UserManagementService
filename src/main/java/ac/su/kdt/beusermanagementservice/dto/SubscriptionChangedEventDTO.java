package ac.su.kdt.beusermanagementservice.dto;

import ac.su.kdt.beusermanagementservice.entity.SubscriptionPlan;

public record SubscriptionChangedEventDTO(
    Long userId, // 플랜을 변경한 사용자의 ID
    SubscriptionPlan newPlan // 새로 변경된 플랜 정보
) {}