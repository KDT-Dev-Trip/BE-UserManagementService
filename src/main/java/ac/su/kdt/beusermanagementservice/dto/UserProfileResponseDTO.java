package ac.su.kdt.beusermanagementservice.dto;

import ac.su.kdt.beusermanagementservice.entity.User;
// 사용자 프로필 조회 응답에 사용될 DTO
public record UserProfileResponseDTO(Long id, String email, String name, String phone, User.Role role) {
    // User 엔티티를 DTO로 변환하는 정적 팩토리 메서드
    public static UserProfileResponseDTO from(User user) {
        return new UserProfileResponseDTO(user.getId(), user.getEmail(), user.getName(), user.getPhone(), user.getRole());
    }
}