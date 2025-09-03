package com.example.med.controller;

import com.example.med.dto.logDto.LogShowDto;
import com.example.med.service.StudyCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogViewController {

    private final StudyCommentService studyCommentService;

    @GetMapping("/showAll")
    public ResponseEntity<?> showAllLogs(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        if (!"admin".equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "관리자만 접근 가능합니다."));
        }
        List<LogShowDto> allLogs = studyCommentService.getAllLogs(page, size);
        return ResponseEntity.ok(allLogs);
    }

    @GetMapping("/showViewLog")
    public ResponseEntity<?> showViewLogs(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        if (!"admin".equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "관리자만 접근 가능합니다."));
        }
        List<LogShowDto> allLogs = studyCommentService.getViewLogs(page, size);
        return ResponseEntity.ok(allLogs);
    }
}
