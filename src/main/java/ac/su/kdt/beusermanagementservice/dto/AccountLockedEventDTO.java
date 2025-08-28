package ac.su.kdt.beusermanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AccountLockedEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    @JsonProperty("auth_user_id")
    String authUserId,
    
    String email,
    String lockReason,
    Integer lockDurationMinutes,
    LocalDateTime lockedAt,
    LocalDateTime unlocksAt,
    String ipAddress,
    
    @JsonProperty("timestamp")
    long timestamp
) {}