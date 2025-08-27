package com.example.med.service;

import com.example.med.dto.AnnotationDto;
import com.example.med.mapper.second.AnnotationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationMapper annotationMapper;

    public List<AnnotationDto> getAnnotations(long studyKey, long seriesKey, long imageKey, Integer frameNo) {
        return annotationMapper.findAnnotations(studyKey, seriesKey, imageKey, frameNo);
    }

    public void saveAnnotations(long studyKey, long seriesKey, long imageKey, Integer frameNo, String annotations, String userId) {
        AnnotationDto dto = new AnnotationDto();
        dto.setStudyKey(studyKey);
        dto.setSeriesKey(seriesKey);
        dto.setImageKey(imageKey);
        dto.setFrameNo(frameNo != null ? frameNo : -1);
        dto.setAnnotations(annotations);
        dto.setCreatedBy(userId);

        // H2 DB는 MERGE, Oracle은 MERGE INTO를 사용해 UPSERT 처리
        annotationMapper.upsertAnnotations(dto);
    }
}
