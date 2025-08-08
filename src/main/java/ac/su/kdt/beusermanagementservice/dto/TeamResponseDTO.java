package ac.su.kdt.beusermanagementservice.dto;

import ac.su.kdt.beusermanagementservice.entity.Team;

// 팀 관련 API의 성공 응답으로 클라이언트에게 반환할 데이터 구조
public record TeamResponseDTO(
    Long id,
    String name,
    String teamCode,
    Long instructorId,
    int currentMembers,
    int maxMembers
) {
    // 정적 팩토리 메서드: Team 엔티티 객체를 TeamResponseDTO로 변환하는 로직을 캡슐화
    public static TeamResponseDTO from(Team team) {
        return new TeamResponseDTO(
            team.getId(),
            team.getName(),
            team.getTeamCode(),
            team.getInstructorId(),
            team.getCurrentMembers(),
            team.getMaxMembers()
        );
    }
}