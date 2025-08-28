package com.example.med.controller;

import com.example.med.dto.logDto.LogShowDto;
import com.example.med.service.StudyCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogViewController {

    private final StudyCommentService studyCommentService;

    @GetMapping("/showAll")
    public ResponseEntity<List<LogShowDto>> showAllLogs(@AuthenticationPrincipal String userId){
        List<LogShowDto> allLogs = studyCommentService.getAllLogs();
        return ResponseEntity.ok(allLogs);
    }
}
