package com.sole.domain.auth.service;

import com.sole.domain.auth.dto.LoginRequest;
import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.domain.user.service.UserPrincipal;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public Long signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        Region region = null;
        if (request.regionId() != null) {
            region = regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(request.nickname())
                .region(region)
                .preferredLevel(request.preferredLevel())
                .build();

        Long id = userRepository.save(user).getId();
        log.info("user signed up id={} email={}", id, request.email());
        return id;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

        Authentication authentication = authenticationManager.authenticate(authToken);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return new LoginResult(authentication, principal.getId(),principal.getUsername(),principal.getNickname());
    }

    @Transactional(readOnly = true)
    public void logout() {}

    public record LoginResult(
            Authentication authentication,
            Long userId,
            String email,
            String nickname
    ) {}
}
