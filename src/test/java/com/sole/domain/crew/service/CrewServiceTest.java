package com.sole.domain.crew.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.entity.CrewMember;
import com.sole.domain.crew.entity.CrewRole;
import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.crew.repository.CrewMemberRepository;
import com.sole.domain.crew.repository.RunningCrewRepository;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrewServiceTest {

    @Mock
    private RunningCrewRepository runningCrewRepository;
    @Mock
    private CrewMemberRepository crewMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private CrewService crewService;

    @Test
    @DisplayName("이미 가입한 사용자는 다시 가입할 수 없다")
    void joinCrewDuplicate() {
        RunningCrew crew = sampleCrew();
        User user = sampleUser(2L);
        when(runningCrewRepository.findById(1L)).thenReturn(Optional.of(crew));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        CrewMember existing = CrewMember.builder()
                .user(user)
                .role(CrewRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
        crew.addMember(existing);
        when(crewMemberRepository.findByCrewAndUser(crew, user)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> crewService.joinCrew(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREW_MEMBER_ALREADY_JOINED);
    }

    @Test
    @DisplayName("정원이 가득 찬 모임에는 가입할 수 없다")
    void joinCrewOverCapacity() {
        RunningCrew crew = sampleCrew();
        User user = sampleUser(3L);
        when(runningCrewRepository.findById(1L)).thenReturn(Optional.of(crew));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(crewMemberRepository.findByCrewAndUser(crew, user)).thenReturn(Optional.empty());
        when(crewMemberRepository.countByCrew(crew)).thenReturn(crew.getMaxParticipants());

        assertThatThrownBy(() -> crewService.joinCrew(1L, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREW_MEMBER_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("호스트는 탈퇴할 수 없다")
    void hostCannotLeave() {
        RunningCrew crew = sampleCrew();
        User host = crew.getHost();
        CrewMember hostMember = CrewMember.builder()
                .user(host)
                .role(CrewRole.HOST)
                .joinedAt(LocalDateTime.now())
                .build();
        when(runningCrewRepository.findById(1L)).thenReturn(Optional.of(crew));
        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(crewMemberRepository.findByCrewAndUser(crew, host)).thenReturn(Optional.of(hostMember));

        assertThatThrownBy(() -> crewService.leaveCrew(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("모임 생성 시 호스트가 존재하지 않으면 예외가 발생한다")
    void createCrewMissingHost() {
        CrewCreateRequest request = new CrewCreateRequest(
                "제목",
                "소개",
                1L,
                LocalDateTime.now(),
                "장소",
                1.0,
                1.0,
                5,
                PreferredLevel.BEGINNER
        );
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crewService.createCrew(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private RunningCrew sampleCrew() {
        User host = sampleUser(1L);
        Region region = new Region("서울", "중구");
        RunningCrew crew = RunningCrew.builder()
                .title("함께 달리기")
                .description("설명")
                .host(host)
                .region(region)
                .meetingTime(LocalDateTime.now().plusDays(1))
                .place("시청")
                .latitude(37.5)
                .longitude(127.0)
                .maxParticipants(5)
                .level(PreferredLevel.INTERMEDIATE)
                .build();
        return crew;
    }

    private User sampleUser(Long id) {
        User user = User.builder()
                .email("user" + id + "@example.com")
                .password("pw")
                .nickname("사용자" + id)
                .region(new Region("서울", "강남구"))
                .preferredLevel(PreferredLevel.BEGINNER)
                .build();
        // id는 equals/hashCode보다 테스트 인자 전달에 필요
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
