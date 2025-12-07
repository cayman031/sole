package com.sole.domain.crew.controller;

import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.dto.CrewDetailResponse;
import com.sole.domain.crew.service.CrewService;
import com.sole.domain.user.service.UserPrincipal;
import com.sole.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}