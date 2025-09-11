package com.example.med.controller;

import com.example.med.dto.ChatMessageRequest;
import com.example.med.dto.ChatUserInfo;
import com.example.med.service.ChatService;
import com.example.med.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WebSocket 세션 관리를 위한 REST API 컨트롤러
 * - WebSocket 연결을 위한 개인화된 토픽 주소 제공
 * - 온라인 사용자 목록 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class WebSocketSessionController {

    private final PresenceService presenceService;
    private final ChatService chatService;

    /**
     * WebSocket 연결을 위한 개인화된 STOMP 토픽 주소 생성
     * - 클라이언트가 구독할 개인 채팅 큐 주소를 반환
     *
     * @param userId 인증된 사용자 ID (JWT에서 추출)
     * @return 개인화된 토픽 주소 정보
     */
    @PostMapping("/connect-websocket")
    public ResponseEntity<Map<String, Object>> connectWebSocket(@AuthenticationPrincipal String userId) {

        log.info("[WebSocket Session] WebSocket 연결 요청 - 사용자 ID: {}", userId);

        try {
            // 개인화된 채팅 큐 주소 생성
            String personalChatTopic = "/sub/queue/chat/" + userId;
            String presenceTopic = "/sub/presence";

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("personalChatTopic", personalChatTopic);
            response.put("presenceTopic", presenceTopic);
            response.put("websocketEndpoint", "/ws");
            response.put("success", true);
            response.put("message", "WebSocket 연결 정보가 성공적으로 생성되었습니다.");

            log.info("[WebSocket Session] WebSocket 연결 정보 생성 완료 - 사용자: {}, 개인 토픽: {}",
                    userId, personalChatTopic);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[WebSocket Session] WebSocket 연결 정보 생성 실패 - 사용자: {}, 오류: {}",
                    userId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "WebSocket 연결 정보 생성에 실패했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 현재 온라인 상태인 사용자 목록 조회
     *
     * @param userId 인증된 사용자 ID
     * @return 온라인 사용자 목록
     */
    @GetMapping("/online-users")
    public ResponseEntity<Map<String, Object>> getOnlineUsers(@AuthenticationPrincipal String userId) {

        log.debug("[WebSocket Session] 온라인 사용자 목록 조회 요청 - 요청자: {}", userId);

        try {
            Set<String> onlineUsers = presenceService.getOnlineUsers();
            int onlineUserCount = presenceService.getOnlineUserCount();

            Map<String, Object> response = new HashMap<>();
            response.put("onlineUsers", onlineUsers);
            response.put("onlineUserCount", onlineUserCount);
            response.put("requesterId", userId);
            response.put("success", true);

            log.info("[WebSocket Session] 온라인 사용자 목록 조회 완료 - 요청자: {}, 온라인 사용자 수: {}",
                    userId, onlineUserCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[WebSocket Session] 온라인 사용자 목록 조회 실패 - 요청자: {}, 오류: {}",
                    userId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "온라인 사용자 목록 조회에 실패했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 특정 사용자의 온라인 상태 확인
     *
     * @param targetUserId 확인할 대상 사용자 ID
     * @param userId 인증된 요청자 ID
     * @return 대상 사용자의 온라인 상태
     */
    @GetMapping("/user-status/{targetUserId}")
    public ResponseEntity<Map<String, Object>> getUserStatus(
            @PathVariable String targetUserId,
            @AuthenticationPrincipal String userId) {

        log.debug("[WebSocket Session] 사용자 상태 조회 요청 - 요청자: {}, 대상: {}", userId, targetUserId);

        try {
            boolean isOnline = presenceService.isUserOnline(targetUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("targetUserId", targetUserId);
            response.put("isOnline", isOnline);
            response.put("status", isOnline ? "ONLINE" : "OFFLINE");
            response.put("requesterId", userId);
            response.put("success", true);

            log.debug("[WebSocket Session] 사용자 상태 조회 완료 - 대상: {}, 상태: {}",
                    targetUserId, isOnline ? "ONLINE" : "OFFLINE");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[WebSocket Session] 사용자 상태 조회 실패 - 요청자: {}, 대상: {}, 오류: {}",
                    userId, targetUserId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사용자 상태 조회에 실패했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 채팅 가능한 모든 사용자 목록 조회 (온라인 상태 포함)
     * - 회원정보 DB의 모든 사용자를 조회
     * - 각 사용자의 온라인/오프라인 상태를 함께 제공
     *
     * @param userId 인증된 사용자 ID
     * @return 채팅 가능한 사용자 목록
     */
    @GetMapping("/chat-users")
    public ResponseEntity<Map<String, Object>> getChatAvailableUsers(@AuthenticationPrincipal String userId) {

        log.debug("[WebSocket Session] 채팅 가능한 사용자 목록 조회 요청 - 요청자: {}", userId);

        try {
            List<ChatUserInfo> chatUsers = presenceService.getChatAvailableUsers(userId);

            // 온라인/오프라인별로 분류
            List<ChatUserInfo> onlineUsers = chatUsers.stream()
                    .filter(ChatUserInfo::isOnline)
                    .collect(Collectors.toList());

            List<ChatUserInfo> offlineUsers = chatUsers.stream()
                    .filter(user -> !user.isOnline())
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("allUsers", chatUsers);
            response.put("onlineUsers", onlineUsers);
            response.put("offlineUsers", offlineUsers);
            response.put("totalCount", chatUsers.size());
            response.put("onlineCount", onlineUsers.size());
            response.put("offlineCount", offlineUsers.size());
            response.put("requesterId", userId);
            response.put("success", true);

            log.info("[WebSocket Session] 채팅 가능한 사용자 목록 조회 완료 - 요청자: {}, 전체: {}, 온라인: {}, 오프라인: {}",
                    userId, chatUsers.size(), onlineUsers.size(), offlineUsers.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[WebSocket Session] 채팅 가능한 사용자 목록 조회 실패 - 요청자: {}, 오류: {}",
                    userId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "채팅 가능한 사용자 목록 조회에 실패했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 특정 사용자의 상세 정보 조회 (온라인 상태 포함)
     *
     * @param targetUserId 조회할 대상 사용자 ID
     * @param userId 인증된 요청자 ID
     * @return 대상 사용자의 상세 정보
     */
    @GetMapping("/user-info/{targetUserId}")
    public ResponseEntity<Map<String, Object>> getChatUserInfo(
            @PathVariable String targetUserId,
            @AuthenticationPrincipal String userId) {

        log.debug("[WebSocket Session] 사용자 상세 정보 조회 요청 - 요청자: {}, 대상: {}", userId, targetUserId);

        try {
            ChatUserInfo userInfo = presenceService.getChatUserInfo(targetUserId);

            if (userInfo == null) {
                Map<String, Object> notFoundResponse = new HashMap<>();
                notFoundResponse.put("success", false);
                notFoundResponse.put("message", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("userInfo", userInfo);
            response.put("requesterId", userId);
            response.put("success", true);

            log.debug("[WebSocket Session] 사용자 상세 정보 조회 완료 - 대상: {}, 상태: {}",
                    targetUserId, userInfo.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[WebSocket Session] 사용자 상세 정보 조회 실패 - 요청자: {}, 대상: {}, 오류: {}",
                    userId, targetUserId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사용자 정보 조회에 실패했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 특정 사용자와의 1:1 채팅 시작
     * - 채팅방 정보 생성 및 반환
     * - 채팅 히스토리 포함 여부 선택 가능
     *
     * @param targetUserId 채팅 대상 사용자 ID
     * @param includeHistory 채팅 히스토리 포함 여부 (기본값: true)
     * @param historyLimit 히스토리 조회 개수 (기본값: 50)
     * @param userId 인증된 사용자 ID
     * @return 채팅방 정보 및 히스토리
     */
    @PostMapping("/start-chat/{targetUserId}")
    public ResponseEntity<Map<String, Object>> startChatWithUser(
            @PathVariable String targetUserId,
            @RequestParam(defaultValue = "true") boolean includeHistory,
            @RequestParam(defaultValue = "50") int historyLimit,
            @AuthenticationPrincipal String userId) {

        log.info("[WebSocket Session] 1:1 채팅 시작 요청 - 요청자: {}, 대상: {}, 히스토리 포함: {}",
                userId, targetUserId, includeHistory);

        try {
            // 대상 사용자 정보 조회
            ChatUserInfo targetUserInfo = presenceService.getChatUserInfo(targetUserId);
            if (targetUserInfo == null) {
                Map<String, Object> notFoundResponse = new HashMap<>();
                notFoundResponse.put("success", false);
                notFoundResponse.put("message", "대상 사용자를 찾을 수 없습니다.");
                return ResponseEntity.notFound().build();
            }

            // 채팅방 정보 생성
            String chatRoomId = generateChatRoomId(userId, targetUserId);
            String personalTopic = "/sub/queue/chat/" + userId;
            String targetTopic = "/sub/queue/chat/" + targetUserId;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chatRoomId", chatRoomId);
            response.put("currentUserId", userId);
            response.put("targetUser", targetUserInfo);
            response.put("personalTopic", personalTopic);
            response.put("targetTopic", targetTopic);

            // 채팅 히스토리 포함 여부에 따라 추가 정보 제공
            if (includeHistory) {
                try {
                    // ChatHistoryController와 동일한 로직으로 히스토리 조회
                    List<ChatMessageRequest> chatHistory = chatService.getChatHistory(userId, targetUserId, historyLimit);
                    response.put("chatHistory", chatHistory);
                    response.put("historyCount", chatHistory.size());
                    log.info("[WebSocket Session] 채팅 히스토리 포함 - 메시지 수: {}", chatHistory.size());
                } catch (Exception e) {
                    log.warn("[WebSocket Session] 채팅 히스토리 조회 실패, 히스토리 없이 진행 - 오류: {}", e.getMessage());
                    response.put("chatHistory", Collections.emptyList());
                    response.put("historyCount", 0);
                }
            }

            response.put("timestamp", LocalDateTime.now());
            response.put("message", "채팅방이 성공적으로 생성되었습니다.");

            log.info("[WebSocket Session] 1:1 채팅 시작 완료 - 채팅방 ID: {}, 대상: {} ({})",
                    chatRoomId, targetUserInfo.getUserName(), targetUserInfo.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[WebSocket Session] 1:1 채팅 시작 실패 - 요청자: {}, 대상: {}, 오류: {}",
                    userId, targetUserId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "채팅방 생성에 실패했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 채팅방 ID 생성 (두 사용자 ID를 기반으로 고유한 채팅방 ID 생성)
     * - 사용자 ID를 알파벳 순으로 정렬하여 일관된 채팅방 ID 생성
     *
     * @param userId1 사용자 1 ID
     * @param userId2 사용자 2 ID
     * @return 고유한 채팅방 ID
     */
    private String generateChatRoomId(String userId1, String userId2) {
        // 두 사용자 ID를 알파벳 순으로 정렬하여 일관된 채팅방 ID 생성
        String[] sortedIds = {userId1, userId2};
        Arrays.sort(sortedIds);
        return "chat_" + sortedIds[0] + "_" + sortedIds[1];
    }
}
