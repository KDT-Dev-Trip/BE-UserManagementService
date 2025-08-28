package ac.su.kdt.beusermanagementservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserSyncEventDTO(
        String eventType, // "FULL_SYNC", "UPDATE", "DELETE"
        List<UserSyncDataDTO> users,
        long timestamp
) {}