package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TeamRepositoryTest {
    @Autowired
    private TeamRepository teamRepository;

    @Test
    @DisplayName("팀 저장 및 초대코드로 조회 테스트")
    void saveAndFindByInviteCode() {
        // given
        Team newTeam = Team.createTeam("test팀","owner-id-123",10);

        // when
        teamRepository.save(newTeam);

        // then
        Team foundTeam = teamRepository.findByInviteCode(newTeam.getInviteCode()).orElseThrow();
        assertThat(foundTeam.getId()).isEqualTo(newTeam.getId());
    }
}
