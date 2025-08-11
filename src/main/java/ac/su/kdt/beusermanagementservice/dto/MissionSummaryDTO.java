package ac.su.kdt.beusermanagementservice.dto;

// MissionSvc로부터 받은 미션 정보
public record MissionSummaryDTO(Long missionId, String title, String status) {}