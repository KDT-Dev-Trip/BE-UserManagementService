package ac.su.kdt.beusermanagementservice.service;

import ac.su.kdt.beusermanagementservice.entity.SubscriptionPlan;
import ac.su.kdt.beusermanagementservice.entity.TicketTransaction;
import ac.su.kdt.beusermanagementservice.entity.User;
import ac.su.kdt.beusermanagementservice.repository.TicketTransactionRepository;
import ac.su.kdt.beusermanagementservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import org.springframework.scheduling.annotation.Scheduled;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private final UserRepository userRepository;
    private final UserService userService;
    private final TicketTransactionRepository ticketTransactionRepository;
    private static final int MAX_TICKET_COUNT = 10; // 최대 보유 티켓 수
    private static final int INITIAL_TICKET_COUNT = 7; // 최초 지급 티켓 수

    public TicketService(TicketTransactionRepository ticketTransactionRepository, UserRepository userRepository, @Lazy UserService userService) {
        this.ticketTransactionRepository = ticketTransactionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

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
    @Scheduled(fixedRateString = "120000") // 5분
    @Transactional
    public void scheduledTicketRefill() {
        logger.info("티켓 자동 충전 시작");

        AtomicInteger refillCount = new AtomicInteger(0);
        // 현재는 모든 사용자를 대상으로 충전 여부 확인
        List<User> allUsers = userRepository.findAll();

        // 모든 사용자를 순회하며 개별 충전 로직 호출
        for (User user : allUsers) {
            boolean isRefilled = refillTicketForUser(user);
            if (isRefilled) {
                // ## 충전에 성공한 경우에만 카운트를 1 증가시킵니다.
                refillCount.incrementAndGet();
            }
        }
        // 스케줄러가 작업을 마쳤음을 로그로 알림.
        logger.info("티켓 자동 충전 스케줄러 종료. 총 {}명의 사용자 중 {}명에게 티켓이 충전되었습니다.", allUsers.size(), refillCount.get());
    }

    // 특정 사용자의 티켓을 충전하는 내부 로직
    @Transactional
    public boolean refillTicketForUser(User user) {
        SubscriptionPlan effectivePlan = userService.getEffectivePlan(user); // 사용자의 플랜을 조회.
        long refillInterval = effectivePlan.getRefillIntervalHours(); // 플랜에 맞는 충전 주기 로드
        int maxTickets = effectivePlan.getMaxTickets(); // 플랜에 맞는 최대 보유량 로드

        // 해당 사용자의 가장 마지막 'REFILL' 거래 내역을 조회
        Optional<TicketTransaction> lastRefillOpt = ticketTransactionRepository.findTopByUserAndTransactionTypeOrderByCreatedAtDesc(user, TicketTransaction.TransactionType.REFILL);

        // 마지막 충전으로부터 2분 지났는지 여부 판단
        boolean shouldRefill = lastRefillOpt.map(lastRefill ->
                lastRefill.getCreatedAt().before(Timestamp.from(Instant.now().minus(refillInterval, ChronoUnit.HOURS)))
        ).orElse(true);

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
                return true;
            }
        }
        return false;
    }

    //최초 가입 시 사용자에게 티켓 지급
    @Transactional
    public void grantInitialTickets(User user) {
        SubscriptionPlan plan = user.getSubscriptionPlan(); // 사용자의 '기본' 구독 플랜을 가져옴
        int initialTickets = plan.getInitialTickets(); // 해당 플랜의 초기 지급 티켓 수를 가져옴

        TicketTransaction transaction = new TicketTransaction(user, TicketTransaction.TransactionType.INITIAL, initialTickets, 0, initialTickets, "신규 가입 환영 티켓 지급 (" + plan.name() + ")");
        ticketTransactionRepository.save(transaction);
        logger.info("신규가입 티켓 지급: userId={}, plan={}, 지급 개수={}, 잔액: 0 -> {}", user.getId(), plan.name(), initialTickets, initialTickets);
    }

    // ## [신규] 구독 플랜 변경 시 추가 티켓을 지급하는 메서드입니다.
    @Transactional
    public void upgradeSubscription(User user, SubscriptionPlan newPlan) {
        int bonusTickets = newPlan.getInitialTickets(); // 새로운 플랜의 초기 지급 티켓 수를 추가로 지급
        int balanceBefore = getTicketBalance(user.getId()); // 변경 직전의 현재 티켓 잔액 조회
        int balanceAfter = balanceBefore + bonusTickets; // 현재 잔액에 추가 티켓을 추가

        TicketTransaction transaction = new TicketTransaction(user, TicketTransaction.TransactionType.ADMIN_GRANT, bonusTickets, balanceBefore, balanceAfter, "구독 플랜 변경 보너스 지급 (" + newPlan.name() + ")");
        ticketTransactionRepository.save(transaction);
        logger.info("구독 플랜 변경 티켓 지급: userId={}, newPlan={}, 지급 개수={}, 잔액: {} -> {}", user.getId(), newPlan.name(), bonusTickets, balanceBefore, balanceAfter);
    }
}