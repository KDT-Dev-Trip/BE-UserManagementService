package ac.su.kdt.beusermanagementservice.controller;

import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.repository.TeamRepository;
import ac.su.kdt.beusermanagementservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가짜 HTTP 요청을 보내기 위한 객체

    @Autowired
    private ObjectMapper objectMapper; // Java 객체를 JSON 문자열로 변환하기 위한 객체

    @Autowired
    private UserRepository userRepository; // 테스트 데이터를 미리 만들기 위한 리포지토리

    @Autowired
    private TeamRepository teamRepository; // 테스트 데이터 확인을 위한 리포지토리

    private User testUser; // 여러 테스트에서 공통으로 사용할 사용자 객체

    @BeforeEach
    void setUp() {
        // 테스트를 실행하기 전에 항상 깨끗한 상태를 만들기 위해 DB를 초기화
        teamRepository.deleteAll();
        userRepository.deleteAll();
        // 모든 테스트에서 사용할 기본 사용자를 미리 생성하고 DB에 저장
        testUser = userRepository.save(new User("auth|testuser", "test@test.com", "테스트유저"));
    }

    @Test
    @DisplayName("팀 생성 API 성공 테스트")
    void createTeam_success() throws Exception {
        // given
        // 팀 생성을 요청할 때 Body에 담을 데이터를 Map으로 생성
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "KDT팀");
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // when & then
        mockMvc.perform(post("/api/teams") // POST 메서드로 /api/teams 경로에 요청
                .header("X-User-Id", String.valueOf(testUser.getId())) // 인증을 대신할 사용자 ID 헤더 추가
                .contentType(MediaType.APPLICATION_JSON) // 요청 본문의 타입이 JSON임을 명시
                .content(jsonBody)) // 요청 본문에 JSON 데이터 추가
            // .andExpect(): 요청에 대한 응답을 검증
            .andExpect(status().isCreated()) // 응답 상태 코드가 201 Created 인지 확인
            // jsonPath("$.필드명"): 응답 JSON의 특정 필드 값 확인. '$'는 JSON 전체를 의미
            .andExpect(jsonPath("$.name").value("나의 멋진 팀")) // 응답의 name 필드가 "KDT팀"인지 확인
            .andExpect(jsonPath("$.instructorId").value(testUser.getId())) // 응답의 instructorId가 테스트 유저의 ID인지 확인
            .andExpect(jsonPath("$.teamCode").exists()); // 응답에 teamCode 필드가 존재하는지 확인
    }
}