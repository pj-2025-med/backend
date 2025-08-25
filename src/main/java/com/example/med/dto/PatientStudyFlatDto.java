package com.example.med.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientStudyFlatDto {
    // 환자 정보
    private String pid;
    private String pname;
    private String psex;
    private String pbirthdate;

    // study 정보
    private Long studyKey;
    private String studyDate;
    private String studyTime;
    private String studyDesc;
    private String modality;
    private String bodyPart;
    private String patAge;
}