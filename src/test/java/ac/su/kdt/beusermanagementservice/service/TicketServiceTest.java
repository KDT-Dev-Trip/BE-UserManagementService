package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.entity.SubscriptionPlan;
import ac.su.kdt.beusermanagementservice.repository.TicketTransactionRepository;
import ac.su.kdt.beusermanagementservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @InjectMocks
    private TicketService ticketService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketTransactionRepository ticketTransactionRepository;

    @Test
    @DisplayName("티켓 사용 성공 테스트")
    void consumeTicket_success() {
        // given
        final Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(new User("auth|1", "test@test.com", "user", SubscriptionPlan.FREE)));
        given(ticketTransactionRepository.findCurrentTicketBalanceByUserId(userId)).willReturn(Optional.of(3));
        // when
        ticketService.consumeTicket(userId, "미션 시작");
        // then
        then(ticketTransactionRepository).should().save(any());
    }

    @Test
    @DisplayName("티켓 부족으로 사용 실패 테스트")
    void consumeTicket_fail_insufficientTickets() {
        // given
        final Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(new User("auth|1", "test@test.com", "user", SubscriptionPlan.FREE)));
        given(ticketTransactionRepository.findCurrentTicketBalanceByUserId(userId)).willReturn(Optional.of(0));
        // when & then
        assertThrows(IllegalStateException.class, () -> ticketService.consumeTicket(userId, "미션 시작"));
        then(ticketTransactionRepository).should(never()).save(any());
    }
}