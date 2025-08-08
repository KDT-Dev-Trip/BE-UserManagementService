package ac.su.kdt.beusermanagementservice.dto;

public record UserSignedUpEventDTO(
    String auth0Id, // Auth0에서 발급한 고유 사용자 ID
    String email,   // 사용자 이메일
    String name     // 사용자 이름
) {}