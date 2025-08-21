package ac.su.kdt.beusermanagementservice.dto;

// 프로필 이미지 URL 변경 요청을 담는 DTO
public record UpdateProfileImageRequestDTO(
    String profileImageUrl // 새로 변경할 프로필 이미지의 URL
) {}