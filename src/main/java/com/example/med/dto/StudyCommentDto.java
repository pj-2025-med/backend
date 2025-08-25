package com.example.med.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudyCommentDto {
    private Long commentId;
    private Long studyKey;
    private String userId;
    private String commentTitle;
    private String commentContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
