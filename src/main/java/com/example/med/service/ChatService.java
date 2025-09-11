package com.example.med.service;

import com.example.med.dto.ChatMessageRequest;
import com.example.med.mapper.ChatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 메시지 처리 서비스
 * - 메시지를 데이터베이스에 저장
 * - 수신자에게 실시간으로 메시지 전송
 * - 메시지 전송 히스토리 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;

    /**
     * 채팅 메시지 처리 (저장 및 전송)
     * - 데이터베이스에 메시지 저장
     * - 수신자가 온라인인 경우 실시간 전송
     *
     * @param chatMessage 처리할 채팅 메시지
     */
    @Transactional
    public void processMessage(ChatMessageRequest chatMessage) {
        try {
            log.debug("[Chat Service] 메시지 처리 시작 - 발신자: {}, 수신자: {}",
                    chatMessage.getSenderId(), chatMessage.getRecipientId());

            // 메시지 ID 생성 (UUID 사용)
            String messageId = UUID.randomUUID().toString();

            // 메시지 데이터베이스 저장
            saveChatMessage(messageId, chatMessage);

            // 수신자에게 실시간 메시지 전송
            sendMessageToRecipient(chatMessage);

            log.info("[Chat Service] 메시지 처리 완료 - ID: {}, 발신자: {}, 수신자: {}",
                    messageId, chatMessage.getSenderId(), chatMessage.getRecipientId());

        } catch (Exception e) {
            log.error("[Chat Service] 메시지 처리 중 오류 발생 - 발신자: {}, 수신자: {}, 오류: {}",
                    chatMessage.getSenderId(), chatMessage.getRecipientId(), e.getMessage(), e);
            throw new RuntimeException("메시지 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 채팅 메시지를 데이터베이스에 저장
     *
     * @param messageId 메시지 고유 ID
     * @param chatMessage 저장할 채팅 메시지
     */
    private void saveChatMessage(String messageId, ChatMessageRequest chatMessage) {
        try {
            // 메시지 저장을 위한 파라미터 설정
            ChatMessageRequest messageToSave = ChatMessageRequest.builder()
                    .senderId(chatMessage.getSenderId())
                    .recipientId(chatMessage.getRecipientId())
                    .content(chatMessage.getContent())
                    .messageType(chatMessage.getMessageType())
                    .timestamp(chatMessage.getTimestamp() != null ? chatMessage.getTimestamp() : LocalDateTime.now())
                    .build();

            // MyBatis 매퍼를 통해 데이터베이스에 저장
            int result = chatMapper.insertChatMessage(messageId, messageToSave);

            if (result <= 0) {
                throw new RuntimeException("메시지 저장에 실패했습니다.");
            }

            log.debug("[Chat Service] 메시지 데이터베이스 저장 완료 - ID: {}", messageId);

        } catch (Exception e) {
            log.error("[Chat Service] 메시지 저장 중 오류 발생 - ID: {}, 오류: {}", messageId, e.getMessage(), e);
            throw new RuntimeException("메시지 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 수신자에게 실시간으로 메시지 전송
     *
     * @param chatMessage 전송할 채팅 메시지
     */
    private void sendMessageToRecipient(ChatMessageRequest chatMessage) {
        try {
            String recipientId = chatMessage.getRecipientId();

            // 수신자 온라인 상태 확인
            boolean isRecipientOnline = presenceService.isUserOnline(recipientId);

            log.debug("[Chat Service] 수신자 온라인 상태 - ID: {}, 온라인: {}", recipientId, isRecipientOnline);

            // 수신자의 개인 큐로 메시지 전송 (온라인 여부와 관계없이 전송)
            // 오프라인 사용자도 나중에 연결했을 때 받을 수 있도록 큐에 전송
            String destination = "/sub/queue/chat/" + recipientId;
            messagingTemplate.convertAndSend(destination, chatMessage);

            log.info("[Chat Service] 메시지 실시간 전송 완료 - 수신자: {}, 목적지: {}, 온라인: {}",
                    recipientId, destination, isRecipientOnline);

        } catch (Exception e) {
            log.error("[Chat Service] 실시간 메시지 전송 중 오류 발생 - 수신자: {}, 오류: {}",
                    chatMessage.getRecipientId(), e.getMessage(), e);
            // 실시간 전송 실패는 전체 처리를 중단시키지 않음 (메시지는 이미 저장됨)
        }
    }

    /**
     * 특정 사용자 간의 채팅 히스토리 조회
     *
     * @param userId1 사용자 1 ID
     * @param userId2 사용자 2 ID
     * @param limit 조회할 메시지 수 제한
     * @return 채팅 메시지 목록
     */
    @Transactional(readOnly = true)
    public java.util.List<ChatMessageRequest> getChatHistory(String userId1, String userId2, int limit) {
        try {
            log.debug("[Chat Service] 채팅 히스토리 조회 - 사용자1: {}, 사용자2: {}, 제한: {}",
                    userId1, userId2, limit);

            java.util.List<ChatMessageRequest> messages = chatMapper.selectChatHistory(userId1, userId2, limit);

            log.info("[Chat Service] 채팅 히스토리 조회 완료 - 메시지 수: {}", messages.size());

            return messages;

        } catch (Exception e) {
            log.error("[Chat Service] 채팅 히스토리 조회 중 오류 발생 - 사용자1: {}, 사용자2: {}, 오류: {}",
                    userId1, userId2, e.getMessage(), e);
            throw new RuntimeException("채팅 히스토리 조회 중 오류가 발생했습니다.", e);
        }
    }
}
