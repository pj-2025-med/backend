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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.time.*;

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.debug("[JWT Filter] 요청 URI: {}", requestURI);

        // 스웨거 및 공개 경로는 토큰 검증 없이 통과
        if (isPublicPath(requestURI)) {
            log.debug("[JWT Filter] 공개 경로로 인증 없이 통과: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

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
                log.warn("[JWT Filter] 유효하지 않은 JWT 토큰. URI: {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다.");
                return;
            } else {
                log.debug("[JWT Filter] 유효한 JWT 토큰. URI: {}", request.getRequestURI());

                String userId = jwtTokenProvider.getClaim(token, "sub");
                String userName = jwtTokenProvider.getClaim(token, "name");
                String userRole = jwtTokenProvider.getClaim(token, "role");

                java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                if (userRole != null && !userRole.isEmpty()) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + userRole));
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                 LocalDateTime now = LocalDateTime.now();
                 String formatedNow = now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초"));

                log.debug("[JWT Filter] 인증 정보 설정 완료: 사용자 ID = {}, 사용자 이름 = {}, 역할 = {}", userId, userName, userRole);
                String userLog = String.format("[LOG] 접속 시간 = %s, 사용자 ID = %s, 사용자 이름 = %s, 역할 = %s", formatedNow, userId, userName, userRole);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String requestURI) {
        return requestURI.startsWith("/swagger-ui/") ||
               requestURI.equals("/swagger-ui.html") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.startsWith("/swagger-resources/") ||
               requestURI.startsWith("/webjars/") ||
               requestURI.equals("/api/register") ||
               requestURI.equals("/api/login") ||
               requestURI.equals("/api/logout") ||
               requestURI.equals("/api/db-ping");
    }
}