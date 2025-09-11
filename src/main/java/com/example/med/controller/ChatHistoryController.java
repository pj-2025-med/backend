package com.example.med.controller;

import com.example.med.dto.ChatMessageRequest;
import com.example.med.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 채팅 히스토리 조회를 위한 REST API 컨트롤러
 * - 채팅 메시지 히스토리 조회
 * - 읽지 않은 메시지 수 확인
 * - 최근 대화 상대 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatService chatService;

    /**
     * 특정 사용자와의 채팅 히스토리 조회
     *
     * @param partnerId 대화 상대방 ID
     * @param limit 조회할 메시지 수 (기본값: 50)
     * @param userId 인증된 사용자 ID
     * @return 채팅 메시지 히스토리
     */
    @GetMapping("/history/{partnerId}")
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @PathVariable String partnerId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal String userId) {

        log.info("[Chat History] 채팅 히스토리 조회 요청 - 요청자: {}, 상대방: {}, 제한: {}",
                userId, partnerId, limit);

        try {
            // 입력값 검증
            if (limit <= 0 || limit > 200) {
                limit = 50; // 기본값으로 설정
            }

            List<ChatMessageRequest> chatHistory = chatService.getChatHistory(userId, partnerId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("messages", chatHistory);
            response.put("messageCount", chatHistory.size());
            response.put("partnerId", partnerId);
            response.put("userId", userId);
            response.put("success", true);

            log.info("[Chat History] 채팅 히스토리 조회 완료 - 메시지 수: {}", chatHistory.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Chat History] 채팅 히스토리 조회 실패 - 요청자: {}, 상대방: {}, 오류: {}",
                    userId, partnerId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "채팅 히스토리 조회에 실패했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
