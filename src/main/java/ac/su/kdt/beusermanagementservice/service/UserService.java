package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.dto.*;
import ac.su.kdt.beusermanagementservice.entity.SubscriptionPlan;
import ac.su.kdt.beusermanagementservice.entity.Team;
import ac.su.kdt.beusermanagementservice.entity.TeamMembership;
import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ac.su.kdt.beusermanagementservice.dto.UpdateProfileRequestDTO;
import ac.su.kdt.beusermanagementservice.dto.UserProfileResponseDTO;
import ac.su.kdt.beusermanagementservice.repository.TeamMembershipRepository;
import ac.su.kdt.beusermanagementservice.repository.TeamRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
// Lombok 어노테이션으로, final로 선언된 필드들만 인자로 받는 생성자를 자동으로 생성.
// 생성자 주입(Constructor Injection) 방식의 의존성 주입을 간결하게 구현할 수 있음.
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamRepository teamRepository;
    private final UserEventPublisher userEventPublisher;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;


    // 메서드가 성공적으로 완료되면 모든 변경사항이 커밋(commit)되고, 중간에 예외가 발생하면 모든 변경사항이 롤백(rollback)되어 데이터 정합성을 보장
    @Transactional
    public void registerNewUser(UserSignedUpEventDTO event) {
        // Kafka는 최소 한 번 전송을 보장하므로, 동일한 메시지가 중복 수신될 수 있음
        // 처리하기 전에 항상 DB에 해당 사용자가 이미 존재하는지 확인 (authUserId 기준)
        userRepository.findByAuthUserId(event.authUserId()).ifPresentOrElse(
                existingUser -> logger.warn("이미 존재하는 사용자에 대한 가입 이벤트 수신 (멱등 처리): authUserId={}", event.authUserId()),
                () -> {
                    // planType 문자열을 SubscriptionPlan enum으로 변환
                    SubscriptionPlan subscriptionPlan = parseSubscriptionPlan(event.planType());
                    
                    User newUser = new User(event.authUserId(), event.email(), event.name(), subscriptionPlan);
                    userRepository.save(newUser);
                    ticketService.grantInitialTickets(newUser);
                    logger.info("신규 사용자 프로필 생성 및 초기 티켓 지급 완료: authUserId={}, userId={}, plan={}", 
                              event.authUserId(), newUser.getId(), newUser.getSubscriptionPlan());
                }
        );
    }
    
    // planType 문자열을 SubscriptionPlan enum으로 변환하는 헬퍼 메서드
    private SubscriptionPlan parseSubscriptionPlan(String planType) {
        if (planType == null) {
            return SubscriptionPlan.FREE; // 기본값
        }
        try {
            return SubscriptionPlan.valueOf(planType.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("알 수 없는 구독 플랜 타입: {}. FREE 플랜으로 설정합니다.", planType);
            return SubscriptionPlan.FREE;
        }
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
        
        // 변경사항 추적
        Map<String, Object> changes = new HashMap<>();
        if (!user.getName().equals(request.name())) {
            changes.put("name", Map.of("old", user.getName(), "new", request.name()));
        }
        if (!user.getPhone().equals(request.phone())) {
            changes.put("phone", Map.of("old", user.getPhone(), "new", request.phone()));
        }
        
        // 2. 엔티티 내부에 상태 변경 로직을 위임. (엔티티에 updateProfile 메서드 추가 필요)
        user.updateProfile(request.name(), request.phone());
        
        // 3. 변경된 엔티티는 @Transactional에 의해 메서드 종료 시 자동으로 DB에 반영
        UserProfileResponseDTO response = UserProfileResponseDTO.from(user);
        
        // 4. 프로필 업데이트 이벤트 발행
        if (!changes.isEmpty()) {
            try {
                userEventPublisher.publishProfileUpdated(
                    userId, 
                    user.getAuthUserId(), 
                    user.getEmail(), 
                    user.getName(), 
                    user.getPhone(), 
                    user.getProfileImageUrl(),
                    changes
                );
            } catch (Exception e) {
                logger.error("Failed to publish profile updated event for userId: {}", userId, e);
            }
        }
        
        return response;
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

    // 사용자의 '실질적인' 구독 플랜을 계산하는 핵심 메서드
    @Transactional(readOnly = true)
    public SubscriptionPlan getEffectivePlan(User user) {
        // 1. 사용자가 현재 속한 모든 활성(ACTIVE) 팀 멤버십 정보를 조회
        List<TeamMembership> memberships = teamMembershipRepository.findByUserAndStatus(user, TeamMembership.Status.ACTIVE);

        // 2. 멤버십 목록을 순회하며, 팀 리더가 ENTERPRISE 플랜인지 확인
        for (TeamMembership membership : memberships) {
            Team team = membership.getTeam(); // 멤버십 정보에서 팀 정보
            User instructor = userRepository.findById(team.getInstructorId()) // 팀 정보에서 강사(팀 리더)의 ID로 사용자 정보를 조회
                    .orElseThrow(() -> new IllegalStateException("팀의 강사 정보를 찾을 수 없습니다: " + team.getInstructorId()));

            // 3. 만약 팀 리더의 플랜이 ENTERPRISE라면,
            if (instructor.getSubscriptionPlan() == SubscriptionPlan.ENTERPRISE) {
                logger.debug("사용자(id:{})가 ENTERPRISE 팀(id:{})에 속해있어 실질 플랜을 ENTERPRISE로 적용합니다.", user.getId(), team.getId());
                return SubscriptionPlan.ENTERPRISE;
            }
        }

        // 4. 만약 ENTERPRISE 플랜의 팀에 속해있지 않다면, 사용자의 기본 플랜을 반환
        return user.getSubscriptionPlan();
    }

    // 구독 변경 이벤트를 처리하는 로직
    @Transactional
    public void processSubscriptionChange(SubscriptionChangedEventDTO event) {
        User user = userRepository.findById(event.userId()) // userId로 사용자 검색
                .orElseThrow(() -> new IllegalArgumentException("구독 변경 이벤트 처리: 사용자를 찾을 수 없습니다. ID: " + event.userId()));

        user.updateSubscriptionPlan(event.newPlan()); // 1. 사용자의 '기본' 구독 플랜을 새로운 플랜으로 변경

        ticketService.upgradeSubscription(user, event.newPlan()); // 2. TicketService를 호출하여 보너스 티켓을 지급

        logger.info("사용자 구독 플랜 변경 완료: userId={}, newPlan={}", user.getId(), event.newPlan());
    }

    // 유저 프로필 이미지 생성
    @Transactional
    public void updateUserProfileImage(Long userId, String newImageUrl) {
        // userId로 사용자를 찾고, 없으면 예외를 발생
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 이전 이미지 URL 저장
        String oldImageUrl = user.getProfileImageUrl();

        // User 엔티티에 만들어 둘 updateProfileImageUrl 메서드를 호출
        user.updateProfileImageUrl(newImageUrl);

        // 변경사항이 자동으로 DB에 저장(commit)
        logger.info("사용자 프로필 이미지 업데이트 완료: userId={}, newImageUrl={}", userId, newImageUrl);
        
        // 프로필 이미지 변경 이벤트 발행
        try {
            userEventPublisher.publishProfileImageChanged(
                userId,
                user.getAuthUserId(),
                user.getEmail(),
                oldImageUrl,
                newImageUrl,
                "USER_UPDATE"
            );
        } catch (Exception e) {
            logger.error("Failed to publish profile image changed event for userId: {}", userId, e);
        }
    }
    
    // === User Synchronization Methods ===
    
    /**
     * Auth 서비스로부터 받은 동기화 이벤트를 처리
     */
    @Transactional
    public void processSyncEvent(UserSyncEventDTO syncEvent) {
        logger.info("사용자 동기화 이벤트 처리 시작: eventType={}, userCount={}", 
                   syncEvent.eventType(), syncEvent.users().size());
        
        switch (syncEvent.eventType()) {
            case "FULL_SYNC":
                processFullSync(syncEvent.users());
                break;
            case "UPDATE":
                processUserUpdates(syncEvent.users());
                break;
            case "DELETE":
                processUserDeletions(syncEvent.users());
                break;
            default:
                logger.warn("알 수 없는 동기화 이벤트 타입: {}", syncEvent.eventType());
        }
        
        logger.info("사용자 동기화 이벤트 처리 완료: eventType={}", syncEvent.eventType());
    }
    
    /**
     * 전체 동기화 처리 - Auth 서비스의 모든 사용자와 User 서비스를 동기화
     */
    private void processFullSync(List<UserSyncDataDTO> authUsers) {
        logger.info("전체 사용자 동기화 시작: {} users from auth service", authUsers.size());
        
        // 1. Auth 서비스의 모든 사용자를 User 서비스에 생성/업데이트
        for (UserSyncDataDTO userData : authUsers) {
            syncSingleUser(userData);
        }
        
        // 2. Auth 서비스에는 없지만 User 서비스에만 있는 사용자들을 비활성화
        // (실제 삭제하지 않고 상태만 변경)
        var authUserIds = authUsers.stream()
                .map(UserSyncDataDTO::authUserId)
                .toList();
        
        var orphanedUsers = userRepository.findAll().stream()
                .filter(user -> !authUserIds.contains(user.getAuthUserId()))
                .toList();
        
        for (User orphanedUser : orphanedUsers) {
            logger.warn("Auth 서비스에 없는 사용자 발견, 비활성화 처리: authUserId={}, email={}", 
                       orphanedUser.getAuthUserId(), orphanedUser.getEmail());
            // User 엔티티에 status 필드가 있다면 INACTIVE로 설정
            // 현재는 로그만 남김
        }
        
        logger.info("전체 사용자 동기화 완료");
    }
    
    /**
     * 개별 사용자 업데이트 처리
     */
    private void processUserUpdates(List<UserSyncDataDTO> users) {
        for (UserSyncDataDTO userData : users) {
            syncSingleUser(userData);
        }
    }
    
    /**
     * 사용자 삭제 처리
     */
    private void processUserDeletions(List<UserSyncDataDTO> users) {
        for (UserSyncDataDTO userData : users) {
            userRepository.findByAuthUserId(userData.authUserId()).ifPresent(user -> {
                logger.info("사용자 삭제 처리: authUserId={}, email={}", userData.authUserId(), user.getEmail());
                userRepository.delete(user);
            });
        }
    }
    
    /**
     * 개별 사용자 동기화 (생성 또는 업데이트)
     */
    private void syncSingleUser(UserSyncDataDTO userData) {
        if ("DELETED".equals(userData.status())) {
            return; // 삭제된 사용자는 별도 처리
        }
        
        userRepository.findByAuthUserId(userData.authUserId()).ifPresentOrElse(
            existingUser -> {
                // 기존 사용자 업데이트
                updateExistingUser(existingUser, userData);
            },
            () -> {
                // 새 사용자 생성
                createUserFromSyncData(userData);
            }
        );
    }
    
    /**
     * 기존 사용자 정보 업데이트
     */
    private void updateExistingUser(User existingUser, UserSyncDataDTO userData) {
        boolean updated = false;
        
        if (!existingUser.getEmail().equals(userData.email())) {
            existingUser.updateProfile(existingUser.getName(), existingUser.getPhone()); // 이메일 업데이트는 별도 메서드 필요
            updated = true;
        }
        
        if (!existingUser.getName().equals(userData.name())) {
            existingUser.updateProfile(userData.name(), existingUser.getPhone());
            updated = true;
        }
        
        SubscriptionPlan newPlan = parseSubscriptionPlan(userData.planType());
        if (existingUser.getSubscriptionPlan() != newPlan) {
            existingUser.updateSubscriptionPlan(newPlan);
            updated = true;
        }
        
        if (updated) {
            logger.info("사용자 정보 업데이트 완료: authUserId={}, email={}", userData.authUserId(), userData.email());
        }
    }
    
    /**
     * 동기화 데이터로부터 새 사용자 생성
     */
    private void createUserFromSyncData(UserSyncDataDTO userData) {
        SubscriptionPlan subscriptionPlan = parseSubscriptionPlan(userData.planType());
        
        User newUser = new User(userData.authUserId(), userData.email(), userData.name(), subscriptionPlan);
        userRepository.save(newUser);
        ticketService.grantInitialTickets(newUser);
        
        logger.info("동기화를 통한 신규 사용자 생성 완료: authUserId={}, email={}, plan={}", 
                   userData.authUserId(), userData.email(), subscriptionPlan);
    }
    
    /**
     * Auth 서비스에 동기화를 요청
     */
    @Transactional
    public int requestSyncFromAuthService() {
        try {
            // Auth 서비스의 동기화 API 호출
            String authServiceUrl = "http://localhost:8080/auth/sync-users"; // Auth 서비스 URL
            String response = restTemplate.postForObject(authServiceUrl, null, String.class);
            
            logger.info("Auth 서비스 동기화 요청 완료: {}", response);
            
            // Auth 서비스 응답에서 syncedCount 추출 (간단히 현재 User 수 반환)
            return (int) userRepository.count();
            
        } catch (Exception e) {
            logger.error("Auth 서비스 동기화 요청 실패", e);
            throw new RuntimeException("Auth 서비스 동기화 요청 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 동기화 상태 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSyncStatus() {
        try {
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.findAll().stream()
                    .filter(user -> user.getStatus() == User.Status.ACTIVE)
                    .count();
            
            // 최근 생성된 사용자 조회 (마지막 동기화 시간 추정용)
            var recentUsers = userRepository.findAll().stream()
                    .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                    .limit(1)
                    .toList();
            
            // Map.of()는 null을 허용하지 않으므로 HashMap 사용
            Map<String, Object> status = new HashMap<>();
            status.put("totalUsers", totalUsers);
            status.put("activeUsers", activeUsers);
            status.put("inactiveUsers", totalUsers - activeUsers);
            status.put("lastSyncTime", recentUsers.isEmpty() ? "없음" : recentUsers.get(0).getCreatedAt());
            status.put("syncEnabled", true);
            
            return status;
            
        } catch (Exception e) {
            logger.error("동기화 상태 조회 실패", e);
            throw new RuntimeException("동기화 상태 조회 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 현재 사용자 수 조회 (디버깅용)
     */
    @Transactional(readOnly = true)
    public long getUserCount() {
        return userRepository.count();
    }
    
    /**
     * 사용자 로그아웃 이벤트 처리
     */
    @Transactional
    public void processUserLogoutEvent(UserLoggedOutEventDTO event) {
        try {
            // 로그아웃 이벤트 로깅
            logger.info("사용자 로그아웃 이벤트 처리: authUserId={}, email={}, reason={}", 
                       event.authUserId(), event.email(), event.logoutReason());
            
            // 필요시 로그아웃 통계 수집이나 세션 정리 등의 로직을 추가할 수 있음
            // 현재는 단순히 로깅만 수행
            
        } catch (Exception e) {
            logger.error("사용자 로그아웃 이벤트 처리 실패: {}", event, e);
        }
    }
    
    /**
     * 로그인 실패 이벤트 처리
     */
    @Transactional
    public void processLoginFailedEvent(LoginFailedEventDTO event) {
        try {
            // 로그인 실패 이벤트 로깅 및 보안 모니터링
            logger.warn("로그인 실패 이벤트 처리: email={}, reason={}, attemptCount={}, ip={}", 
                       event.email(), event.failureReason(), event.attemptCount(), event.ipAddress());
            
            // 필요시 보안 알림이나 사용자 계정 상태 업데이트 등의 로직을 추가할 수 있음
            // 현재는 단순히 로깅만 수행
            
        } catch (Exception e) {
            logger.error("로그인 실패 이벤트 처리 실패: {}", event, e);
        }
    }
    
    /**
     * 계정 잠금 이벤트 처리
     */
    @Transactional
    public void processAccountLockedEvent(AccountLockedEventDTO event) {
        try {
            // 계정 잠금 이벤트 로깅 및 사용자 상태 업데이트
            logger.warn("계정 잠금 이벤트 처리: authUserId={}, email={}, reason={}, duration={}분", 
                       event.authUserId(), event.email(), event.lockReason(), event.lockDurationMinutes());
            
            // 필요시 User 엔티티에 계정 잠금 상태를 반영하거나 알림을 보내는 로직을 추가할 수 있음
            // 현재는 단순히 로깅만 수행
            
        } catch (Exception e) {
            logger.error("계정 잠금 이벤트 처리 실패: {}", event, e);
        }
    }
    
    /**
     * 사용자의 구독 플랜 정보를 업데이트 (결제 이벤트 처리용)
     */
    @Transactional
    public void updateSubscriptionPlan(Long userId, String planName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 플랜 이름을 SubscriptionPlan enum으로 변환
        SubscriptionPlan plan = convertPlanNameToEnum(planName);
        user.updateSubscriptionPlan(plan);
        userRepository.save(user);
        
        logger.info("사용자 구독 플랜 캐시 업데이트: userId={}, plan={}", userId, plan);
    }
    
    /**
     * 사용자 조회 (결제 이벤트 처리용)
     */
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
    
    /**
     * 플랜 이름을 SubscriptionPlan enum으로 변환
     */
    private SubscriptionPlan convertPlanNameToEnum(String planName) {
        if (planName == null) return SubscriptionPlan.FREE;
        
        return switch (planName.toUpperCase()) {
            case "ECONOMY_CLASS", "ECONOMY" -> SubscriptionPlan.ECONOMY;
            case "BUSINESS_CLASS", "BUSINESS" -> SubscriptionPlan.BUSINESS;
            case "FIRST_CLASS", "FIRST" -> SubscriptionPlan.FIRST;
            case "ENTERPRISE_CLASS", "ENTERPRISE" -> SubscriptionPlan.ENTERPRISE;
            default -> SubscriptionPlan.FREE;
        };
    }
}