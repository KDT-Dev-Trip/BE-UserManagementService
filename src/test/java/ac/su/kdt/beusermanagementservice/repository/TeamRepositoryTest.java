package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.Team;
import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.entity.SubscriptionPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TeamRepositoryTest {
    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;

    @Test
    @DisplayName("팀 저장 및 고유 팀 코드로 조회 테스트")
    void saveAndFindByTeamCode() {
        // given
        User instructor = userRepository.save(new User("auth|ins", "ins@test.com", "강사", SubscriptionPlan.FREE));
        Team newTeam = new Team("테스트팀", "UNIQUECODE123", instructor.getId());

        // when
        teamRepository.save(newTeam);

        // then
        Team foundTeam = teamRepository.findByTeamCode("UNIQUECODE123").orElseThrow();
        assertThat(foundTeam.getId()).isNotNull();
        assertThat(foundTeam.getName()).isEqualTo("테스트팀");
        assertThat(foundTeam.getInstructorId()).isEqualTo(instructor.getId());
    }
}