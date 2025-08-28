package ac.su.kdt.beusermanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record TeamCreatedEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    @JsonProperty("team_id")
    Long teamId,
    
    @JsonProperty("creator_user_id")
    Long creatorUserId,
    
    @JsonProperty("creator_auth_user_id")
    String creatorAuthUserId,
    
    String teamName,
    String teamDescription,
    LocalDateTime createdAt,
    Integer maxMembers,
    
    @JsonProperty("timestamp")
    long timestamp
) {
    public static TeamCreatedEventDTO createDefault(Long teamId, Long creatorUserId, String creatorAuthUserId, String teamName, String teamDescription, Integer maxMembers) {
        return new TeamCreatedEventDTO(
            "auth.team-created",
            java.util.UUID.randomUUID().toString(),
            teamId,
            creatorUserId,
            creatorAuthUserId,
            teamName,
            teamDescription,
            LocalDateTime.now(),
            maxMembers,
            System.currentTimeMillis()
        );
    }
}