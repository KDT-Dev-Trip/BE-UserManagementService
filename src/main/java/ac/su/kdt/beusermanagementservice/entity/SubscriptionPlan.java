package ac.su.kdt.beusermanagementservice.entity;

import lombok.Getter;

@Getter
public enum SubscriptionPlan {
    // 등급명(초기 지급 티켓 수, 최대 보유 가능 티켓 수, 충전 주기(시간))
    FREE(3, 5, 24),       // 이코노미 클래스 (무료)
    BASIC(5, 10, 6),      // 비즈니스 클래스 (22달러)
    PRO(10, 20, 1),       // 퍼스트 클래스 (110달러)
    ENTERPRISE(5, 10, 6); // 팀 플랜 (22달러) - BASIC과 동일

    private final int initialTickets;       // 초기 지급 티켓 수
    private final int maxTickets;           // 최대 보유 가능 티켓 수
    private final int refillIntervalHours;  // 충전 주기 (시간 단위)

    SubscriptionPlan(int initialTickets, int maxTickets, int refillIntervalHours) {
        this.initialTickets = initialTickets;             // 초기 지급 티켓 수 초기화
        this.maxTickets = maxTickets;                 // 최대 보유 티켓 수 초기화
        this.refillIntervalHours = refillIntervalHours;   // 충전 주기 초기화
    }
}