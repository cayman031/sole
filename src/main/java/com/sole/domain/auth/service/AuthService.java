package com.sole.domain.auth.service;

import com.sole.domain.auth.dto.LoginRequest;
import com.sole.domain.auth.dto.LoginResponse;
import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.domain.user.service.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public Long signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        Region region = null;
        if (request.regionId() != null) {
            region = regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역입니다."));
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(request.nickname())
                .region(region)
                .preferredLevel(request.preferredLevel())
                .build();

        return userRepository.save(user).getId();
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest
            httpRequest) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.email(),
                        request.password());

        // 실패 시 BadCredentialsException 등이 던져짐
        Authentication authentication =
                authenticationManager.authenticate(authToken);

        // 인증 성공 시 세션 생성/갱신
        httpRequest.getSession(true);

        UserPrincipal principal = (UserPrincipal)
                authentication.getPrincipal();
        return new LoginResponse(principal.getId(), principal.getUsername(),
                principal.getUsername());
    }
}