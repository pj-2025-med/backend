package com.example.med.websocket;

import com.example.med.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * STOMP WebSocket 연결 시 JWT 토큰 인증을 처리하는 인터셉터
 * - STOMP CONNECT 명령 시 Authorization 헤더에서 JWT 토큰을 추출하여 검증
 * - 유효한 토큰인 경우 사용자 정보를 STOMP 세션에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 메시지 전송 전 JWT 토큰 검증 처리
     * - STOMP CONNECT 명령에 대해서만 JWT 토큰 검증 수행
     * - Authorization 헤더에서 Bearer 토큰 추출
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("[STOMP Auth] CONNECT 명령 감지, JWT 토큰 검증 시작");

            // Authorization 헤더에서 JWT 토큰 추출
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7); // "Bearer " 제거

                try {
                    // JWT 토큰 유효성 검증
                    if (jwtTokenProvider.ValidateToken(token)) {
                        // 토큰에서 사용자 정보 추출
                        String userId = jwtTokenProvider.getClaim(token, "sub");
                        String userName = jwtTokenProvider.getClaim(token, "name");
                        String userRole = jwtTokenProvider.getClaim(token, "role");

                        log.info("[STOMP Auth] JWT 토큰 검증 성공 - 사용자 ID: {}, 이름: {}, 역할: {}",
                                userId, userName, userRole);

                        // 권한 설정
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        if (userRole != null && !userRole.isEmpty()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + userRole));
                        }

                        // 인증 객체 생성 및 세션에 저장
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        accessor.setUser(authentication);

                        // 사용자 정보를 세션 속성에 저장 (이후 사용을 위해)
                        if (accessor.getSessionAttributes() != null) {
                            accessor.getSessionAttributes().put("userId", userId);
                            accessor.getSessionAttributes().put("userName", userName);
                            accessor.getSessionAttributes().put("userRole", userRole);
                        }

                    } else {
                        log.warn("[STOMP Auth] 유효하지 않은 JWT 토큰");
                        throw new SecurityException("Invalid JWT token");
                    }
                } catch (Exception e) {
                    log.error("[STOMP Auth] JWT 토큰 검증 실패: {}", e.getMessage());
                    throw new SecurityException("JWT token validation failed", e);
                }
            } else {
                log.warn("[STOMP Auth] Authorization 헤더가 없거나 형식이 잘못됨");
                throw new SecurityException("Missing or invalid Authorization header");
            }
        }

        return message;
    }
}
