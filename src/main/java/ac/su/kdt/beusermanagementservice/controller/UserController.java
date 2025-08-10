package ac.su.kdt.beusermanagementservice.controller;

import ac.su.kdt.beusermanagementservice.dto.UpdateProfileRequestDTO;
import ac.su.kdt.beusermanagementservice.dto.UserProfileResponseDTO;
import ac.su.kdt.beusermanagementservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}