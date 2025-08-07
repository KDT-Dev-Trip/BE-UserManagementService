package ac.su.kdt.beusermanagementservice.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name="users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 파라미터가 없는 생성자를 생성
public class User {

    @Id
    @Column(name="id", nullable = false, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name="email", nullable = false, unique = true)
    private String email;

    @Column(name="password_hash", nullable = false)
    private String passwordHash;

    @Column(name="name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable = false)
    private UserRole role;

    @Column(name="current_plan_id", columnDefinition = "VARCHAR(36)")
    private String currentPlanId;

    @CreationTimestamp
    @Column(name="created_at", nullable = false, updatable=false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name="updated_at", nullable = false)
    private Timestamp updatedAt;

    @Column(name="is_active", nullable = false)
    private Boolean isActive = true;

    public enum UserRole {
        ADMIN, USER, TEAM_ADMIN
    }

    public static User createUser(String email, String passwordHash, String name) {
        User user = new User();
        user.id = UUID.randomUUID().toString();
        user.email = email;
        user.passwordHash = passwordHash;
        user.name = name;
        user.role = UserRole.USER;
        user.isActive = true;
        return user;
    }
}
