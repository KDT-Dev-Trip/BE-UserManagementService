package ac.su.kdt.beusermanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// public record: 불변 데이터 객체를 간결하게 정의
// 팀 생성을 요청할 때 클라이언트가 보내는 데이터의 구조
public record CreateTeamRequestDTO(
    @NotBlank(message = "팀 이름은 필수입니다.")
    @Size(min = 2, max = 20, message = "팀 이름은 2자 이상 20자 이하로 입력해주세요.")
    String name
) {}