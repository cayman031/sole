package com.sole.domain.user.service;

import com.sole.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// 도메인 User를 Spring Security UserDetails로 감싸는 어댑터
public class UserPrincipal implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    private UserPrincipal(User user) {
        this.user = user;
        // 현재는 단일 역할만 가정. 향후 Role 컬럼/테이블이 생기면 여기서 매핑.
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user);
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // 로그인 식별자로 이메일 사용
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 정책 추가 시 변경
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 잠금 정책 추가 시 변경
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 정책 추가 시 변경
    }

    @Override
    public boolean isEnabled() {
        return true; // 활성/비활성 플래그 추가 시 변경
    }
}
