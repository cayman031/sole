package com.sole.domain.crew.controller;

import com.sole.domain.crew.dto.*;
import com.sole.domain.crew.service.CrewService;
import com.sole.domain.user.service.UserPrincipal;
import com.sole.global.common.ApiResponse;
import com.sole.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;

    @PostMapping
    public ApiResponse<Long> createCrew(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CrewCreateRequest request
    ) {
        Long crewId = crewService.createCrew(principal.getId(), request);
        return ApiResponse.success(crewId);
    }

    @GetMapping("/{crewId}")
    public ApiResponse<CrewDetailResponse> getCrewDetail(@PathVariable Long crewId) {
        CrewDetailResponse response = crewService.getCrewDetail(crewId);
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<PageResponse<CrewSummaryResponse>> getCrews(
            @ModelAttribute CrewSearchCondition condition,
            @PageableDefault(size = 10, sort = "meetingTime") Pageable pageable
    ) {
        Page<CrewSummaryResponse> page = crewService.getCrews(condition, pageable);
        return ApiResponse.success(PageResponse.from(page));
    }

    @PutMapping("/{crewId}")
    public ApiResponse<Long> updateCrew(
            @PathVariable Long crewId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CrewUpdateRequest request
    ) {
        crewService.updateCrew(crewId, principal.getId(), request);
        return ApiResponse.success(crewId);
    }

    @DeleteMapping("/{crewId}")
    public ApiResponse<Void> deleteCrew(
            @PathVariable Long crewId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        crewService.deleteCrew(crewId, principal.getId());
        return ApiResponse.success();
    }

    @PostMapping("/{crewId}/join")
    public ApiResponse<Void> joinCrew(
            @PathVariable Long crewId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        crewService.joinCrew(crewId, principal.getId());
        return ApiResponse.success();
    }

    @PostMapping("/{crewId}/leave")
    public ApiResponse<Void> leaveCrew(
            @PathVariable Long crewId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        crewService.leaveCrew(crewId, principal.getId());
        return ApiResponse.success();
    }
}