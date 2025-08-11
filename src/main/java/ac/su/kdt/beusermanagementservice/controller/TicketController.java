package ac.su.kdt.beusermanagementservice.controller;

import ac.su.kdt.beusermanagementservice.dto.TicketBalanceResponseDTO;
import ac.su.kdt.beusermanagementservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users/{userId}/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    // GET /api/users/{userId}/tickets : 티켓 잔액 조회 API
    @GetMapping
    public ResponseEntity<TicketBalanceResponseDTO> getTicketBalance(
            // URL 경로의 일부({userId})를 메서드 파라미터로 받아옴
            @PathVariable Long userId) {
        // 서비스 계층에 잔액 조회 로직을 넘김
        int balance = ticketService.getTicketBalance(userId);
        return ResponseEntity.ok(new TicketBalanceResponseDTO(userId, balance));
    }

    // POST /api/users/{userId}/tickets/consume: 티켓 사용 API
    @PostMapping("/consume")
    public ResponseEntity<Void> consumeTicket(
            @PathVariable Long userId,
            @RequestBody Map<String, String> requestBody) {
        // 요청 본문에서 'reason' 값을 가져오고, 없으면 기본값을 사용
        String reason = requestBody.getOrDefault("reason", "미션 시작");
        // 서비스 계층에 티켓 사용 로직을 넘김
        ticketService.consumeTicket(userId, reason);
        // 성공 처리, 별도의 응답 본문이 필요 없으므로 ok()와 빈 Body를 반환
        return ResponseEntity.ok().build();
    }
}