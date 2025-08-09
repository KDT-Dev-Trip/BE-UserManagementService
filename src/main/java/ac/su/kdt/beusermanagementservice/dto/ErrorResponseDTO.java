package ac.su.kdt.beusermanagementservice.dto;
// 에러 발생 시 클라이언트에게 일관된 형식으로 응답하기 위한 DTO
public record ErrorResponseDTO(String errorCode, String message) {}