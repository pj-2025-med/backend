package com.example.med.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 사용자 정보 DTO
 * - 채팅 가능한 사용자 목록 표시용
 * - 온라인/오프라인 상태 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserInfo {

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 사용자 이름
     */
    private String userName;

    /**
     * 이메일
     */
    private String email;

    /**
     * 온라인 상태 (true: 온라인, false: 오프라인)
     */
    private boolean isOnline;

    /**
     * 상태 문자열 ("ONLINE" 또는 "OFFLINE")
     */
    private String status;

    /**
     * 마지막 접속 시간 (온라인인 경우 null)
     */
    private String lastSeen;
}
