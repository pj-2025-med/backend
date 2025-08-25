package com.example.med.dto;

import lombok.Data;

import java.util.List;

@Data
public class PatientInfo {
    String PID;
    String PNAME;
    String PSEX;
    String PBIRTHDATE;
    List<StudyListDto> studyListDto;
}
