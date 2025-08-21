package ac.su.kdt.beusermanagementservice.controller;

import ac.su.kdt.beusermanagementservice.dto.UpdateProfileRequestDTO;
import ac.su.kdt.beusermanagementservice.dto.UserProfileResponseDTO;
import ac.su.kdt.beusermanagementservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ac.su.kdt.beusermanagementservice.dto.UserDashboardDTO;
import ac.su.kdt.beusermanagementservice.dto.UserPassportDTO;
import ac.su.kdt.beusermanagementservice.dto.UpdateProfileImageRequestDTO;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // GET /api/users/{userId}/profile
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponseDTO> getUserProfile(@PathVariable Long userId) {
        UserProfileResponseDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    // PATCH /api/users/{userId}/profile
    @PatchMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponseDTO> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody UpdateProfileRequestDTO request
    ) {
        UserProfileResponseDTO updatedProfile = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(updatedProfile);
    }

    // "/{userId}/dashboard"는 /api/users/{userId}/dashboard 경로와 매핑
    // GET /api/users/{userId}/dashboard
    @GetMapping("/{userId}/dashboard")
    public ResponseEntity<UserDashboardDTO> getUserDashboard(
            @PathVariable Long userId
    ) {
        UserDashboardDTO dashboard = userService.getUserDashboard(userId);
        return ResponseEntity.ok(dashboard);
    }

    // GET /api/users/{userId}/passport
    @GetMapping("/{userId}/passport")
    public ResponseEntity<UserPassportDTO> getUserPassport(@PathVariable Long userId) {
        UserPassportDTO passport = userService.getUserPassport(userId);
        return ResponseEntity.ok(passport);
    }

    // PUT /api/users/{userId}/profile-image
    @PutMapping("/{userId}/profile-image")
    public ResponseEntity<Void> updateUserProfileImage(
            // URL 경로에서 사용자 ID를 로드
            @PathVariable Long userId,
            // 요청 본문(JSON)을 UpdateProfileImageRequestDTO 객체로 변환
            @RequestBody UpdateProfileImageRequestDTO request
    ) {
        // UserService에 실제 로직 처리를 전달
        userService.updateUserProfileImage(userId, request.profileImageUrl());
        return ResponseEntity.ok().build();
    }
}