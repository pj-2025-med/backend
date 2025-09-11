package com.example.med.service;

import com.example.med.dto.ChatUserInfo;
import com.example.med.dto.UserDto;
import com.example.med.dto.UserStatusNotification;
import com.example.med.mapper.second.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 사용자 온라인/오프라인 상태를 관리하는 서비스
 * - 메모리에서 현재 온라인 상태인 의사 목록 관리
 * - 상태 변경 시 모든 구독자에게 알림 전송
 * - 회원정보 DB와 연동하여 채팅 가능한 모든 사용자 목록 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserInfoMapper userInfoMapper;

    // 현재 온라인 상태인 사용자 ID를 저장하는 Thread-Safe Set
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    // 사용자 ID와 세션 ID 매핑 (한 사용자가 여러 세션을 가질 수 있음)
    private final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * 사용자 온라인 상태로 변경
     *
     * @param userId   사용자 ID
     * @param userName 사용자 이름
     * @param sessionId WebSocket 세션 ID
     */
    public void setUserOnline(String userId, String userName, String sessionId) {
        boolean wasOffline = !onlineUsers.contains(userId);

        // 사용자를 온라인 목록에 추가
        onlineUsers.add(userId);

        // 세션 매핑 추가
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        log.info("[Presence] 사용자 온라인 상태 변경 - ID: {}, 이름: {}, 세션: {}", userId, userName, sessionId);

        // 처음 온라인이 된 경우에만 알림 전송
        if (wasOffline) {
            UserStatusNotification notification = UserStatusNotification.builder()
                    .userId(userId)
                    .userName(userName)
                    .status("ONLINE")
                    .timestamp(LocalDateTime.now())
                    .build();

            // 모든 구독자에게 온라인 상태 알림 전송
            messagingTemplate.convertAndSend("/sub/presence", notification);
            log.info("[Presence] 온라인 상태 알림 전송 - 사용자: {} ({})", userName, userId);
        }
    }

    /**
     * 사용자 오프라인 상태로 변경
     *
     * @param userId   사용자 ID
     * @param userName 사용자 이름
     * @param sessionId WebSocket 세션 ID
     */
    public void setUserOffline(String userId, String userName, String sessionId) {
        // 해당 세션 제거
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);

            // 해당 사용자의 모든 세션이 종료된 경우에만 오프라인 처리
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                onlineUsers.remove(userId);

                log.info("[Presence] 사용자 오프라인 상태 변경 - ID: {}, 이름: {}", userId, userName);

                UserStatusNotification notification = UserStatusNotification.builder()
                        .userId(userId)
                        .userName(userName)
                        .status("OFFLINE")
                        .timestamp(LocalDateTime.now())
                        .build();

                // 모든 구독자에게 오프라인 상태 알림 전송
                messagingTemplate.convertAndSend("/sub/presence", notification);
                log.info("[Presence] 오프라인 상태 알림 전송 - 사용자: {} ({})", userName, userId);
            } else {
                log.debug("[Presence] 사용자 {}의 다른 세션이 남아있어 온라인 상태 유지", userId);
            }
        }
    }

    /**
     * 현재 온라인 상태인 사용자 목록 조회
     *
     * @return 온라인 사용자 ID Set
     */
    public Set<String> getOnlineUsers() {
        return Set.copyOf(onlineUsers);
    }

    /**
     * 특정 사용자의 온라인 상태 확인
     *
     * @param userId 사용자 ID
     * @return 온라인 여부
     */
    public boolean isUserOnline(String userId) {
        return onlineUsers.contains(userId);
    }

    /**
     * 현재 온라인 사용자 수 조회
     *
     * @return 온라인 사용자 수
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    /**
     * 채팅 가능한 모든 사용자 목록 조회 (온라인 상태 포함)
     * - 회원정보 DB의 모든 사용자를 조회
     * - 각 사용자의 온라인/오프라인 상태를 함께 제공
     *
     * @param currentUserId 현재 요청한 사용자 ID (자기 자신 제외용)
     * @return 채팅 가능한 사용자 목록
     */
    public List<ChatUserInfo> getChatAvailableUsers(String currentUserId) {
        try {
            log.debug("[Presence] 채팅 가능한 사용자 목록 조회 시작 - 요청자: {}", currentUserId);

            // 회원정보 DB에서 모든 사용자 조회
            List<UserDto> allUsers = userInfoMapper.findAll();

            // 현재 사용자 제외하고 ChatUserInfo로 변환
            List<ChatUserInfo> chatUsers = allUsers.stream()
                    .filter(user -> !user.getUserId().equals(currentUserId)) // 자기 자신 제외
                    .map(user -> {
                        boolean isOnline = onlineUsers.contains(user.getUserId());
                        return ChatUserInfo.builder()
                                .userId(user.getUserId())
                                .userName(user.getUserName())
                                .email(user.getEmail())
                                .isOnline(isOnline)
                                .status(isOnline ? "ONLINE" : "OFFLINE")
                                .lastSeen(isOnline ? null : "오프라인")
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("[Presence] 채팅 가능한 사용자 목록 조회 완료 - 전체: {}, 온라인: {}",
                    chatUsers.size(),
                    chatUsers.stream().mapToLong(u -> u.isOnline() ? 1 : 0).sum());

            return chatUsers;

        } catch (Exception e) {
            log.error("[Presence] 채팅 가능한 사용자 목록 조회 실패 - 요청자: {}, 오류: {}",
                    currentUserId, e.getMessage(), e);
            throw new RuntimeException("채팅 가능한 사용자 목록 조회에 실패했습니다.", e);
        }
    }

    /**
     * 특정 사용자의 상세 정보 조회 (온라인 상태 포함)
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 (온라인 상태 포함)
     */
    public ChatUserInfo getChatUserInfo(String userId) {
        try {
            log.debug("[Presence] 사용자 정보 조회 - 사용자 ID: {}", userId);

            // 회원정보 DB에서 사용자 정보 조회
            List<UserDto> users = userInfoMapper.findAll();
            UserDto user = users.stream()
                    .filter(u -> u.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (user == null) {
                log.warn("[Presence] 사용자를 찾을 수 없음 - 사용자 ID: {}", userId);
                return null;
            }

            boolean isOnline = onlineUsers.contains(userId);
            ChatUserInfo chatUserInfo = ChatUserInfo.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .isOnline(isOnline)
                    .status(isOnline ? "ONLINE" : "OFFLINE")
                    .lastSeen(isOnline ? null : "오프라인")
                    .build();

            log.debug("[Presence] 사용자 정보 조회 완료 - ID: {}, 상태: {}", userId, chatUserInfo.getStatus());

            return chatUserInfo;

        } catch (Exception e) {
            log.error("[Presence] 사용자 정보 조회 실패 - 사용자 ID: {}, 오류: {}",
                    userId, e.getMessage(), e);
            throw new RuntimeException("사용자 정보 조회에 실패했습니다.", e);
        }
    }
}
