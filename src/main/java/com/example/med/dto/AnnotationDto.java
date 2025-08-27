package com.example.med.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationDto {
    private Long annoImageId;
    private Long studyKey;
    private Long seriesKey;
    private Long imageKey;
    private Integer frameNo;
    private String annotations;
    private String createdBy;
    private String createdAt;
}
