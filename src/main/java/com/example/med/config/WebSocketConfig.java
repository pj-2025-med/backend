package com.example.med.config;

import com.example.med.websocket.StompJwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 설정 클래스
 * - WebSocket 연결 엔드포인트 설정
 * - STOMP 메시지 브로커 설정
 * - JWT 인증 인터셉터 등록
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtAuthInterceptor stompJwtAuthInterceptor;

    /**
     * STOMP 메시지 브로커 설정
     * - /sub: 클라이언트가 구독할 브로커 경로
     * - /pub: 클라이언트가 메시지를 발행할 애플리케이션 경로
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 브로커 경로 설정
        // 예: /sub/queue/chat/{userId}, /sub/presence
        config.enableSimpleBroker("/sub");

        // 클라이언트가 메시지를 발행할 애플리케이션 데스티네이션 프리픽스
        // 예: /pub/chat/message
        config.setApplicationDestinationPrefixes("/pub");

        log.info("[WebSocket] 메시지 브로커 설정 완료 - 구독: /sub, 발행: /pub");
    }

    /**
     * STOMP 엔드포인트 등록
     * - WebSocket 연결을 위한 엔드포인트 설정
     * - SockJS 폴백 지원
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 등록
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 설정 (운영환경에서는 특정 도메인으로 제한)
                .withSockJS(); // SockJS 폴백 지원

        log.info("[WebSocket] STOMP 엔드포인트 등록 완료 - /ws");
    }

    /**
     * 클라이언트 인바운드 채널 설정
     * - JWT 인증 인터셉터 등록
     * - STOMP 헤더에서 JWT 토큰을 검증
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT 인증 인터셉터 등록
        registration.interceptors(stompJwtAuthInterceptor);
        log.info("[WebSocket] JWT 인증 인터셉터 등록 완료");
    }
}
