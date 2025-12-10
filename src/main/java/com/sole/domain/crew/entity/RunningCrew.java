package com.sole.domain.crew.entity;

import com.sole.domain.region.entity.Region;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.global.common.BaseTimeEntity;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Entity
@Table(
        name = "running_crews",
        indexes = {
                @Index(name = "IDX_running_crews_region_meeting", columnList
                        = "region_id, meeting_time"),
                @Index(name = "IDX_running_crews_lat_lng", columnList =
                        "latitude, longitude")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningCrew extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "meeting_time", nullable = false)
    private LocalDateTime meetingTime;

    @Column(nullable = false, length = 255)
    private String place;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PreferredLevel level;

    @OneToMany(mappedBy = "crew", cascade = CascadeType.ALL, orphanRemoval
            = true)
    private final List<CrewMember> members = new ArrayList<>();

    @Builder
    private RunningCrew(String title, String description, User host, Region
                                region,
                        LocalDateTime meetingTime, String place,
                        double latitude, double longitude,
                        int maxParticipants, PreferredLevel level) {
        this.title = title;
        this.description = description;
        this.host = host;
        this.region = region;
        this.meetingTime = meetingTime;
        this.place = place;
        this.latitude = latitude;
        this.longitude = longitude;
        this.maxParticipants = maxParticipants;
        this.level = level;
    }

    // 편의 메서드: 양방향 연관관계 세팅
    public void addMember(CrewMember member) {
        members.add(member);
        member.assignCrew(this);
    }

    // 모임 정보 수정 (호스트 권한 체크 후 호출)
    public void update(
            String title,
            String description,
            Region region,
            LocalDateTime meetingTime,
            String place,
            double latitude,
            double longitude,
            int maxParticipants,
            PreferredLevel level
    ) {
        this.title = title;
        this.description = description;
        this.region = region;
        this.meetingTime = meetingTime;
        this.place = place;
        this.latitude = latitude;
        this.longitude = longitude;
        this.maxParticipants = maxParticipants;
        this.level = level;
    }

    // 현재 참여 인원 수로 정원 초과 여부 확인
    public void validateCapacity(int currentCount) {
        if (currentCount >= maxParticipants) {
            throw new BusinessException(ErrorCode.CREW_MEMBER_LIMIT_EXCEEDED);
        }
    }
}