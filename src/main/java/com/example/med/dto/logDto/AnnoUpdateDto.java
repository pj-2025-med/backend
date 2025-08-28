package com.example.med.dto.logDto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnnoUpdateDto {
    private Long studyKey;
    private Long seriesKey;
    private Long imageKey;
    private Integer frameNo;

    private Long commentId;        // == annoImageId (로그의 COMMENT_ID)
    private String originalContent;
    private String newContent;
    private String createdBy;
    private LocalDateTime createdAt; // 로그 시각(보통 now) or 행의 createdAt을 재사용해도 무방
}
