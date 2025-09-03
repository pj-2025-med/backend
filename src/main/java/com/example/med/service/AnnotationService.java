package com.example.med.service;

import com.example.med.dto.AnnotationDto;
import com.example.med.dto.logDto.AnnoDto;
import com.example.med.dto.logDto.AnnoUpdateDto;
import com.example.med.mapper.StudyCommentMapper;
import com.example.med.mapper.second.AnnotationMapper;
import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationMapper annotationMapper;
    private final StudyCommentMapper studyCommentMapper;
    @Qualifier("jasyptStringEncryptor")
    private final StringEncryptor stringEncryptor; // Jasypt 암호/복호화

    public List<AnnotationDto> getAnnotations(long studyKey, long seriesKey, long imageKey, Integer frameNo) {
        List<AnnotationDto> annotations = annotationMapper.findAnnotations(studyKey, seriesKey, imageKey, frameNo);

        // 조회한 어노테이션 데이터 복호화
        for (AnnotationDto annotation : annotations) {
            if (annotation.getAnnotations() != null) {
                annotation.setAnnotations(stringEncryptor.decrypt(annotation.getAnnotations()));
            }
            if (annotation.getCreatedBy() != null) {
                annotation.setCreatedBy(stringEncryptor.decrypt(annotation.getCreatedBy()));
            }
        }

        return annotations;
    }

    @Transactional
    public void saveAnnotations(long studyKey, long seriesKey, long imageKey,
                                Integer frameNo, String annotations, String userId) {
        // String 타입인 annotations와 userId 암호화
        String encryptedAnnotations = stringEncryptor.encrypt(annotations);
        String encryptedUserId = stringEncryptor.encrypt(userId);

        AnnotationDto dto = new AnnotationDto();
        dto.setStudyKey(studyKey);
        dto.setSeriesKey(seriesKey);
        dto.setImageKey(imageKey);
        dto.setFrameNo(frameNo); // null이면 매퍼에서 NVL(-1)
        dto.setAnnotations(encryptedAnnotations);
        dto.setCreatedBy(encryptedUserId);

        annotationMapper.upsertAnnotations(dto);

        if (dto.getWasUpdated() != null && dto.getWasUpdated() == 1) {
            // UPDATE 로그
            AnnoUpdateDto upd = new AnnoUpdateDto();
            upd.setStudyKey(studyKey);
            upd.setSeriesKey(seriesKey);
            upd.setImageKey(imageKey);
            upd.setFrameNo(frameNo != null ? frameNo : -1);
            upd.setCommentId(dto.getAnnoImageId());                 // COMMENT_ID = annoImageId
            upd.setOriginalContent(dto.getOriginalAnnotations());   // DB의 예전 값
            upd.setNewContent(encryptedAnnotations);                         // 새 값
            upd.setCreatedBy(encryptedUserId);
            upd.setCreatedAt(java.time.LocalDateTime.now());
            studyCommentMapper.insertAnnoUpdateLog(upd);
        } else {
            // INSERT 로그 (기존 C 로그)
            AnnoDto annoDto = new AnnoDto();
            annoDto.setAnnoImageId(dto.getAnnoImageId());
            annoDto.setStudyKey(studyKey);
            annoDto.setSeriesKey(seriesKey);
            annoDto.setImageKey(imageKey);
            annoDto.setAnnotation(encryptedAnnotations);
            annoDto.setCreatedBy(encryptedUserId);
            annoDto.setCreatedAt(dto.getCreatedAt());               // 행의 created_at
            studyCommentMapper.insertAnnoLog(annoDto);
        }
    }
}
