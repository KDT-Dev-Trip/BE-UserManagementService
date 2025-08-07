package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.User;
import org.junit.jupiter.api.DisplayName; // 테스트의 목적을 명확하게 설명하는 이름을 부여
import org.junit.jupiter.api.Test; // 테스트 메서드임을 선언
import org.springframework.beans.factory.annotation.Autowired; // SPring이 관리하는 UserRepository 빈을 이 필드에 자동으로 주입
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest; // JAP 관련 테스트 환경을 자동으로 설정

import static org.assertj.core.api.Assertions.assertThat; // AsserJ 라이브러리의 정적 메서드로, 테스트 결과 검증을 위해 사용


@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자 정보 저장 및 이메일로 조회 테스트")
    void saveAndFindByEmail(){
        // given
        // 테스트를 위해 User 객체를 생성하고 필요한 데이터를 설정
        User newUser = User.createUser("test@example.com", "hashed-password","테스트유저");

        // when
        // 테스트하려는 핵심 로직, 즉 user레파지토리의 save() 메서드를 실행하여 사용자 정보를 데이터베이스에 저장
        userRepository.save(newUser);

        // then (검증 단계)
        // 1. userRepository.findByEmail() 메서드를 호출하여 저장된 사용자 정보를 이메일로 조회
        // 2. orElseThrow(): 만약 조회 결과가 없으면(Optional.empty) 예외를 발생
        // 3. assertThat(foundUser.getEmail()).isEqualTo(newUser.getEmail()): AssertJ를 사용하여 조회된 사용자의 이메일이
        //    처음에 저장한 사용자의 이메일과 일치하는지 검증. 일치하지 않으면 테스트는 실패
        User foundUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(foundUser.getEmail()).isEqualTo(newUser.getEmail());
    }
}
