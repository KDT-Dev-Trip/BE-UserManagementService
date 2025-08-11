package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.entity.TicketTransaction;
import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.repository.TicketTransactionRepository;
import ac.su.kdt.beusermanagementservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private final UserRepository userRepository;
    private final TicketTransactionRepository ticketTransactionRepository;

    // 티켓 잔액을 조회
    @Transactional(readOnly = true)
    public int getTicketBalance(Long userId) {
        return ticketTransactionRepository.findCurrentTicketBalanceByUserId(userId).orElse(0);
    }

    // 티켓 사용
    @Transactional
    public void consumeTicket(Long userId, String reason) {
        // 1. 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 현재 티켓 잔액 확인
        int balanceBefore = getTicketBalance(userId);
        // 3. 티켓이 0개 이하이면 예외 발생
        if (balanceBefore <= 0) {
            throw new IllegalStateException("티켓이 부족하여 미션을 시작할 수 없습니다.");
        }

        // 4. 티켓 사용 후의 잔액을 계산
        int balanceAfter = balanceBefore - 1;
        // 5. 'CONSUME' 타입의 티켓 거래 내역(Transaction) 객체 생성
        TicketTransaction transaction = new TicketTransaction(user, TicketTransaction.TransactionType.CONSUME, -1, balanceBefore, balanceAfter, reason);
        // 6. 생성된 거래 내역 DB에 저장
        ticketTransactionRepository.save(transaction);
        // 7. 로그 기록
        logger.info("티켓 사용: userId={}, 사유={}, 잔액: {} -> {}", userId, reason, balanceBefore, balanceAfter);
    }
}