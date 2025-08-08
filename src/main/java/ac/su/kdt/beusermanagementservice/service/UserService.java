package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.dto.UserSignedUpEventDTO;
import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 비즈니스 로직을 처리하는 서비스 계층의 컴포넌트임을 Spring에 알림
@Service
// Lombok 어노테이션으로, final로 선언된 필드들만 인자로 받는 생성자를 자동으로 생성.
// 생성자 주입(Constructor Injection) 방식의 의존성 주입을 간결하게 구현할 수 있음.
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    // 메서드가 성공적으로 완료되면 모든 변경사항이 커밋(commit)되고, 중간에 예외가 발생하면 모든 변경사항이 롤백(rollback)되어 데이터 정합성을 보장.
    @Transactional
    public void registerNewUser(UserSignedUpEventDTO event) {
        // 멱등성 보장
        // Kafka는 최소 한 번 전송를 보장하므로, 동일한 메시지가 중복 수신될 수 있음.
        // 처리하기 전에 항상 DB에 해당 사용자가 이미 존재하는지 확인
        userRepository.findByAuth0Id(event.auth0Id()).ifPresentOrElse(
            // 값이 있을 경우 (이미 사용자가 존재함)
            existingUser -> logger.warn("이미 존재하는 사용자에 대한 가입 이벤트 수신 (멱등 처리): {}", event.auth0Id()),
            // 값이 없을 경우 (신규 사용자임)
            () -> {
                // 새로운 User 엔티티 객체를 생성
                User newUser = new User(event.auth0Id(), event.email(), event.name());
                // 생성된 엔티티를 데이터베이스에 저장
                userRepository.save(newUser);
                // 성공적으로 처리되었음을 로그에 기록
                logger.info("신규 사용자 프로필 생성 완료: auth0Id={}, email={}", newUser.getAuth0Id(), newUser.getEmail());
            }
        );
    }
}