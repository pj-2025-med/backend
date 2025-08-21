package com.example.med.controller;

import com.example.med.service.DicomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dicom")
@RequiredArgsConstructor
@Slf4j
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

    @GetMapping("/studies/{studyKey}/series/{seriesKey}/images/{imageKey}")
    public void streamDicomRaw(
            @PathVariable long studyKey,
            @PathVariable long seriesKey,
            @PathVariable long imageKey,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        studyKey = 21;
        seriesKey = 1;
        imageKey = 1;
        var resource = dicomService.getDicomResource(studyKey, seriesKey, imageKey);
        if (resource == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long fileLength = resource.contentLength();
        String range = request.getHeader("Range");

        long start = 0;
        long end = fileLength - 1;

        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.substring(6).split("-", 2);
            if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) end = Math.min(Long.parseLong(parts[1]), end);
            // 원하는 청크 제한(예: 1MiB)을 걸고 싶다면:
            long maxChunk = 1024L * 1024L;
            end = Math.min(end, start + maxChunk - 1);

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        } else {
            response.setStatus(HttpServletResponse.SC_OK); // 200 전체
        }

        long contentLength = end - start + 1;
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Length", String.valueOf(contentLength));

        try (InputStream in = resource.getInputStream();
             OutputStream out = response.getOutputStream()) {

            in.skip(start);
            byte[] buf = new byte[8192];
            long remaining = contentLength;
            int read;
            while (remaining > 0 && (read = in.read(buf, 0, (int)Math.min(buf.length, remaining))) != -1) {
                out.write(buf, 0, read);
                remaining -= read;
            }
        }
    }
}