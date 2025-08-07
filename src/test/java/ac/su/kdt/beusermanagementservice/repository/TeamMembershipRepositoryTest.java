package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.Team;
import ac.su.kdt.beusermanagementservice.entity.TeamMembership;
import ac.su.kdt.beusermanagementservice.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TeamMembershipRepositoryTest {
    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMembershipRepository teamMembershipRepository;

    @Test
    @DisplayName("팀 멤버십 저장 및 관계 확인 테스트")
    void saveTeamMembership() {
        // given
        User instructor = userRepository.save(new User("auth|ins", "ins@test.com", "강사"));
        User student = userRepository.save(new User("auth|stu", "stu@test.com", "학생"));
        Team team = teamRepository.save(new Team("KDT팀", "KDTTEAM", instructor.getId()));
        TeamMembership membership = new TeamMembership(team, student);

        // when
        teamMembershipRepository.save(membership);

        // then
        TeamMembership found = teamMembershipRepository.findById(membership.getId()).orElseThrow();
        // 조회된 멤버십의 Team과 User 객체를 통해 관계가 잘 설정되었는지 검증
        assertThat(found.getTeam().getName()).isEqualTo("KDT팀");
        assertThat(found.getUser().getName()).isEqualTo("학생");
        assertThat(found.getRole()).isEqualTo(TeamMembership.Role.MEMBER);
    }
}