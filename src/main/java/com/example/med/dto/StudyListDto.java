package com.example.med.dto;


import lombok.Data;

@Data
public class StudyListDto {
    private Long studyKey;
    private String studyDate;
    private String studyTime;
    private String studyDesc;
    private String modality;
    private String bodyPart;
    private String patAge;
}
