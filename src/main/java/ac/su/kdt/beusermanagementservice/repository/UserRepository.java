package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository<엔티티, 기본키타입>을 상속받아 기본적인 CRUD 메서드를 자동 생성
public interface UserRepository extends JpaRepository<User, String> {
    // 메서드 이름 규칙(Query Method)만으로도 Spring이 쿼리를 자동으로 생성
    Optional<User> findByEmail(String email);
}