package ac.su.kdt.beusermanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

public record UserProfileUpdatedEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    @JsonProperty("user_id")
    Long userId,
    
    @JsonProperty("auth_user_id")
    String authUserId,
    
    String email,
    String name,
    String phone,
    String profileImageUrl,
    Map<String, Object> changes, // 변경된 필드들
    LocalDateTime updatedAt,
    
    @JsonProperty("timestamp")
    long timestamp
) {
    public static UserProfileUpdatedEventDTO createDefault(Long userId, String authUserId, String email, String name, String phone, String profileImageUrl, Map<String, Object> changes) {
        return new UserProfileUpdatedEventDTO(
            "user.profile-updated",
            java.util.UUID.randomUUID().toString(),
            userId,
            authUserId,
            email,
            name,
            phone,
            profileImageUrl,
            changes,
            LocalDateTime.now(),
            System.currentTimeMillis()
        );
    }
}