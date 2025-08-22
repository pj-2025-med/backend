package com.example.med.service;

import com.example.med.config.PacsStorageProps;
import com.example.med.dto.FilePathDto;
import com.example.med.mapper.DicomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DicomService {

    private final DicomMapper dicomMapper;
    private final PacsStorageProps props;

    public List<Long> listSeriesKeys(long studyKey) {
        return dicomMapper.findSeriesKeys(studyKey);
    }

    public List<Long> listImageKeys(long studyKey, long seriesKey) {
        return dicomMapper.findImageKeys(studyKey, seriesKey);
    }

    public FileSystemResource getDicomResource(long studyKey, long seriesKey, long imageKey) {
        FilePathDto fp = dicomMapper.findImagePath(studyKey, seriesKey, imageKey);
        if (fp == null || !StringUtils.hasText(fp.getFname())) return null;

        // PATH 끝/시작 슬래시를 고려하여 안전하게 합치기
        String base = props.getBasePath();          // \\WW210.94.241.9\STS\
        String sub  = (fp.getPath() == null) ? "" : fp.getPath();
        String full = joinWindowsPath(base, sub, fp.getFname());

        File file = new File(full);
        if (!file.exists() || !file.isFile()) return null;

        return new FileSystemResource(file);
    }

    public ResourceRegion buildRegion(FileSystemResource resource, String rangeHeader) throws Exception {
        long contentLength = resource.contentLength();
        long chunkSize = 1024 * 1024; // 1MB

        if (rangeHeader == null) {
            return new ResourceRegion(resource, 0, Math.min(chunkSize, contentLength));
        }
        HttpRange range = HttpRange.parseRanges(rangeHeader).get(0);
        long start = range.getRangeStart(contentLength);
        long end   = range.getRangeEnd(contentLength);
        long rangeLength = Math.min(chunkSize, end - start + 1);
        return new ResourceRegion(resource, start, rangeLength);
    }

    public MediaType dicomMediaType() {
        return MediaType.parseMediaType("application/dicom");
    }

    /** 윈도우 경로 조합: 백슬래시 정규화 */
    private String joinWindowsPath(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (!StringUtils.hasText(parts[i])) continue;
            String p = parts[i].replace('/', '\\');
            if (sb.length() == 0) {
                sb.append(p);
            } else {
                boolean endBS = sb.charAt(sb.length() - 1) == '\\';
                boolean startBS = p.charAt(0) == '\\';
                if (endBS && startBS) sb.setLength(sb.length() - 1);
                else if (!endBS && !startBS) sb.append('\\');
                sb.append(p);
            }
        }
        return sb.toString();
    }
}
