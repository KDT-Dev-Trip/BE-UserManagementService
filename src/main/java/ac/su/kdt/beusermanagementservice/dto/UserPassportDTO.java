package ac.su.kdt.beusermanagementservice.dto;

import java.util.List;

// 여권 조회 API의 최종 응답 데이터
public record UserPassportDTO(
    String userName, // 사용자 이름
    int completedMissionsCount, // 완료한 미션 총 개수
    List<MissionSummaryDTO> completedMissions // 완료한 미션 상세 목록
) {}