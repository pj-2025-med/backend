package com.example.med.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 요청 DTO
 * - 클라이언트에서 서버로 채팅 메시지를 전송할 때 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    /**
     * 메시지 발신자 ID (의사의 고유 ID)
     */
    private String senderId;

    /**
     * 메시지 수신자 ID (의사의 고유 ID)
     */
    private String recipientId;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * 메시지 전송 시간
     * 클라이언트에서 설정하거나 서버에서 자동 설정
     */
    private LocalDateTime timestamp;

    /**
     * 메시지 타입 (텍스트, 이미지, 파일 등)
     * 기본값: TEXT
     */
    @Builder.Default
    private String messageType = "TEXT";
}
