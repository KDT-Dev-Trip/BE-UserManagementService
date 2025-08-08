package ac.su.kdt.beusermanagementservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Timestamp;

@Entity
@Table(name = "team") // 최종 ERD의 테이블명 'team'으로 수정
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "team_code", unique = true, nullable = false)
    private String teamCode;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 6;

    @Column(name = "current_members", nullable = false)
    private Integer currentMembers = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    public Team(String name, String teamCode, Long instructorId) {
        this.name = name;
        this.teamCode = teamCode;
        this.instructorId = instructorId;
    }

    public enum Status {
        ACTIVE, INACTIVE, COMPLETED
    }
}