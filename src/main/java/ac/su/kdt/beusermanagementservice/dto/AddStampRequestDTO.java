package ac.su.kdt.beusermanagementservice.dto;

// 여권 도장 추가 요청에 사용될 DTO
public record AddStampRequestDTO(Long userId, Long missionId) {}