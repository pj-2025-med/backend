package com.example.med.mapper;

import com.example.med.dto.ChatMessageRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 채팅 메시지 관련 데이터베이스 작업을 위한 MyBatis 매퍼 인터페이스
 * - 채팅 메시지 저장, 조회 등의 데이터베이스 작업 정의
 */
@Mapper
public interface ChatMapper {

    /**
     * 채팅 메시지를 데이터베이스에 저장
     *
     * @param messageId 메시지 고유 ID
     * @param chatMessage 저장할 채팅 메시지 정보
     * @return 저장된 레코드 수
     */
    int insertChatMessage(@Param("messageId") String messageId,
                         @Param("chatMessage") ChatMessageRequest chatMessage);

    /**
     * 두 사용자 간의 채팅 히스토리 조회
     *
     * @param userId1 사용자 1 ID
     * @param userId2 사용자 2 ID
     * @param limit 조회할 메시지 수 제한
     * @return 채팅 메시지 목록 (최신순)
     */
    List<ChatMessageRequest> selectChatHistory(@Param("userId1") String userId1,
                                              @Param("userId2") String userId2,
                                              @Param("limit") int limit);

    /**
     * 특정 사용자의 최근 대화 상대 목록 조회
     *
     * @param userId 사용자 ID
     * @param limit 조회할 대화 상대 수 제한
     * @return 최근 대화 상대 목록
     */
    List<String> selectRecentChatPartners(@Param("userId") String userId,
                                         @Param("limit") int limit);
}
