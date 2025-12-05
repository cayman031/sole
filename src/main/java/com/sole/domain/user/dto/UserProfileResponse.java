package com.sole.domain.user.dto;

import com.sole.domain.region.entity.Region;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String email,
        String nickname,
        PreferredLevel preferredLevel,
        RegionResponse region,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPreferredLevel(),
                RegionResponse.from(user.getRegion()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public record RegionResponse(Long id, String city, String district) {
        public static RegionResponse from(Region region) {
            if (region == null) {
                return null;
            }
            return new RegionResponse(region.getId(), region.getCity(),
                    region.getDistrict());
        }
    }
}