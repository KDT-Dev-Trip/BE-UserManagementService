package ac.su.kdt.beusermanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record UserProfileImageChangedEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    @JsonProperty("user_id")
    Long userId,
    
    @JsonProperty("auth_user_id")
    String authUserId,
    
    String email,
    String oldImageUrl,
    String newImageUrl,
    String changeReason, // "USER_UPDATE", "ADMIN_MODERATION", etc.
    LocalDateTime changedAt,
    
    @JsonProperty("timestamp")
    long timestamp
) {
    public static UserProfileImageChangedEventDTO createDefault(Long userId, String authUserId, String email, String oldImageUrl, String newImageUrl, String changeReason) {
        return new UserProfileImageChangedEventDTO(
            "user.profile-image-changed",
            java.util.UUID.randomUUID().toString(),
            userId,
            authUserId,
            email,
            oldImageUrl,
            newImageUrl,
            changeReason,
            LocalDateTime.now(),
            System.currentTimeMillis()
        );
    }
}