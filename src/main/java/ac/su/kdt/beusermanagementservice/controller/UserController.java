package ac.su.kdt.beusermanagementservice.controller;

import ac.su.kdt.beusermanagementservice.dto.UpdateProfileRequestDTO;
import ac.su.kdt.beusermanagementservice.dto.UserProfileResponseDTO;
import ac.su.kdt.beusermanagementservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ac.su.kdt.beusermanagementservice.dto.UserDashboardDTO;
import ac.su.kdt.beusermanagementservice.dto.UserPassportDTO;
import ac.su.kdt.beusermanagementservice.dto.UpdateProfileImageRequestDTO;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "사용자 관리 API")
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}/profile")
    @Operation(summary = "Get User Profile", description = "사용자 프로필 정보를 조회합니다.")
    public ResponseEntity<UserProfileResponseDTO> getUserProfile(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        UserProfileResponseDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/{userId}/profile")
    @Operation(summary = "Update User Profile", description = "사용자 프로필 정보를 수정합니다.")
    public ResponseEntity<UserProfileResponseDTO> updateUserProfile(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "프로필 수정 요청 데이터") @RequestBody UpdateProfileRequestDTO request
    ) {
        UserProfileResponseDTO updatedProfile = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/{userId}/dashboard")
    @Operation(summary = "Get User Dashboard", description = "사용자 대시보드 정보를 조회합니다.")
    public ResponseEntity<UserDashboardDTO> getUserDashboard(
            @Parameter(description = "사용자 ID") @PathVariable Long userId
    ) {
        UserDashboardDTO dashboard = userService.getUserDashboard(userId);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/{userId}/passport")
    @Operation(summary = "Get User Passport", description = "사용자 여권 정보를 조회합니다.")
    public ResponseEntity<UserPassportDTO> getUserPassport(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        UserPassportDTO passport = userService.getUserPassport(userId);
        return ResponseEntity.ok(passport);
    }

    // PUT /api/users/{userId}/profile-image
    @PutMapping("/{userId}/profile-image")
    @Operation(summary = "Update User Profile Image", description = "사용자 프로필 이미지를 업데이트합니다.")
    public ResponseEntity<Void> updateUserProfileImage(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "프로필 이미지 URL 요청 데이터") @RequestBody UpdateProfileImageRequestDTO request
    ) {
        // UserService에 실제 로직 처리를 전달
        userService.updateUserProfileImage(userId, request.profileImageUrl());
        return ResponseEntity.ok().build();
    }

    // POST /api/users/request-sync-from-auth
    @PostMapping("/request-sync-from-auth")
    @Operation(
        summary = "Request User Sync from Auth Service", 
        description = "Auth 서비스에 사용자 동기화를 요청합니다."
    )
    public ResponseEntity<?> requestSyncFromAuth() {
        try {
            int syncedCount = userService.requestSyncFromAuthService();
            
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Auth 서비스에 동기화를 요청했습니다",
                "syncedCount", syncedCount
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                        "success", false,
                        "error", "SYNC_REQUEST_FAILED", 
                        "message", "동기화 요청 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    // GET /api/users/sync-status
    @GetMapping("/sync-status")
    @Operation(
        summary = "Get User Sync Status", 
        description = "사용자 동기화 상태를 조회합니다."
    )
    public ResponseEntity<?> getSyncStatus() {
        try {
            Map<String, Object> syncStatus = userService.getSyncStatus();
            return ResponseEntity.ok(syncStatus);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                        "error", "SYNC_STATUS_ERROR", 
                        "message", "동기화 상태 조회 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    // GET /api/users/debug/count
    @GetMapping("/debug/count")
    @Operation(summary = "Debug - Get User Count", description = "디버깅용: 현재 User 서비스의 사용자 수를 확인합니다.")
    public ResponseEntity<?> getUserCount() {
        try {
            long userCount = userService.getUserCount();
            return ResponseEntity.ok(Map.of(
                "userCount", userCount,
                "message", userCount == 0 ? "사용자가 없습니다. 동기화가 필요할 수 있습니다." : userCount + "명의 사용자가 있습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                        "error", "COUNT_ERROR", 
                        "message", "사용자 수 조회 중 오류 발생: " + e.getMessage()
                    ));
        }
    }

    // GET /api/users/list
    @GetMapping("/list")
    @Operation(summary = "Get User List", description = "테스트용: 모든 사용자 목록을 조회합니다.")
    public ResponseEntity<?> getUserList() {
        try {
            var userList = userService.getAllUsers();
            return ResponseEntity.ok(userList);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                        "error", "USER_LIST_ERROR", 
                        "message", "사용자 목록 조회 중 오류 발생: " + e.getMessage()
                    ));
        }
    }
}