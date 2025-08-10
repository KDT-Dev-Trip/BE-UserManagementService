package ac.su.kdt.beusermanagementservice.controller;

import ac.su.kdt.beusermanagementservice.entity.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가짜 HTTP 요청을 보내기 위한 객체
    @Autowired
    private ObjectMapper objectMapper; // Java 객체를 JSON 문자열로 변환하기 위한 객체
    @Autowired
    private UserRepository userRepository; // 테스트 데이터를 미리 만들기 위한 리포지토리

    private User testUser; // 여러 테스트에서 공통으로 사용할 사용자 객체

    // 각각의 @Test 메서드가 실행되기 직전에 매번 실행
    @BeforeEach

    void setUp() {
        userRepository.deleteAll();
        testUser = userRepository.save(new User("auth|testuser", "test@test.com", "테스트유저"));
    }

    @Test
    @DisplayName("사용자 프로필 조회 API 테스트")
    void getUserProfile_success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/users/" + testUser.getId() + "/profile") //GET 메서드로 /api/users/{userId}/profile 경로에 요청
                .header("X-User-Id", String.valueOf(testUser.getId()))) // 인증 헤더 추가
            .andExpect(status().isOk()) // 응답 상태 코드가 200 OK 인지 확인
            .andExpect(jsonPath("$.id").value(testUser.getId())) // 응답 JSON의 id 필드가 테스트 유저의 ID와 일치하는지 확인
            .andExpect(jsonPath("$.email").value("test@test.com")); // 응답 JSON의 email 필드가 일치하는지 확인
    }

    @Test
    @DisplayName("사용자 프로필 수정 API 테스트")
    void updateUserProfile_success() throws Exception {
        // given
        // #프로필 수정을 요청할 때 Body에 담을 데이터를 Map으로 생성
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "수정된이름");
        requestBody.put("phone", "010-1234-5678");
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // when & then
        mockMvc.perform(patch("/api/users/" + testUser.getId() + "/profile") // PATCH 메서드로 프로필 수정 요청
                .header("X-User-Id", String.valueOf(testUser.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
            .andExpect(status().isOk()) // 200 OK 응답 확인
            .andExpect(jsonPath("$.name").value("수정된이름")); // 응답의 이름이 변경되었는지 확인

        // DB에서 직접 확인하여 변경사항이 실제로 반영되었는지 추가 검증
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("수정된이름");
        assertThat(updatedUser.getPhone()).isEqualTo("010-1234-5678");
    }
    
    // ERD에 user 테이블에는 도장 관련 컬럼이 없으므로, UserSvc가 별도로 관리해야 함을 가정.
    // 지금은 별도의 테이블 없이 UserService 내에서 상태를 관리하는 로직으로 테스트
    @Test
    @DisplayName("여권 도장 기록 API 테스트")
    void addPassportStamp_success() throws Exception {
        // given
        // 도장 기록을 요청할 때 Body에 담을 데이터
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userId", testUser.getId());
        requestBody.put("missionId", 1L); // ## 임의의 미션 ID
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // when & then
        mockMvc.perform(post("/api/passport/stamps") // POST 메서드로 /api/passport/stamps 경로에 요청
                // MissionSvc와 같은 '내부 서비스'가 호출한다고 가정하므로 별도 인증 헤더는 생략 가능
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
            .andExpect(status().isOk()); // ## 성공 시 200 OK 응답 확인
    }
}