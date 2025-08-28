package ac.su.kdt.beusermanagementservice.dto;

import java.time.LocalDateTime;

public record UserSignedUpEventDTO(
        String authUserId,           // Auth 서비스의 UUID
        String email,
        String name,
        String planType,            // 구독 플랜 타입
        LocalDateTime signupTimestamp,
        String source,              // "EMAIL", "GOOGLE", "GITHUB" 등
        String socialProvider,      // 소셜 로그인 제공자
        String ipAddress,           // IP 주소
        long timestamp              // 타임스탬프
) {}