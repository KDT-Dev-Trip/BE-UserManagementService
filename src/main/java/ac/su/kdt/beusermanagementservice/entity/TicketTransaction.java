package ac.su.kdt.beusermanagementservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;

@Entity
@Table(name = "ticket_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "ticket_amount", nullable = false)
    private Integer ticketAmount;

    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    // 거래 사유 (예: '미션 시작', '자동 충전')
    private String reason;

    // 엔티티 생성 시 시간을 자동으로 기록
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    public TicketTransaction(User user, TransactionType transactionType, Integer ticketAmount, Integer balanceBefore, Integer balanceAfter, String reason) {
        this.user = user;
        this.transactionType = transactionType;
        this.ticketAmount = ticketAmount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
    }

    public enum TransactionType {
        REFILL,     // 주기적 충전
        CONSUME,    // 미션 사용
        ADMIN_GRANT,// 관리자 지급
        INITIAL     // 최초 가입 시 지급
    }
}