package ac.su.kdt.beusermanagementservice.dto;

// 팀 생성 완료 시 Kafka에 발행
// record는 데이터를 불변(immutable) 객체로 편리하게 다룰 수 있게 해줌
public record TeamCreatedEventDTO(
    Long teamId,        // 생성된 팀의 고유 ID
    String teamName,    // 생성된 팀의 이름
    Long instructorId   // 팀을 생성한 강사(사용자)의 ID
) {}