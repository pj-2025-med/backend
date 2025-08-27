package com.example.med.service;

import com.example.med.dto.logDto.CommentDeleteLogDto;
import com.example.med.dto.logDto.CommentUpdateLogDto;
import com.example.med.dto.StudyCommentDto;
import com.example.med.mapper.DicomMapper;
import com.example.med.mapper.StudyCommentMapper;
import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        List<Long> seriesKeys = dicomMapper.findSeriesKeys(comment.getStudyKey());
        if (seriesKeys == null || seriesKeys.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 studyKey입니다: " + comment.getStudyKey());
        }
        studyCommentMapper.insertComment(comment);
        return comment;
    }

    @Transactional
    public StudyCommentDto updateComment(long commentId, String currentUserId, StudyCommentDto studyCommentDto, CommentUpdateLogDto commentUpdateLogDto) {
        StudyCommentDto existingComment = studyCommentMapper.findCommentById(commentId);
        if (existingComment == null) {
            throw new IllegalStateException("코멘트를 찾을 수 없습니다: " + commentId);
        }

        if (!Objects.equals(existingComment.getUserId(), currentUserId)) {
            throw new IllegalStateException("코멘트를 수정할 권한이 없습니다.");
        }
        studyCommentDto.setCommentId(commentId);
        studyCommentMapper.updateComment(studyCommentDto);
        studyCommentMapper.insertLog(commentUpdateLogDto);
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

        // 2. 코멘트 작성자와 현재 로그인한 사용자가 동일한지 확인 (권한 체크)
        if (!Objects.equals(existingComment.getUserId(), currentUserId)) {
            throw new IllegalStateException("코멘트를 삭제할 권한이 없습니다.");
        }

        // 3. DB에서 코멘트 삭제
        studyCommentMapper.deleteComment(commentId);
        studyCommentMapper.deleteLog(commentDeleteLogDto);
    }
}
