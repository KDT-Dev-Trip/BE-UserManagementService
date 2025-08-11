package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.TicketTransaction;
import ac.su.kdt.beusermanagementservice.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TicketTransactionRepositoryTest {
    @Autowired private UserRepository userRepository;
    @Autowired private TicketTransactionRepository ticketTransactionRepository;

    @Test
    @DisplayName("티켓 거래 내역 저장 및 사용자 ID로 조회 테스트")
    void saveAndFindByUserId() {
        // given
        User user = userRepository.save(new User("auth|ticket", "ticket@test.com", "티켓사용자"));
        TicketTransaction refill = new TicketTransaction(user, TicketTransaction.TransactionType.REFILL, 5, 0, 5, "자동 충전");
        TicketTransaction consume = new TicketTransaction(user, TicketTransaction.TransactionType.CONSUME, -1, 5, 4, "미션 시작");

        // when
        ticketTransactionRepository.save(refill);
        ticketTransactionRepository.save(consume);

        // then
        // 특정 사용자의 모든 티켓 거래량의 합을 계산하는 테스트
        Integer currentBalance = ticketTransactionRepository.findCurrentTicketBalanceByUserId(user.getId()).orElse(0);
        assertThat(currentBalance).isEqualTo(4);
    }
}