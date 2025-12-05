package com.sole.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void bcrypt로_암호화하면_원문과_다르지만_매칭된다() {
        String raw = "P@ssw0rd123";
        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).isNotEqualTo(raw); // 해시가 평문과 달라야 한다
        assertThat(passwordEncoder.matches(raw, encoded)).isTrue(); // 원문 매칭
        assertThat(passwordEncoder.matches("wrong", encoded)).isFalse(); // 오입력 매칭 실패
    }
}
