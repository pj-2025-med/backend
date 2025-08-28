package com.example.med.service;

import com.example.med.dto.logDto.CommentDeleteLogDto;
import com.example.med.dto.logDto.CommentUpdateLogDto;
import com.example.med.dto.StudyCommentDto;
import com.example.med.dto.logDto.LogShowDto;
import com.example.med.mapper.DicomMapper;
import com.example.med.mapper.StudyCommentMapper;
import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudyCommentService {

    private final DicomMapper dicomMapper; // 메인 DB 매퍼
    private final StudyCommentMapper studyCommentMapper; // 서브 DB 매퍼
    private final StringEncryptor stringEncryptor; // Jasypt 암호/복호화

    @Transactional(readOnly = true)
    public List<StudyCommentDto> getCommentsByStudyKey(long studyKey) {
        List<StudyCommentDto> comments = studyCommentMapper.findCommentsByStudyKey(studyKey);
        for (StudyCommentDto comment : comments) {
            // userId 복호화
            String encryptedUserId = comment.getUserId();
            if (encryptedUserId != null && !encryptedUserId.isEmpty()) {
                try {
                    String decryptedUserId = stringEncryptor.decrypt(encryptedUserId);
                    comment.setUserId(decryptedUserId);
                } catch (Exception e) {
                    // 복호화 실패 시 원본 유지
                }
            }
            // commentTitle 복호화
            String encryptedTitle = comment.getCommentTitle();
            if (encryptedTitle != null && !encryptedTitle.isEmpty()) {
                try {
                    String decryptedTitle = stringEncryptor.decrypt(encryptedTitle);
                    comment.setCommentTitle(decryptedTitle);
                } catch (Exception e) {
                    // 복호화 실패 시 원본 유지
                }
            }
            // commentContent 복호화
            String encryptedContent = comment.getCommentContent();
            if (encryptedContent != null && !encryptedContent.isEmpty()) {
                try {
                    String decryptedContent = stringEncryptor.decrypt(encryptedContent);
                    comment.setCommentContent(decryptedContent);
                } catch (Exception e) {
                    // 복호화 실패 시 원본 유지
                }
            }
        }
        // CommentId, createdAt, updatedAt 필드는 복호화하지 않음 -> String 타입만 암호화 가능하기에 변환을 해야하고, 굳이 이건
        return comments;
    }

    @Transactional
    public StudyCommentDto createComment(StudyCommentDto comment) {
        // 1. studyKey 유효성 검증
        List<Long> seriesKeys = dicomMapper.findSeriesKeys(comment.getStudyKey());
        if (seriesKeys == null || seriesKeys.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 studyKey입니다: " + comment.getStudyKey());
        }

        // userId, commentTitle, commentContent 암호화 후 DB 저장
        if (comment.getUserId() != null && !comment.getUserId().isEmpty()) {
            try {
                String encryptedUserId = stringEncryptor.encrypt(comment.getUserId());
                comment.setUserId(encryptedUserId);
            } catch (Exception e) {
                // 암호화 실패 시 원본 유지
            }
        }

        if (comment.getCommentTitle() != null && !comment.getCommentTitle().isEmpty()) {
            try {
                String encryptedTitle = stringEncryptor.encrypt(comment.getCommentTitle());
                comment.setCommentTitle(encryptedTitle);
            } catch (Exception e) {
                // 암호화 실패 시 원본 유지
            }
        }

        if (comment.getCommentContent() != null && !comment.getCommentContent().isEmpty()) {
            try {
                String encryptedContent = stringEncryptor.encrypt(comment.getCommentContent());
                comment.setCommentContent(encryptedContent);
            } catch (Exception e) {
                // 암호화 실패 시 원본 유지
            }
        }

        // 2. 댓글 생성 시간 설정
        comment.setCreatedAt(LocalDateTime.now());

        // 3. 댓글 저장 (Mapper의 RETURNING 절에 의해 comment.commentId가 채워짐)
        studyCommentMapper.insertComment(comment);

        // 4. 생성 로그 기록
        studyCommentMapper.insertLog(comment);

        // 5. ID와 생성 시간이 포함된 완전한 DTO 반환
        return comment;
    }

    @Transactional
    public StudyCommentDto updateComment(long commentId, String currentUserId, StudyCommentDto studyCommentDto, CommentUpdateLogDto commentUpdateLogDto) {
        // 1. 수정할 댓글 조회
        StudyCommentDto existingComment = studyCommentMapper.findCommentById(commentId);
        if (existingComment == null) {
            throw new IllegalStateException("코멘트를 찾을 수 없습니다: " + commentId);
        }

        // 기존 댓글의 userId를 복호화해서 권한 체크
        String decryptedUserId = existingComment.getUserId();
        if (decryptedUserId != null && !decryptedUserId.isEmpty()) {
            try {
                decryptedUserId = stringEncryptor.decrypt(decryptedUserId);
            } catch (Exception e) {
                // 복호화 실패 시 원본 사용
            }
        }

        if (!Objects.equals(decryptedUserId, currentUserId)) {
            throw new IllegalStateException("코멘트를 수정할 권한이 없습니다.");
        }

        // 수정할 데이터 암호화
        if (studyCommentDto.getUserId() != null && !studyCommentDto.getUserId().isEmpty()) {
            try {
                String encryptedUserId = stringEncryptor.encrypt(studyCommentDto.getUserId());
                studyCommentDto.setUserId(encryptedUserId);
            } catch (Exception e) {
                // 암호화 실패 시 원본 유지
            }
        }

        if (studyCommentDto.getCommentTitle() != null && !studyCommentDto.getCommentTitle().isEmpty()) {
            try {
                String encryptedTitle = stringEncryptor.encrypt(studyCommentDto.getCommentTitle());
                studyCommentDto.setCommentTitle(encryptedTitle);
            } catch (Exception e) {
                // 암호화 실패 시 원본 유지
            }
        }

        if (studyCommentDto.getCommentContent() != null && !studyCommentDto.getCommentContent().isEmpty()) {
            try {
                String encryptedContent = stringEncryptor.encrypt(studyCommentDto.getCommentContent());
                studyCommentDto.setCommentContent(encryptedContent);
            } catch (Exception e) {
                // 암호화 실패 시 원본 유지
            }
        }
        // 2. 수정 권한 확인
        if (!Objects.equals(existingComment.getUserId(), currentUserId)) {
            throw new IllegalStateException("코멘트를 수정할 권한이 없습니다.");
        }

        // 3. 로그의 신뢰성을 위해 DB에서 조회한 원본(수정 전) 데이터를 DTO에 설정
        commentUpdateLogDto.setOriginalTitle(existingComment.getCommentTitle());
        commentUpdateLogDto.setOriginalContent(existingComment.getCommentContent());

        // 4. 댓글 업데이트
        studyCommentDto.setCommentId(commentId);
        studyCommentMapper.updateComment(studyCommentDto);

        // 5. 수정 로그 기록
        studyCommentMapper.updateLog(commentUpdateLogDto);

        // 6. 업데이트된 최신 댓글 정보 반환
        return studyCommentMapper.findCommentById(commentId);
    }

    /**
     * 코멘트를 삭제합니다.
     * @param commentId 삭제할 코멘트의 ID
     * @param currentUserId 현재 로그인한 사용자의 ID
     */
    @Transactional
    public void deleteComment(long commentId, String currentUserId, CommentDeleteLogDto commentDeleteLogDto) {
        // 1. 삭제할 코멘트가 DB에 존재하는지 확인
        StudyCommentDto existingComment = studyCommentMapper.findCommentById(commentId);
        if (existingComment == null) {
            throw new IllegalStateException("코멘트를 찾을 수 없습니다: " + commentId);
        }

        // 2. 기존 댓글의 userId를 복호화해서 권한 체크
        String decryptedUserId = existingComment.getUserId();
        if (decryptedUserId != null && !decryptedUserId.isEmpty()) {
            try {
                decryptedUserId = stringEncryptor.decrypt(decryptedUserId);
            } catch (Exception e) {
                // 복호화 실패 시 원본 사용
            }
        }

        if (!Objects.equals(decryptedUserId, currentUserId)) {
            throw new IllegalStateException("코멘트를 삭제할 권한이 없습니다.");
        }

        // 3. 로그 기록을 위해 삭제 전 원본 데이터를 DTO에 설정
        commentDeleteLogDto.setOriginalTitle(existingComment.getCommentTitle());
        commentDeleteLogDto.setOriginalContent(existingComment.getCommentContent());

        // 4. DB에서 코멘트 삭제
        studyCommentMapper.deleteComment(commentId);

        // 5. 삭제 로그 기록
        studyCommentMapper.deleteLog(commentDeleteLogDto);
    }

    @Transactional(readOnly = true)
    public List<LogShowDto> getAllLogs() {

        // DB에서 가져온 암호화된 로그 데이터를 복호화합니다.
        return studyCommentMapper.showAllLogs();
    }
}
