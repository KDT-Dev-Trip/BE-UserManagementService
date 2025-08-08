package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.dto.CreateTeamRequestDTO;
import ac.su.kdt.beusermanagementservice.dto.JoinTeamRequestDTO;
import ac.su.kdt.beusermanagementservice.dto.TeamResponseDTO;
import ac.su.kdt.beusermanagementservice.entity.Team;
import ac.su.kdt.beusermanagementservice.entity.TeamMembership;
import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.repository.TeamMembershipRepository;
import ac.su.kdt.beusermanagementservice.repository.TeamRepository;
import ac.su.kdt.beusermanagementservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamResponseDTO createTeam(CreateTeamRequestDTO request, Long instructorId) {
        // 1. 팀 생성자 정보 조회
        User instructor = userRepository.findById(instructorId)
            .orElseThrow(() -> new IllegalArgumentException("강사 정보를 찾을 수 없습니다."));

        // 2. 고유한 팀 코드 생성 (UUID의 일부를 사용)
        String teamCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 3. 팀 엔티티 생성 및 저장
        Team newTeam = new Team(request.name(), teamCode, instructor.getId());
        teamRepository.save(newTeam);

        // 4. 팀 생성자를 LEADER 역할로 팀 멤버십에 추가
        TeamMembership membership = new TeamMembership(newTeam, instructor);
        // ERD에 따라 강사는 LEADER가 아닌 INSTRUCTOR 역할이므로, 팀 멤버십의 역할은 별도로 관리될 수 있음
        membership.setRole(TeamMembership.Role.LEADER);
        teamMembershipRepository.save(membership);
        
        // 5. 팀의 현재 인원 수를 업데이트
        newTeam.setCurrentMembers(1);

        // TODO: Kafka로 TeamCreatedEvent 발행 로직 추가 예정

        // 6. 생성된 팀 정보를 DTO로 변환하여 반환
        return TeamResponseDTO.from(newTeam);
    }
}