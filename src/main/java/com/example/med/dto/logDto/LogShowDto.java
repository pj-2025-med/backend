package com.example.med.dto.logDto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogShowDto {
    private Long logId;
    private Long studyKey;
    private String userId;
    private Long commentId;
    private String commentType;
    private String originalTitle;
    private String originalContent;
    private String newTitle;
    private String newContent;
    private String actionType;
    private String createdAt;
    private LocalDateTime updatedAt;
}
