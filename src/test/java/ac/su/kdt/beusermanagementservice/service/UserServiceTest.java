// src/test/java/ac/su/kdt/beusermanagementservice/service/UserServiceTest.java
package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.dto.UserSignedUpEventDTO; // 아직 DTO가 없어 에러 발생
import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.entity.SubscriptionPlan;
import ac.su.kdt.beusermanagementservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;
    
    @MockitoBean
    private TicketService ticketService;

    @Test
    @DisplayName("신규 회원가입 이벤트 수신 시 사용자 프로필 생성 성공 테스트")
    void registerNewUser_whenNewUser_shouldSaveUser() {
        // given
        UserSignedUpEventDTO event = new UserSignedUpEventDTO(
            "auth0|new", 
            "new@test.com", 
            "신규유저", 
            "FREE",
            LocalDateTime.now(),
            "EMAIL",
            "none",
            "127.0.0.1",
            System.currentTimeMillis()
        );
        // 아직 가입되지 않은 사용자임을 시뮬레이션
        given(userRepository.findByAuthUserId(event.authUserId())).willReturn(Optional.empty());

        // when
        userService.registerNewUser(event);

        // then
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("중복 회원가입 이벤트 수신 시 사용자 프로필 생성 안함 테스트 (멱등성)")
    void registerNewUser_whenExistingUser_shouldNotSaveUser() {
        // given
        UserSignedUpEventDTO event = new UserSignedUpEventDTO(
            "auth0|existing", 
            "exist@test.com", 
            "기존유저", 
            "FREE",
            LocalDateTime.now(),
            "EMAIL",
            "none",
            "127.0.0.1",
            System.currentTimeMillis()
        );
        given(userRepository.findByAuthUserId(event.authUserId())).willReturn(Optional.of(new User(event.authUserId(), event.email(), event.name(), SubscriptionPlan.FREE)));

        // when
        userService.registerNewUser(event);

        // then
        then(userRepository).should(never()).save(any(User.class));
    }
}