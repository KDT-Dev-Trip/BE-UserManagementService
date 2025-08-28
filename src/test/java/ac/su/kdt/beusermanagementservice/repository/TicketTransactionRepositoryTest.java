package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.TicketTransaction;
import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.entity.SubscriptionPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TicketTransactionRepositoryTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TicketTransactionRepository ticketTransactionRepository; // 아직 없어서 에러 발생

    @Test
    @DisplayName("티켓 거래 내역 저장 및 사용자 ID로 조회 테스트")
    void saveAndFindByUserId() {
        // given
        User user = userRepository.save(new User("auth|ticket", "ticket@test.com", "티켓사용자", SubscriptionPlan.FREE));
        // 두 개의 거래 내역 객체를 생성. (TicketTransaction 클래스가 아직 없어 에러 발생)
        TicketTransaction refill = new TicketTransaction(user, TicketTransaction.TransactionType.REFILL, 5, 0, 5, "자동 충전");
        TicketTransaction consume = new TicketTransaction(user, TicketTransaction.TransactionType.CONSUME, -1, 5, 4, "미션 시작");

        // when
        ticketTransactionRepository.save(refill);
        ticketTransactionRepository.save(consume);

        // then
        Integer currentBalance = ticketTransactionRepository.findCurrentTicketBalanceByUserId(user.getId()).orElse(0);
        // 잔액이 4가 맞는지 확인
        assertThat(currentBalance).isEqualTo(4);
    }
}