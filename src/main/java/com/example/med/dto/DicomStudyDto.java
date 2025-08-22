package com.example.med.dto;

import lombok.Data;

import java.util.List;

@Data
public class DicomStudyDto {
    private long studyKey;
    private List<DicomSeriesDto> series;
}