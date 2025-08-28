package com.example.med.controller;

import com.example.med.dto.DicomStudyDto;
import com.example.med.service.DicomService;
import com.example.med.service.ViewLogService;
import com.example.med.util.DicomUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dicom")
@RequiredArgsConstructor
@Slf4j
public class DicomController {

    private final DicomService dicomService;
    private final DicomUtil dicomUtil;
    private final ViewLogService viewLogService;

    // studyKey 내 seriesKey 목록
    @GetMapping("/studies/{studyKey}/series")
    public ResponseEntity<List<Long>> listSeriesKeys(@PathVariable long studyKey) {
        return ResponseEntity.ok(dicomService.listSeriesKeys(studyKey));
    }

    // 시리즈 내 imageKey 목록
    @GetMapping("/studies/{studyKey}/series/{seriesKey}/images")
    public ResponseEntity<List<Long>> listImageKeys(
            @PathVariable long studyKey,
            @PathVariable long seriesKey
    ) {
        return ResponseEntity.ok(dicomService.listImageKeys(studyKey, seriesKey));
    }

    @GetMapping("/studies/{studyKey}/series/{seriesKey}/images/{imageKey}")
    public void streamDicomRaw(
            @PathVariable long studyKey,
            @PathVariable long seriesKey,
            @PathVariable long imageKey,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        var resource = dicomService.getDicomResource(studyKey, seriesKey, imageKey);

        if (resource == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        dicomUtil.dicomDeal(resource, request, response);
    }

    @GetMapping("/studies/{studyKey}")
    @ResponseBody
    public DicomStudyDto studyDicomRaw(@PathVariable long studyKey,
                                       @AuthenticationPrincipal String userId) throws IOException {
        viewLogService.viewLog(userId, studyKey);
        return dicomService. getStudyDicom(studyKey);
    }

}
