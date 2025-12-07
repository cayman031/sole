package com.sole.domain.crew.entity;

import com.sole.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Entity
@Table(
        name = "crew_members",
        uniqueConstraints = @UniqueConstraint(name = "UK_crew_member",
                columnNames = {"crew_id", "user_id"}),
        indexes = @Index(name = "IDX_member_user", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private RunningCrew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrewRole role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Builder
    private CrewMember(User user, CrewRole role, LocalDateTime joinedAt) {
        this.user = user;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    // RunningCrew에서 addMember 호출 시 crew를 물려받는다.
    void assignCrew(RunningCrew crew) {
        this.crew = crew;
    }
}