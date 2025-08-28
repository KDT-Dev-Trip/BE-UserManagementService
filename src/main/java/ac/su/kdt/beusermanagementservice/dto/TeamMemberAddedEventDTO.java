package ac.su.kdt.beusermanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record TeamMemberAddedEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    @JsonProperty("team_id")
    Long teamId,
    
    @JsonProperty("user_id")
    Long userId,
    
    @JsonProperty("auth_user_id")
    String authUserId,
    
    @JsonProperty("added_by_user_id")
    Long addedByUserId,
    
    String teamName,
    String userEmail,
    String userName,
    String memberRole, // "LEADER", "MEMBER", "VIEWER"
    LocalDateTime joinedAt,
    
    @JsonProperty("timestamp")
    long timestamp
) {
    public static TeamMemberAddedEventDTO createDefault(Long teamId, Long userId, String authUserId, Long addedByUserId, String teamName, String userEmail, String userName, String memberRole) {
        return new TeamMemberAddedEventDTO(
            "auth.team-member-added",
            java.util.UUID.randomUUID().toString(),
            teamId,
            userId,
            authUserId,
            addedByUserId,
            teamName,
            userEmail,
            userName,
            memberRole,
            LocalDateTime.now(),
            System.currentTimeMillis()
        );
    }
}