package com.sole.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/*
* HTTP 세션 + Security 저장/정리 전담 컴포넌트
* 서비스는 서블릿 API를 알 필요없이 이 컴포넌트를 컨트롤러에서 호출
* */

@Component
public class SessionManager {

    public void storeAuthentication(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
    }

    public void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
