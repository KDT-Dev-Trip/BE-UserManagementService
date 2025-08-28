package ac.su.kdt.beusermanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record LoginFailedEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    String email,
    String failureReason,
    String ipAddress,
    String userAgent,
    Integer attemptCount,
    LocalDateTime failedAt,
    
    @JsonProperty("timestamp")
    long timestamp
) {}