package ac.su.kdt.beusermanagementservice.controller;

import ac.su.kdt.beusermanagementservice.dto.AddStampRequestDTO;
import ac.su.kdt.beusermanagementservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 책임 분리를 위해 여권 관련 API는 별도의 컨트롤러로 관리
@RestController
@RequestMapping("/api/passport")
@RequiredArgsConstructor
public class PassportController {
    private final UserService userService; // 지금은 UserService를 공유하지만, 나중에 PassportService로 분리할 수 있음

    // POST /api/passport/stamps
    @PostMapping("/stamps")
    public ResponseEntity<Void> addPassportStamp(@RequestBody AddStampRequestDTO request) {
        userService.addStamp(request.userId(), request.missionId());
        // 성공적으로 처리되었고, 별도의 응답 본문이 필요 없으므로 ok()만 반환
        return ResponseEntity.ok().build();
    }
}