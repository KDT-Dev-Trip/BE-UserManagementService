package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.dto.*;
import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ac.su.kdt.beusermanagementservice.dto.UpdateProfileRequestDTO;
import ac.su.kdt.beusermanagementservice.dto.UserProfileResponseDTO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
// Lombok 어노테이션으로, final로 선언된 필드들만 인자로 받는 생성자를 자동으로 생성.
// 생성자 주입(Constructor Injection) 방식의 의존성 주입을 간결하게 구현할 수 있음.
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;


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

    // 사용자 ID로 프로필 정보를 조회
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getUserProfile(Long userId) {
        // 1. DB에서 사용자 정보를 검색
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        // 2. 찾은 User 엔티티를 DTO로 변환하여 반환
        return UserProfileResponseDTO.from(user);
    }

    // 사용자 프로필 정보를 수정
    @Transactional
    public UserProfileResponseDTO updateUserProfile(Long userId, UpdateProfileRequestDTO request) {
        // 1. DB에서 수정할 사용자 정보를 검색
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        // 2. 엔티티 내부에 상태 변경 로직을 위임. (엔티티에 updateProfile 메서드 추가 필요)
        user.updateProfile(request.name(), request.phone());
        // 3. 변경된 엔티티는 @Transactional에 의해 메서드 종료 시 자동으로 DB에 반영
        return UserProfileResponseDTO.from(user);
    }

    // 여권에 도장을 추가
    @Transactional
    public void addStamp(Long userId, Long missionId) {
        // 1. DB에서 도장을 추가할 사용자 정보를 검색
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // TODO: ERD에 도장(stamp) 관련 테이블이 없으므로, 추후 별도의 Stamp 엔티티를 만들거나
        // User 엔티티에 stamp_count와 같은 컬럼을 추가
        // 지금은 로직이 호출되었음을 확인하는 로그만 남깁니다.
        logger.info("사용자 ID {}에게 미션 ID {} 완료 도장을 추가합니다.", userId, missionId);
    }

    // 사용자 대시보드 정보 조회
    @Transactional(readOnly = true) // DB 변경이 없는 조회 작업 -> readOnly=true로 설정하여 성능을 최적화
    public UserDashboardDTO getUserDashboard(Long userId) {
        // 1. UserSvc DB에서 사용자 기본 정보를 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. MissionSvc API를 호출하여 학습 이력 데이터 로드
        // "http://mission-service": 실제 서비스 이름. Kubernetes 환경에서는 이 이름으로 서비스를 찾을 수 있음
        String missionServiceUrl = "http://mission-service/api/missions/attempts?userId=" + userId;
        // API 호출 실패에 대비하여 기본값으로 빈 리스트 설정
        List<MissionSummaryDTO> allMissions = Collections.emptyList();
        try {
            // RestTemplate을 사용하여 GET 요청을 보내고, 응답을 문자열(JSON)로 받음
            String responseJson = restTemplate.getForObject(missionServiceUrl, String.class);
            // ObjectMapper를 사용하여 받은 JSON 문자열을 MissionSummaryDTO 객체의 리스트로 변환
            // TypeReference는 제네릭 타입을 명확히 지정하기 위해 사용.
            allMissions = objectMapper.readValue(responseJson, new TypeReference<>() {});
        } catch (Exception e) {
            // MissionSvc가 응답하지 않거나 에러가 발생해도 UserSvc 전체가 멈추지 않도록 예외 처리
            logger.error("Mission 서비스 호출 중 오류 발생: {}", e.getMessage());
            // 실패 시 빈 목록을 반환, 대시보드의 일부(사용자 정보)만이라도 보여주도록 설정
        }

        // 3. Java Stream API를 사용하여 받아온 데이터를 '진행 중'과 '완료'로 분류
        List<MissionSummaryDTO> inProgressMissions = allMissions.stream()
                // .filter(): 리스트의 각 요소에 대해 조건을 검사하여 true인 요소만 남김
                .filter(m -> "IN_PROGRESS".equals(m.status()))
                // .collect(): 스트림의 요소들을 새로운 리스트로 수집
                .collect(Collectors.toList());
        List<MissionSummaryDTO> completedMissions = allMissions.stream()
                .filter(m -> "COMPLETED".equals(m.status()))
                .collect(Collectors.toList());

        // 4. 모든 데이터를 조합하여 최종적인 UserDashboardDTO 객체를 만들어 반환
        return new UserDashboardDTO(UserSimpleInfoDTO.from(user), inProgressMissions, completedMissions);
    }

    // 사용자 여권 정보 조회
    @Transactional(readOnly = true)
    public UserPassportDTO getUserPassport(Long userId) {
        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. MissionSvc에 '완료된' 미션만 요청하는 API 호출
        String completedMissionsUrl = "http://mission-service/api/missions/attempts?userId=" + userId + "&status=COMPLETED";
        // API 호출 실패에 대비하여 기본값으로 빈 리스트 설정
        List<MissionSummaryDTO> completedMissions = Collections.emptyList();
        try {
            // RestTemplate을 사용하여 GET 요청을 보내고 응답을 문자열로 받음
            String responseJson = restTemplate.getForObject(completedMissionsUrl, String.class);
            completedMissions = objectMapper.readValue(responseJson, new TypeReference<>() {});
        } catch (Exception e) {
            logger.error("Mission 서비스(완료) 호출 중 오류 발생: {}", e.getMessage());
        }

        // 3. 조회된 사용자 정보와 MissionSvc로부터 받은 데이터를 조합하여 최종 DTO 반환
        return new UserPassportDTO(user.getName(), completedMissions.size(), completedMissions);
    }
}