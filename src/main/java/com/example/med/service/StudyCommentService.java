package com.example.med.service;

import com.example.med.dto.logDto.CommentDeleteLogDto;
import com.example.med.dto.logDto.CommentUpdateLogDto;
import com.example.med.dto.StudyCommentDto;
import com.example.med.dto.logDto.LogShowDto;
import com.example.med.mapper.DicomMapper;
import com.example.med.mapper.StudyCommentMapper;
import org.jasypt.encryption.StringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
// [수정] @RequiredArgsConstructor 제거
public class StudyCommentService {

    // [추가] 로거(Logger) 선언
    private static final Logger log = LoggerFactory.getLogger(StudyCommentService.class);

    private final DicomMapper dicomMapper; // 메인 DB 매퍼
    private final StudyCommentMapper studyCommentMapper; // 서브 DB 매퍼
    private final StringEncryptor stringEncryptor; // Jasypt 암호/복호화

    // [수정] 생성자를 직접 작성하여 @Qualifier로 의존성 주입을 명시
    public StudyCommentService(DicomMapper dicomMapper,
                               StudyCommentMapper studyCommentMapper,
                               @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {
        this.dicomMapper = dicomMapper;
        this.studyCommentMapper = studyCommentMapper;
        this.stringEncryptor = stringEncryptor;
    }


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
                    // [수정] 복호화 실패 시 로그 추가
                    log.warn("UserID 복호화 실패 (Comment ID: {}): {}", comment.getCommentId(), e.getMessage());
                }
            }
            // commentTitle 복호화
            String encryptedTitle = comment.getCommentTitle();
            if (encryptedTitle != null && !encryptedTitle.isEmpty()) {
                try {
                    String decryptedTitle = stringEncryptor.decrypt(encryptedTitle);
                    comment.setCommentTitle(decryptedTitle);
                } catch (Exception e) {
                    // [수정] 복호화 실패 시 로그 추가
                    log.warn("CommentTitle 복호화 실패 (Comment ID: {}): {}", comment.getCommentId(), e.getMessage());
                }
            }
            // commentContent 복호화
            String encryptedContent = comment.getCommentContent();
            if (encryptedContent != null && !encryptedContent.isEmpty()) {
                try {
                    String decryptedContent = stringEncryptor.decrypt(encryptedContent);
                    comment.setCommentContent(decryptedContent);
                } catch (Exception e) {
                    // [수정] 복호화 실패 시 로그 추가
                    log.warn("CommentContent 복호화 실패 (Comment ID: {}): {}", comment.getCommentId(), e.getMessage());
                }
            }
        }
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
                // [수정] 암호화 실패 시 로그 추가
                log.error("UserID 암호화 실패 (UserId: {}): {}", comment.getUserId(), e.getMessage());
            }
        }

        if (comment.getCommentTitle() != null && !comment.getCommentTitle().isEmpty()) {
            try {
                String encryptedTitle = stringEncryptor.encrypt(comment.getCommentTitle());
                comment.setCommentTitle(encryptedTitle);
            } catch (Exception e) {
                // [수정] 암호화 실패 시 로그 추가
                log.error("CommentTitle 암호화 실패: {}", e.getMessage());
            }
        }

        if (comment.getCommentContent() != null && !comment.getCommentContent().isEmpty()) {
            try {
                String encryptedContent = stringEncryptor.encrypt(comment.getCommentContent());
                comment.setCommentContent(encryptedContent);
            } catch (Exception e) {
                // [수정] 암호화 실패 시 로그 추가
                log.error("CommentContent 암호화 실패: {}", e.getMessage());
            }
        }

        comment.setCreatedAt(LocalDateTime.now());
        studyCommentMapper.insertComment(comment);
        studyCommentMapper.insertLog(comment);
        return comment;
    }

    @Transactional
    public StudyCommentDto updateComment(long commentId, String currentUserId, StudyCommentDto studyCommentDto, CommentUpdateLogDto commentUpdateLogDto) {
        StudyCommentDto existingComment = studyCommentMapper.findCommentById(commentId);
        if (existingComment == null) {
            throw new IllegalStateException("코멘트를 찾을 수 없습니다: " + commentId);
        }

        String decryptedUserId = existingComment.getUserId();
        if (decryptedUserId != null && !decryptedUserId.isEmpty()) {
            try {
                decryptedUserId = stringEncryptor.decrypt(decryptedUserId);
            } catch (Exception e) {
                log.warn("수정 권한 체크 중 UserID 복호화 실패 (Comment ID: {}): {}", commentId, e.getMessage());
            }
        }

        if (!Objects.equals(decryptedUserId, currentUserId)) {
            throw new IllegalStateException("코멘트를 수정할 권한이 없습니다.");
        }

        // ... (이하 암호화 로직에도 위와 같이 로그 추가를 권장합니다) ...

        commentUpdateLogDto.setOriginalTitle(existingComment.getCommentTitle());
        commentUpdateLogDto.setOriginalContent(existingComment.getCommentContent());

        studyCommentDto.setCommentId(commentId);
        // 업데이트할 내용 암호화
        studyCommentDto.setCommentTitle(stringEncryptor.encrypt(studyCommentDto.getCommentTitle()));
        studyCommentDto.setCommentContent(stringEncryptor.encrypt(studyCommentDto.getCommentContent()));

        studyCommentMapper.updateComment(studyCommentDto);
        studyCommentMapper.updateLog(commentUpdateLogDto);

        return studyCommentMapper.findCommentById(commentId);
    }

    @Transactional
    public void deleteComment(long commentId, String currentUserId, CommentDeleteLogDto commentDeleteLogDto) {
        StudyCommentDto existingComment = studyCommentMapper.findCommentById(commentId);
        if (existingComment == null) {
            throw new IllegalStateException("코멘트를 찾을 수 없습니다: " + commentId);
        }

        String decryptedUserId = existingComment.getUserId();
        if (decryptedUserId != null && !decryptedUserId.isEmpty()) {
            try {
                decryptedUserId = stringEncryptor.decrypt(decryptedUserId);
            } catch (Exception e) {
                log.warn("삭제 권한 체크 중 UserID 복호화 실패 (Comment ID: {}): {}", commentId, e.getMessage());
            }
        }

        if (!Objects.equals(decryptedUserId, currentUserId)) {
            throw new IllegalStateException("코멘트를 삭제할 권한이 없습니다.");
        }

        commentDeleteLogDto.setOriginalTitle(existingComment.getCommentTitle());
        commentDeleteLogDto.setOriginalContent(existingComment.getCommentContent());

        studyCommentMapper.deleteComment(commentId);
        studyCommentMapper.deleteLog(commentDeleteLogDto);
    }

    @Transactional(readOnly = true) // [수정] readOnly = true 추가
    public List<LogShowDto> getAllLogs(Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 100);
        List<LogShowDto> logs = studyCommentMapper.showAllLogs(p, s);

        // 로그 데이터 복호화 (반복되는 로직이므로 별도 메소드로 추출하는 것을 고려해볼 수 있습니다)
        logs.forEach(logEntry -> {
            try {
                if (logEntry.getUserId() != null) logEntry.setUserId(stringEncryptor.decrypt(logEntry.getUserId()));
                if (logEntry.getOriginalTitle() != null) logEntry.setOriginalTitle(stringEncryptor.decrypt(logEntry.getOriginalTitle()));
                if (logEntry.getOriginalContent() != null) logEntry.setOriginalContent(stringEncryptor.decrypt(logEntry.getOriginalContent()));
                if (logEntry.getNewTitle() != null) logEntry.setNewTitle(stringEncryptor.decrypt(logEntry.getNewTitle()));
                if (logEntry.getNewContent() != null) logEntry.setNewContent(stringEncryptor.decrypt(logEntry.getNewContent()));
            } catch (Exception e) {
                log.warn("로그 데이터 복호화 실패 (Log ID: {}): {}", logEntry.getLogId(), e.getMessage());
            }
        });

        return logs;
    }

    @Transactional(readOnly = true) // [수정] readOnly = true 추가
    public List<LogShowDto> getViewLogs(Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 100);
        List<LogShowDto> logs = studyCommentMapper.showViewLogs(p, s);

        // userId만 복호화 (View 로그는 userId만 있음)
        logs.forEach(logEntry -> {
            try {
                if (logEntry.getUserId() != null) logEntry.setUserId(stringEncryptor.decrypt(logEntry.getUserId()));
            } catch (Exception e) {
                log.warn("View 로그 UserID 복호화 실패 (Log ID: {}): {}", logEntry.getLogId(), e.getMessage());
            }
        });

        return logs;
    }
}