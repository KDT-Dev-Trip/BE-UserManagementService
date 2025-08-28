package ac.su.kdt.beusermanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

public record UserSettingsChangedEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    @JsonProperty("user_id")
    Long userId,
    
    @JsonProperty("auth_user_id")
    String authUserId,
    
    String email,
    String settingCategory, // "PRIVACY", "NOTIFICATION", "PREFERENCES", etc.
    Map<String, Object> oldSettings,
    Map<String, Object> newSettings,
    LocalDateTime changedAt,
    
    @JsonProperty("timestamp")
    long timestamp
) {
    public static UserSettingsChangedEventDTO createDefault(Long userId, String authUserId, String email, String settingCategory, Map<String, Object> oldSettings, Map<String, Object> newSettings) {
        return new UserSettingsChangedEventDTO(
            "user.settings-changed",
            java.util.UUID.randomUUID().toString(),
            userId,
            authUserId,
            email,
            settingCategory,
            oldSettings,
            newSettings,
            LocalDateTime.now(),
            System.currentTimeMillis()
        );
    }
}