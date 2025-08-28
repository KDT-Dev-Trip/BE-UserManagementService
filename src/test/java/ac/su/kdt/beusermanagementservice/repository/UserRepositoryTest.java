package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.entity.SubscriptionPlan; // 아직 User 클래스가 없어 에러 발생
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository; // 아직 UserRepository 인터페이스가 없어 에러 발생

    @Test
    @DisplayName("사용자 저장 및 auth0_id로 조회 테스트")
    void saveAndFindByAuth0Id() {
        // given
        // User 클래스의 생성자를 사용하여 새로운 사용자 객체를 생성 (아직 User 클래스가 없어 에러 발생)
        User newUser = new User("auth0|test-user-123", "test@example.com", "테스트유저", SubscriptionPlan.FREE);

        // when
        // userRepository의 save 메서드를 호출하여 newUser 객체를 데이터베이스에 저장
        userRepository.save(newUser);

        // then
        // userRepository의 findByAuth0Id 메서드를 호출하여 방금 저장한 사용자를 다시 조회
        User foundUser = userRepository.findByAuth0Id("auth0|test-user-123").orElseThrow();

        // 조회된 사용자의 auth0Id가 처음에 설정한 auth0Id와 같은지 확인
        assertThat(foundUser.getAuth0Id()).isEqualTo(newUser.getAuth0Id());
        // 조회된 사용자의 이름이 처음에 설정한 이름과 같은지 확인
        assertThat(foundUser.getName()).isEqualTo(newUser.getName());
    }
}
