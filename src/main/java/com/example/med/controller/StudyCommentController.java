package com.example.med.controller;

import com.example.med.dto.logDto.CommentDeleteLogDto;
import com.example.med.dto.logDto.CommentUpdateLogDto;
import com.example.med.dto.StudyCommentDto;
import com.example.med.service.StudyCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dicom/study")
@RequiredArgsConstructor
@Slf4j
public class StudyCommentController {

    private final StudyCommentService studyCommentService;

    @GetMapping("/{studyKey}/comment")
    public ResponseEntity<List<StudyCommentDto>> getStudyComments(@PathVariable long studyKey) {
        List<StudyCommentDto> comments = studyCommentService.getCommentsByStudyKey(studyKey);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{studyKey}/comment")
    public ResponseEntity<StudyCommentDto> createStudyComment(
            @PathVariable long studyKey,
            @RequestBody StudyCommentDto commentDto,
            @AuthenticationPrincipal String userId // 로그인한 사용자의 ID를 직접 주입받음
    ) {
        commentDto.setStudyKey(studyKey);
        commentDto.setUserId(userId); // 주입받은 사용자 ID를 DTO에 설정

        StudyCommentDto createdComment = studyCommentService.createComment(commentDto);
        return ResponseEntity.ok(createdComment);
    }

    @PutMapping("/{studyKey}/comment/{commentId}")
    public ResponseEntity<StudyCommentDto> updateStudyComment(
            @PathVariable long studyKey,
            @PathVariable long commentId,
            @RequestBody CommentUpdateLogDto commentUpdateLogDto,
            @AuthenticationPrincipal String userId
    ) {
        commentUpdateLogDto.setUserId(userId);
        commentUpdateLogDto.setCommentId(commentId);
        commentUpdateLogDto.setStudyKey(studyKey);

        StudyCommentDto updateDto = commentUpdateLogDto.toStudyCommentDto(commentId, studyKey, userId);
        StudyCommentDto updatedComment = studyCommentService.updateComment(commentId, userId, updateDto, commentUpdateLogDto);

        log.info("로그1" + updateDto.toString());
        log.info("로그2" + commentUpdateLogDto.toString());
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{studyKey}/comment/{commentId}")
    public ResponseEntity<Void> deleteStudyComment(
            @PathVariable long studyKey,
            @PathVariable long commentId,
            @RequestBody CommentDeleteLogDto commentDeleteLogDto,
            @AuthenticationPrincipal String userId
    ) {
        log.info("딜리트" +  commentId + " " + studyKey);


        commentDeleteLogDto.setUserId(userId);
        commentDeleteLogDto.setCommentId(commentId);
        commentDeleteLogDto.setStudyKey(studyKey);

        studyCommentService.deleteComment(commentId, userId, commentDeleteLogDto);
        return ResponseEntity.ok().build();
    }
}
