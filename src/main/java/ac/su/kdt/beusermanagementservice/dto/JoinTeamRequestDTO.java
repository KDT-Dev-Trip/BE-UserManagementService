package ac.su.kdt.beusermanagementservice.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinTeamRequestDTO(
    @NotBlank(message = "초대 코드는 필수입니다.")
    String teamCode
) {}