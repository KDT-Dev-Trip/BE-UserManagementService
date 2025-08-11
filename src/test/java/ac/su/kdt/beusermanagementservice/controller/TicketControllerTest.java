package ac.su.kdt.beusermanagementservice.controller;

import ac.su.kdt.beusermanagementservice.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private TicketService ticketService;

    @Test
    @DisplayName("티켓 잔액 조회 API 테스트")
    void getTicketBalance() throws Exception {
        // given
        // 테스트에 사용할 사용자 ID
        final Long userId = 1L;
        // 1L 로 호출되면, 숫자 5를 반환하도록 설정
        given(ticketService.getTicketBalance(userId)).willReturn(5);

        // when & then
        // /api/users/1/tickets 경로로 GET 요청을 보냄
        mockMvc.perform(get("/api/users/" + userId + "/tickets"))
                // .andExpect(): 응답 결과를 검증
                .andExpect(status().isOk()) // HTTP 상태 코드가 200 OK인지 확인
                .andExpect(jsonPath("$.userId").value(userId)) // 응답 JSON의 userId 필드가 1인지 확인
                .andExpect(jsonPath("$.ticketBalance").value(5)); // 응답 JSON의 ticketBalance 필드가 5인지 확인
    }

    @Test
    @DisplayName("티켓 사용 API 테스트")
    void consumeTicket() throws Exception {
        // given
        final Long userId = 1L;
        final String requestBody = "{\"reason\":\"미션 시작\"}";

        // when & then
        mockMvc.perform(post("/api/users/" + userId + "/tickets/consume") //  /consume 경로로 POST 요청
                        .contentType(MediaType.APPLICATION_JSON) //  요청 본문이 JSON 타입임을 명시
                        .content(requestBody)) //  요청 본문 추가
                .andExpect(status().isOk()); //  성공 시 200 OK 응답을 기대

        // userId=1L, reason="미션 시작" 정확히 한 번 호출되었는지 확인
        then(ticketService).should().consumeTicket(userId, "미션 시작");
    }
}