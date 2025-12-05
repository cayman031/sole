package com.sole.domain.user.entity;

import com.sole.domain.region.entity.Region;
import com.sole.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_user_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "IDX_user_region", columnList = "region_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region; // null 허용

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_level", length = 20)
    private PreferredLevel preferredLevel; // null 허용

    @Builder
    private User(String email, String password, String nickname, Region
            region, PreferredLevel preferredLevel) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.region = region;
        this.preferredLevel = preferredLevel;
    }

    public void changeProfile(String nickname, Region region, PreferredLevel
            preferredLevel) {
        this.nickname = nickname;
        this.region = region;
        this.preferredLevel = preferredLevel;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}