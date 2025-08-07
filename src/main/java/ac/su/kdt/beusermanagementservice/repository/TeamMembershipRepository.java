package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.TeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {}