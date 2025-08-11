package ac.su.kdt.beusermanagementservice.dto;

import java.util.List;

// 대시보드 API의 최종 응답 데이터 구조
public record UserDashboardDTO(
    UserSimpleInfoDTO userInfo, // 사용자 정보
    List<MissionSummaryDTO> inProgressMissions, // 진행 중인 미션 목록
    List<MissionSummaryDTO> completedMissions  // 완료한 미션 목록
) {}