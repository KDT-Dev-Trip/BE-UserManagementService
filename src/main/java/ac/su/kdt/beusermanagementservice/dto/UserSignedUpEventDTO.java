package ac.su.kdt.beusermanagementservice.dto;

import ac.su.kdt.beusermanagementservice.entity.SubscriptionPlan;

public record UserSignedUpEventDTO(
        String auth0Id,
        String email,
        String name,
        SubscriptionPlan subscriptionPlan // ## 사용자가 가입 시 선택한 구독 플랜 정보
) {}