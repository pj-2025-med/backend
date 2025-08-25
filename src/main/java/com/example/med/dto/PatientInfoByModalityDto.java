package com.example.med.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientInfoByModalityDto {
    private String patientName;
    private String studyDescription;
    private String studyDate;
}
