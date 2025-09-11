package com.example.med.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 온라인/오프라인 상태 알림 DTO
 * - 사용자의 상태 변경을 모든 구독자에게 알릴 때 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusNotification {

    /**
     * 상태가 변경된 사용자 ID
     */
    private String userId;

    /**
     * 사용자 이름
     */
    private String userName;

    /**
     * 사용자 상태 (ONLINE, OFFLINE)
     */
    private String status;

    /**
     * 상태 변경 시간
     */
    private LocalDateTime timestamp;

    /**
     * 추가 메타데이터 (필요시 사용)
     */
    private String metadata;
}
