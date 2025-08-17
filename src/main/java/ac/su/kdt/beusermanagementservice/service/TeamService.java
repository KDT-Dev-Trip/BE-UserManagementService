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
import ac.su.kdt.beusermanagementservice.dto.TeamCreatedEventDTO;
import org.springframework.kafka.core.KafkaTemplate;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TEAM_EVENTS_TOPIC = "team-events";

    @Transactional
    public TeamResponseDTO createTeam(CreateTeamRequestDTO request, Long instructorId) {
        // 1. 팀 생성자 정보 조회
        User instructor = userRepository.findById(instructorId)
            .orElseThrow(() -> new IllegalArgumentException("강사 정보를 찾을 수 없습니다."));

        // 2. 고유한 팀 코드 생성 (UUID의 일부를 사용, 중복되지 않는 랜덤한 8자리 팀 코드를 생성)
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

        // Kafka로 TeamCreatedEvent 발행 로직
        // 1). 다른 서비스에 전달할 이벤트 DTO를 생성
        TeamCreatedEventDTO event = new TeamCreatedEventDTO(newTeam.getId(), newTeam.getName(), instructorId);
        // 2). kafkaTemplate.send() 메서드를 사용하여 지정된 토픽으로 이벤트를 발행(전송)
        kafkaTemplate.send(TEAM_EVENTS_TOPIC, event);

        // 6. 생성된 팀 정보를 DTO로 변환하여 반환
        return TeamResponseDTO.from(newTeam);
    }
    @Transactional
    public TeamResponseDTO joinTeam(JoinTeamRequestDTO request, Long studentId) {
        // 1. 초대 코드로 참가할 팀을 조회
        Team teamToJoin = teamRepository.findByTeamCode(request.teamCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 팀 코드입니다."));

        // 2. 참가하려는 학생 정보를 조회
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        // 3. 이미 팀에 속해 있는지 확인하여 중복 참가를 방지
        if (teamMembershipRepository.existsByTeamAndUser(teamToJoin, student)) {
            throw new IllegalStateException("이미 해당 팀에 속해 있습니다.");
        }

        // 4. 팀 인원 제한을 확인
        if (teamToJoin.getCurrentMembers() >= teamToJoin.getMaxMembers()) {
            throw new IllegalStateException("팀의 최대 인원을 초과하여 참가할 수 없습니다.");
        }

        // 5. 학생을 MEMBER 역할로 팀 멤버십에 추가하고 저장
        TeamMembership newMembership = new TeamMembership(teamToJoin, student, TeamMembership.Role.MEMBER);
        teamMembershipRepository.save(newMembership);

        // 6. 팀의 현재 인원 수를 1 증가
        teamToJoin.updateCurrentMembers(teamToJoin.getCurrentMembers() + 1);

        return TeamResponseDTO.from(teamToJoin);
    }
}