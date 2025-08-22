package com.example.med.dto;

import lombok.Data;

import java.util.List;

@Data
public class DicomSeriesDto {
    private long seriesKey;
    private String volumeId;
    List<String> imageIds;
}