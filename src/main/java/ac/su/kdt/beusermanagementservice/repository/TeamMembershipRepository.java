package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.Team;
import ac.su.kdt.beusermanagementservice.entity.TeamMembership;
import ac.su.kdt.beusermanagementservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {
    boolean existsByTeamAndUser(Team team, User user);
}