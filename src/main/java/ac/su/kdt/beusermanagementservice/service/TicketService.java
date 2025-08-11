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

import org.springframework.scheduling.annotation.Scheduled;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private final UserRepository userRepository;
    private final TicketTransactionRepository ticketTransactionRepository;
    private static final int MAX_TICKET_COUNT = 10; // 일단 10개로 설정

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

    // 주기적으로 티켓 충전
    @Scheduled(fixedRateString = "43200000") // 12 시간
    @Transactional
    public void scheduledTicketRefill() {
        logger.info("티켓 자동 충전 시작");
        // 현재는 모든 사용자를 대상으로 충전 여부 확인
        List<User> allUsers = userRepository.findAll();

        // 모든 사용자를 순회하며 개별 충전 로직 호출
        for (User user : allUsers) {
            refillTicketForUser(user);
        }
        // 스케줄러가 작업을 마쳤음을 로그로 알림.
        logger.info("티켓 자동 충전 스케줄러 종료");
    }

    // 특정 사용자의 티켓을 충전하는 내부 로직
    @Transactional
    public void refillTicketForUser(User user) {
        // 해당 사용자의 가장 마지막 'REFILL' 거래 내역을 조회
        Optional<TicketTransaction> lastRefillOpt = ticketTransactionRepository.findTopByUserAndTransactionTypeOrderByCreatedAtDesc(user, TicketTransaction.TransactionType.REFILL);

        // 마지막 충전으로부터 12시간이 지났는지 여부 판단
        boolean shouldRefill = lastRefillOpt.map(lastRefill -> // 마지막 충전 기록이 있다면, 그 기록의 생성 시간이 12시간 전보다 이전인지(before) 확인
                lastRefill.getCreatedAt().before(Timestamp.from(Instant.now().minus(12, ChronoUnit.HOURS)))
        ).orElse(true); // 마지막 충전 기록이 없다면(orElse), 무조건 충전 대상

        // 충전 대상일 경우에만 실행
        if (shouldRefill) {
            // 현재 잔액 조회
            int balanceBefore = getTicketBalance(user.getId());
            // 최대 보유량(10개) 미만일 경우에만 충전
            if (balanceBefore < MAX_TICKET_COUNT) {
                // 충전 후 잔액 계산
                int balanceAfter = balanceBefore + 1;
                // 'REFILL' 타입의 거래 내역 생성
                TicketTransaction transaction = new TicketTransaction(user, TicketTransaction.TransactionType.REFILL, 1, balanceBefore, balanceAfter, "자동 충전");
                // 생성된 거래 내역을 DB에 저장
                ticketTransactionRepository.save(transaction);
                // 충전 완료 로그 기록
                logger.info("티켓 충전: userId={}, 잔액: {} -> {}", user.getId(), balanceBefore, balanceAfter);
            }
        }
    }
}