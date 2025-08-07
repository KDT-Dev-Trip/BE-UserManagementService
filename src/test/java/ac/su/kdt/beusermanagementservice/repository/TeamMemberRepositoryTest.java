package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.Team;
import ac.su.kdt.beusermanagementservice.entity.TeamMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TeamMemberRepositoryTest {
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Test
    @DisplayName("팀 멤버 저장 및 팀과의 관계 확인 테스트")
    void saveAndVerifyRelation() {
        // given
        // 1. 먼저 부모 엔티티인 Team을 생성하고 저장
        Team newTeam = Team.createTeam("test팀", "owner-id-123", 10);
        teamRepository.save(newTeam);
        // 2. 자식 엔티티인 TeamMember를 생성
        TeamMember newTeamMember = TeamMember.createTeamMember(newTeam, "member-id-456", TeamMember.TeamMemberRole.MEMBER);

        // when
        // 3. TeamMember를 저장
        teamMemberRepository.save(newTeamMember);

        // then
        // 4. 저장된 TeamMember를 조회, 관계가 올바른지 확인
        TeamMember foundMember = teamMemberRepository.findById(newTeamMember.getId()).orElseThrow();
        assertThat(foundMember.getTeam().getId()).isEqualTo(newTeam.getId());
    }
}