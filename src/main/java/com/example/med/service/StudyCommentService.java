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
public class StudyCommentService {

    private static final Logger log = LoggerFactory.getLogger(StudyCommentService.class);

    private final DicomMapper dicomMapper; // 메인 DB 매퍼
    private final StudyCommentMapper studyCommentMapper; // 서브 DB 매퍼
    private final StringEncryptor stringEncryptor; // Jasypt 암호/복호화

    // 생성자를 직접 작성하여 @Qualifier로 의존성 주입을 명시
    public StudyCommentService(DicomMapper dicomMapper,
                               StudyCommentMapper studyCommentMapper,
                               @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {
        this.dicomMapper = dicomMapper;
        this.studyCommentMapper = studyCommentMapper;
        this.stringEncryptor = stringEncryptor;
    }

    private boolean isEncrypted(String value) {
        // 평문타입이라서 정규식 사용
        return value != null && value.matches("^[A-Za-z0-9+/=]{16,}$");
    }

    @Transactional(readOnly = true)
    public List<StudyCommentDto> getCommentsByStudyKey(long studyKey) {
        List<StudyCommentDto> comments = studyCommentMapper.findCommentsByStudyKey(studyKey);
        for (StudyCommentDto comment : comments) {
            String encryptedTitle = comment.getCommentTitle();
            if (encryptedTitle != null && !encryptedTitle.isEmpty()) {
                try {
                    String decryptedTitle = stringEncryptor.decrypt(encryptedTitle);
                    comment.setCommentTitle(decryptedTitle);
                } catch (Exception e) {
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
                    log.warn("CommentContent 복호화 실패 (Comment ID: {}): {}", comment.getCommentId(), e.getMessage());
                }
            }
        }
        return comments;
    }

    @Transactional
    public StudyCommentDto createComment(StudyCommentDto comment) {
        // studyKey 유효성 검증
        List<Long> seriesKeys = dicomMapper.findSeriesKeys(comment.getStudyKey());
        if (seriesKeys == null || seriesKeys.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 studyKey입니다: " + comment.getStudyKey());
        }

        if (comment.getCommentTitle() != null && !comment.getCommentTitle().isEmpty()) {
            try {
                String encryptedTitle = stringEncryptor.encrypt(comment.getCommentTitle());
                comment.setCommentTitle(encryptedTitle);
            } catch (Exception e) {
                log.error("CommentTitle 암호화 실패: {}", e.getMessage());
            }
        }

        if (comment.getCommentContent() != null && !comment.getCommentContent().isEmpty()) {
            try {
                String encryptedContent = stringEncryptor.encrypt(comment.getCommentContent());
                comment.setCommentContent(encryptedContent);
            } catch (Exception e) {
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

        commentUpdateLogDto.setOriginalTitle(existingComment.getCommentTitle());
        commentUpdateLogDto.setOriginalContent(existingComment.getCommentContent());


        if (commentUpdateLogDto.getCommentTitle() != null && !commentUpdateLogDto.getCommentTitle().isEmpty()) {
            try {
                String encryptedNewTitle = stringEncryptor.encrypt(commentUpdateLogDto.getCommentTitle());
                commentUpdateLogDto.setCommentTitle(encryptedNewTitle);
            } catch (Exception e) {
                log.error("로그용 CommentTitle 암호화 실패: {}", e.getMessage());
            }
        }

        if (commentUpdateLogDto.getCommentContent() != null && !commentUpdateLogDto.getCommentContent().isEmpty()) {
            try {
                String encryptedNewContent = stringEncryptor.encrypt(commentUpdateLogDto.getCommentContent());
                commentUpdateLogDto.setCommentContent(encryptedNewContent);
            } catch (Exception e) {
                log.error("로그용 CommentContent 암호화 실패: {}", e.getMessage());
            }
        }

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

    @Transactional(readOnly = true)
    public List<LogShowDto> getAllLogs(Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 100);
        List<LogShowDto> logs = studyCommentMapper.showAllLogs(p, s);


        logs.forEach(logEntry -> {
            try {
                if (logEntry.getUserId() != null && isEncrypted(logEntry.getUserId())) logEntry.setUserId(stringEncryptor.decrypt(logEntry.getUserId()));
                if (logEntry.getOriginalTitle() != null && isEncrypted(logEntry.getOriginalTitle())) logEntry.setOriginalTitle(stringEncryptor.decrypt(logEntry.getOriginalTitle()));
                if (logEntry.getOriginalContent() != null && isEncrypted(logEntry.getOriginalContent())) logEntry.setOriginalContent(stringEncryptor.decrypt(logEntry.getOriginalContent()));
                if (logEntry.getNewTitle() != null && isEncrypted(logEntry.getNewTitle())) logEntry.setNewTitle(stringEncryptor.decrypt(logEntry.getNewTitle()));
                if (logEntry.getNewContent() != null && isEncrypted(logEntry.getNewContent())) logEntry.setNewContent(stringEncryptor.decrypt(logEntry.getNewContent()));
            } catch (Exception e) {
                log.warn("로그 데이터 복호화 실패 (Log ID: {}): {}", logEntry.getLogId(), e.getMessage());
            }
        });

        return logs;
    }

    @Transactional(readOnly = true)
    public List<LogShowDto> getViewLogs(Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 100);

        return studyCommentMapper.showViewLogs(p, s);
    }
}