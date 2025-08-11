package ac.su.kdt.beusermanagementservice.dto;

public record TicketBalanceResponseDTO(
    // ## 사용자 ID
    Long userId,
    // ## 티켓 잔액
    int ticketBalance
) {}