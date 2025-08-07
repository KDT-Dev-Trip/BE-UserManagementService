package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// User 엔티티를 다루며, PK 타입은 Long임을 명시
public interface UserRepository extends JpaRepository<User, Long> {
    // auth0_id를 기준으로 사용자를 찾는 쿼리 메서드
    Optional<User> findByAuth0Id(String auth0Id);
    // email을 기준으로 사용자를 찾는 쿼리 메서드
    Optional<User> findByEmail(String email);
}