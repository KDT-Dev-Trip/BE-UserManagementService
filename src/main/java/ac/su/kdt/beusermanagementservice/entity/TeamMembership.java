package ac.su.kdt.beusermanagementservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;

@Entity
@Table(name = "team_membership", uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.MEMBER;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private Timestamp joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    public TeamMembership(Team team, User user) {
        this.team = team;
        this.user = user;
    }

    public TeamMembership(Team team, User user, Role role) {
        this.team = team;
        this.user = user;
        this.role = role;
    }

    public enum Role {
        LEADER, MEMBER
    }

    public enum Status {
        ACTIVE, INACTIVE
    }
}