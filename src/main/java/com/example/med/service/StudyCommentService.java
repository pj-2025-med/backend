package com.example.med.service;

import com.example.med.dto.CommentUpdateDto;
import com.example.med.dto.StudyCommentDto;
import com.example.med.mapper.DicomMapper;
import com.example.med.mapper.StudyCommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudyCommentService {

    private final DicomMapper dicomMapper; // 메인 DB 매퍼
    private final StudyCommentMapper studyCommentMapper; // 서브 DB 매퍼

    @Transactional(readOnly = true)
    public List<StudyCommentDto> getCommentsByStudyKey(long studyKey) {
        return studyCommentMapper.findCommentsByStudyKey(studyKey);
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
    public StudyCommentDto updateComment(long commentId, String currentUserId, StudyCommentDto studyCommentDto, CommentUpdateDto commentUpdateDto) {
        StudyCommentDto existingComment = studyCommentMapper.findCommentById(commentId);
        if (existingComment == null) {
            throw new IllegalStateException("코멘트를 찾을 수 없습니다: " + commentId);
        }

        if (!Objects.equals(existingComment.getUserId(), currentUserId)) {
            throw new IllegalStateException("코멘트를 수정할 권한이 없습니다.");
        }
        studyCommentDto.setCommentId(commentId);
        studyCommentMapper.updateComment(studyCommentDto);
        studyCommentMapper.insertLog(commentUpdateDto);
        return studyCommentMapper.findCommentById(commentId);
    }

    /**
     * 코멘트를 삭제합니다.
     * @param commentId 삭제할 코멘트의 ID
     * @param currentUserId 현재 로그인한 사용자의 ID
     */
    @Transactional
    public void deleteComment(long commentId, String currentUserId) {
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
    }
}
