package com.sole.domain.crew.dto;

import com.sole.domain.crew.entity.CrewMember;
import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.user.entity.PreferredLevel;
import java.time.LocalDateTime;
import java.util.List;

public record CrewDetailResponse(
        Long id,
        String title,
        String description,
        Long hostId,
        String hostNickname,
        Long regionId,
        String regionCity,
        String regionDistrict,
        LocalDateTime meetingTime,
        String place,
        double latitude,
        double longitude,
        int maxParticipants,
        PreferredLevel level,
        List<MemberDto> members
) {
    public static CrewDetailResponse from(RunningCrew crew) {
        return new CrewDetailResponse(
                crew.getId(),
                crew.getTitle(),
                crew.getDescription(),
                crew.getHost().getId(),
                crew.getHost().getNickname(),
                crew.getRegion().getId(),
                crew.getRegion().getCity(),
                crew.getRegion().getDistrict(),
                crew.getMeetingTime(),
                crew.getPlace(),
                crew.getLatitude(),
                crew.getLongitude(),
                crew.getMaxParticipants(),
                crew.getLevel(),
                crew.getMembers().stream()
                        .map(MemberDto::from)
                        .toList()
        );
    }

    public record MemberDto(
            Long userId,
            String nickname,
            String role
    ) {
        public static MemberDto from(CrewMember member) {
            return new MemberDto(
                    member.getUser().getId(),
                    member.getUser().getNickname(),
                    member.getRole().name()
            );
        }
    }
}