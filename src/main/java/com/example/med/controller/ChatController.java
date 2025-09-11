package com.example.med.controller;

import com.example.med.dto.ChatMessageRequest;
import com.example.med.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket 채팅 메시지 처리 컨트롤러
 * - 클라이언트로부터 채팅 메시지를 수신하고 처리
 * - STOMP 프로토콜을 사용한 실시간 메시지 처리
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 채팅 메시지 수신 및 처리
     * - 클라이언트가 /pub/chat/message로 전송한 메시지를 처리
     * - 메시지를 데이터베이스에 저장하고 수신자에게 전달
     *
     * @param chatMessage 채팅 메시지 요청 객체
     * @param headerAccessor STOMP 헤더 정보 (세션 정보 포함)
     */
    @MessageMapping("/chat/message")
    public void handleChatMessage(@Payload ChatMessageRequest chatMessage,
                                 SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        log.debug("[Chat Controller] 채팅 메시지 수신 - 세션: {}", sessionId);

        try {
            // 세션에서 발신자 정보 추출 (JWT 인증 시 저장된 정보)
            Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes == null) {
                throw new SecurityException("세션 정보를 찾을 수 없습니다.");
            }

            String authenticatedUserId = (String) sessionAttributes.get("userId");
            String userName = (String) sessionAttributes.get("userName");

            // 보안 검증: 인증된 사용자 ID와 메시지의 발신자 ID가 일치하는지 확인
            if (authenticatedUserId == null || !authenticatedUserId.equals(chatMessage.getSenderId())) {
                log.warn("[Chat Controller] 발신자 ID 불일치 - 인증된 ID: {}, 메시지 발신자 ID: {}",
                        authenticatedUserId, chatMessage.getSenderId());
                throw new SecurityException("발신자 ID가 인증된 사용자와 일치하지 않습니다.");
            }

            // 메시지 타임스탬프 설정 (클라이언트에서 설정하지 않은 경우)
            if (chatMessage.getTimestamp() == null) {
                chatMessage.setTimestamp(LocalDateTime.now());
            }

            log.info("[Chat Controller] 채팅 메시지 처리 시작 - 발신자: {} ({}), 수신자: {}, 내용 길이: {} 문자",
                    userName, authenticatedUserId, chatMessage.getRecipientId(),
                    chatMessage.getContent() != null ? chatMessage.getContent().length() : 0);

            // ChatService를 통해 메시지 처리 (저장 및 전송)
            chatService.processMessage(chatMessage);

            log.debug("[Chat Controller] 채팅 메시지 처리 완료 - 세션: {}", sessionId);

        } catch (SecurityException e) {
            log.error("[Chat Controller] 보안 오류 - 세션: {}, 오류: {}", sessionId, e.getMessage());
            // 보안 오류는 클라이언트에게 상세 정보를 전달하지 않음
            throw e;
        } catch (Exception e) {
            log.error("[Chat Controller] 메시지 처리 중 오류 발생 - 세션: {}, 오류: {}",
                    sessionId, e.getMessage(), e);
            throw new RuntimeException("메시지 처리 중 오류가 발생했습니다.", e);
        }
    }
}
