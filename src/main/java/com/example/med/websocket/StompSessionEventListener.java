package com.example.med.websocket;

import com.example.med.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket STOMP 세션 이벤트를 감지하고 처리하는 리스너
 * - 사용자 연결/해제 시 온라인 상태 관리
 * - PresenceService를 통해 상태 변경 및 알림 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompSessionEventListener {

    private final PresenceService presenceService;

    /**
     * WebSocket 연결 완료 이벤트 처리
     * - 사용자가 WebSocket에 성공적으로 연결되었을 때 호출
     * - 세션에서 사용자 정보를 추출하여 온라인 상태로 설정
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.debug("[Session Event] WebSocket 연결 이벤트 감지 - 세션 ID: {}", sessionId);

        try {
            // 세션 속성에서 사용자 정보 추출 (JWT 인증 시 저장된 정보)
            String userId = (String) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            String userRole = (String) headerAccessor.getSessionAttributes().get("userRole");

            if (userId != null && userName != null) {
                // 사용자를 온라인 상태로 설정
                presenceService.setUserOnline(userId, userName, sessionId);
                log.info("[Session Event] 사용자 연결 처리 완료 - ID: {}, 이름: {}, 역할: {}, 세션: {}",
                        userId, userName, userRole, sessionId);
            } else {
                log.warn("[Session Event] 세션에서 사용자 정보를 찾을 수 없음 - 세션 ID: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("[Session Event] 연결 이벤트 처리 중 오류 발생 - 세션 ID: {}, 오류: {}",
                    sessionId, e.getMessage(), e);
        }
    }

    /**
     * WebSocket 연결 해제 이벤트 처리
     * - 사용자가 WebSocket 연결을 종료했을 때 호출
     * - 세션에서 사용자 정보를 추출하여 오프라인 상태로 설정
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.debug("[Session Event] WebSocket 연결 해제 이벤트 감지 - 세션 ID: {}", sessionId);

        try {
            // 세션 속성에서 사용자 정보 추출
            String userId = (String) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            String userRole = (String) headerAccessor.getSessionAttributes().get("userRole");

            if (userId != null && userName != null) {
                // 사용자를 오프라인 상태로 설정
                presenceService.setUserOffline(userId, userName, sessionId);
                log.info("[Session Event] 사용자 연결 해제 처리 완료 - ID: {}, 이름: {}, 역할: {}, 세션: {}",
                        userId, userName, userRole, sessionId);
            } else {
                log.warn("[Session Event] 세션에서 사용자 정보를 찾을 수 없음 - 세션 ID: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("[Session Event] 연결 해제 이벤트 처리 중 오류 발생 - 세션 ID: {}, 오류: {}",
                    sessionId, e.getMessage(), e);
        }
    }
}
