package com.example.med.dto.logDto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnoDto {
    private Long annoImageId;
    private Long studyKey;
    private Long seriesKey;
    private Long imageKey;
    private String annotation;
    private String createdBy;
    private LocalDateTime createdAt;
}
