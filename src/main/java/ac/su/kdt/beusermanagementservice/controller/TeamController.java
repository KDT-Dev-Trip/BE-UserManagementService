package ac.su.kdt.beusermanagementservice.controller;

import ac.su.kdt.beusermanagementservice.dto.CreateTeamRequestDTO;
import ac.su.kdt.beusermanagementservice.dto.TeamResponseDTO;
import ac.su.kdt.beusermanagementservice.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponseDTO> createTeam(
        @RequestBody @Valid CreateTeamRequestDTO request,
        // HTTP 요청의 헤더 값을 파라미터로 받옴
        // 실제 프로젝트에서는 Spring Security와 JWT를 통해 인증된 사용자 정보를 가져오지만,
        // 지금은 학습을 위해 'X-User-Id'라는 임의의 헤더로 사용자 ID를 받는다고 가정
        @RequestHeader("X-User-Id") Long userId
    ) {
        // 1. 서비스 계층에 비즈니스 로직 처리를 위임.
        TeamResponseDTO response = teamService.createTeam(request, userId);
        // 2. 성공적인 리소스 생성을 의미하는 201 Created 상태 코드와 함께, 생성된 팀 정보를 응답 본문에 담아 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}