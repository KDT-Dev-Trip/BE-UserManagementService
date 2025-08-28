package ac.su.kdt.beusermanagementservice.dto;

import java.time.LocalDateTime;

public record UserSyncDataDTO(
        String authUserId,
        String email,
        String name,
        String planType,
        LocalDateTime signupTimestamp,
        String source,
        String socialProvider,
        String status // "ACTIVE", "INACTIVE", "DELETED"
) {}