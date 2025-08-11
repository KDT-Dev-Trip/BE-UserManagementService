package ac.su.kdt.beusermanagementservice.repository;

import ac.su.kdt.beusermanagementservice.entity.TicketTransaction;
import ac.su.kdt.beusermanagementservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface TicketTransactionRepository extends JpaRepository<TicketTransaction, Long> {
    // 특정 사용자의 거래 내역을 최신순으로 조회
    List<TicketTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    // 특정 사용자의 특정 타입 거래 중 가장 마지막 기록을 조회 (스케줄러에서 사용)
    Optional<TicketTransaction> findTopByUserAndTransactionTypeOrderByCreatedAtDesc(User user, TicketTransaction.TransactionType type);
    
    // 복잡한 쿼리를 직접 JPQL(Java Persistence Query Language)로 작성
    // 특정 사용자의 모든 ticketAmount의 합계를 계산하여 현재 티켓 잔액 조회
    @Query("SELECT SUM(tt.ticketAmount) FROM TicketTransaction tt WHERE tt.user.id = :userId")
    Optional<Integer> findCurrentTicketBalanceByUserId(Long userId);
}