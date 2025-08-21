package com.example.med.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    private String ExtractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies()).filter(cookie -> "jwt_token".equals(cookie.getName())).map(Cookie::getValue).findFirst().orElse(null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filter)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.debug("[JWT Filter] 요청 URI: {}", requestURI);

        if (request.getCookies() != null) {
            log.debug("[JWT Filter] 쿠키 개수: {}", request.getCookies().length);
            Arrays.stream(request.getCookies()).forEach(cookie ->
                    log.debug("[JWT Filter] 쿠키: {}={}", cookie.getName(), cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...")
            );
        } else {
            log.warn("[JWT Filter] 쿠키가 없습니다. URI: {}", requestURI);
        }
        String token = ExtractToken(request);
        log.debug("[JWT Filter] 추출된 토큰: {}", token != null ? "존재" : "없음");

        if (token != null) {

            if (!jwtTokenProvider.ValidateToken(token)) {
                log.warn("[JWT Filter] 만료된 않은 JWT 토큰. URI: {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다.");
                return;
            } else {
                log.debug("[JWT Filter] 유효한 JWT 토큰. URI: {}", request.getRequestURI());

                String userId = jwtTokenProvider.getClaim(token, "userId");
                String userName = jwtTokenProvider.getClaim(token, "userName");

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, null);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("[JWT Filter] 인증 정보 설정 완료: 사용자 ID = {}, 사용자 이름 = {}", userId, userName);
            }
            filter.doFilter(request, response);
        }
    }
}