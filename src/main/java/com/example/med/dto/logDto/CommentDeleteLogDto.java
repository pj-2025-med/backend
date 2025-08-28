package com.example.med.dto.logDto;

import lombok.Data;

@Data
public class CommentDeleteLogDto {
    private Long commentId;
    private Long studyKey;
    private String userId;
    private String originalTitle;
    private String originalContent;
    private String createdAt;
}
