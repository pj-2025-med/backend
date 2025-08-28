package com.example.med.dto;

import com.example.med.dto.logDto.AnnoDto;
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


    public static AnnoDto toAnnoDto(AnnotationDto dto) {
        if (dto == null) return null;

        AnnoDto annoDto = new AnnoDto();
        annoDto.setAnnoImageId(dto.getAnnoImageId());
        annoDto.setStudyKey(dto.getStudyKey());
        annoDto.setSeriesKey(dto.getSeriesKey());
        annoDto.setImageKey(dto.getImageKey());
        annoDto.setAnnotation(dto.getAnnotations());
        annoDto.setCreatedBy(dto.getCreatedBy());

        return annoDto;
    }
}
