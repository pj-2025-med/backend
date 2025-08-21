package com.example.med.controller;

import com.example.med.service.DicomService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dicom")
@RequiredArgsConstructor
public class DicomController {

    private final DicomService dicomService;

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

    // wadouri 스트리밍 엔드포인트
    // 예: wadouri:http://host/api/v1/dicom/studies/100/series/10/images/1234
    @GetMapping("/studies/{studyKey}/series/{seriesKey}/images/{imageKey}")
    public ResponseEntity<ResourceRegion> streamDicom(
            @PathVariable long studyKey,
            @PathVariable long seriesKey,
            @PathVariable long imageKey,
            @RequestHeader(value = "Range", required = false) String range
    ) throws Exception {
        studyKey = 21;
        seriesKey = 1;
        imageKey = 1;

        FileSystemResource resource = dicomService.getDicomResource(studyKey, seriesKey, imageKey);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ResourceRegion region = dicomService.buildRegion(resource, range);
        HttpStatus status = (range == null) ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;

        return ResponseEntity.status(status)
                .contentType(dicomService.dicomMediaType())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(region);
    }
}