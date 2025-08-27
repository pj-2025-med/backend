package com.example.med.controller;

import com.example.med.dto.AnnotationDto;
import com.example.med.service.AnnotationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/annotations")
@RequiredArgsConstructor
@Slf4j
public class AnnotationController {

    private final AnnotationService annotationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/studies/{studyKey}/series/{seriesKey}/images/{imageKey}")
    public ResponseEntity<List<AnnotationDto>> getAnnotations(
            @PathVariable long studyKey,
            @PathVariable long seriesKey,
            @PathVariable long imageKey,
            @RequestParam(required = false) Integer frameNo) {
        List<AnnotationDto> annotations = annotationService.getAnnotations(studyKey, seriesKey, imageKey, frameNo);
        return ResponseEntity.ok(annotations);
    }

    @PostMapping("/studies/{studyKey}/series/{seriesKey}/images/{imageKey}")
    public ResponseEntity<Void> saveAnnotations(
            @PathVariable long studyKey,
            @PathVariable long seriesKey,
            @PathVariable long imageKey,
            @RequestParam(required = false) Integer frameNo,
            @RequestBody Map<String, Object> annotationData, // String 대신 Map으로 받음
            @AuthenticationPrincipal String userId) {
        try {
            // Map을 다시 JSON 문자열로 변환하여 데이터 유효성 보장
            String annotationsAsString = objectMapper.writeValueAsString(annotationData);
            annotationService.saveAnnotations(studyKey, seriesKey, imageKey, frameNo, annotationsAsString, userId);
            return ResponseEntity.ok().build();
        } catch (JsonProcessingException e) {
            log.warn("Failed to process annotation JSON data", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
