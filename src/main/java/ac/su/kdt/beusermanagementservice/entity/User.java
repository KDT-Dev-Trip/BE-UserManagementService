package ac.su.kdt.beusermanagementservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Timestamp;

@Entity
@Table(name = "`user`") // SQL 예약어 'user'와의 충돌을 피하기 위해 백틱(`) 사용
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth0_id", unique = true, nullable = false)
    private String auth0Id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @Column(name = "last_login_at")
    private Timestamp lastLoginAt;

    public User(String auth0Id, String email, String name) {
        this.auth0Id = auth0Id;
        this.email = email;
        this.name = name;
        this.role = Role.STUDENT;
        this.status = Status.ACTIVE;
    }

    public enum Role {
        STUDENT, INSTRUCTOR, ADMIN
    }

    public enum Status {
        ACTIVE, INACTIVE, SUSPENDED
    }

    // 프로필 수정을 위한
    public void updateProfile(String name, String phone) {
        // 요청에 이름이 포함되어 있으면 이름을 변경
        if (name != null) {
            this.name = name;
        }
        // 요청에 전화번호가 포함되어 있으면 전화번호를 변경
        if (phone != null) {
            this.phone = phone;
        }
    }
}