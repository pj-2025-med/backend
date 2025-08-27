package com.example.med.service;

import com.example.med.dto.logDto.CommentDeleteLogDto;
import com.example.med.dto.logDto.CommentUpdateLogDto;
import com.example.med.dto.StudyCommentDto;
import com.example.med.mapper.DicomMapper;
import com.example.med.mapper.StudyCommentMapper;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public List<StudyCommentDto> getCommentsByStudyKey(long studyKey) {
        return studyCommentMapper.findCommentsByStudyKey(studyKey);
    }

    @Transactional
    public StudyCommentDto createComment(StudyCommentDto comment) {
        // 1. studyKey 유효성 검증
        List<Long> seriesKeys = dicomMapper.findSeriesKeys(comment.getStudyKey());
        if (seriesKeys == null || seriesKeys.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 studyKey입니다: " + comment.getStudyKey());
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

        // 2. 코멘트 작성자와 현재 로그인한 사용자가 동일한지 확인 (권한 체크)
        if (!Objects.equals(existingComment.getUserId(), currentUserId)) {
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
}
