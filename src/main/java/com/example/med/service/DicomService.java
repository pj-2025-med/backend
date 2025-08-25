package com.example.med.service;

import com.example.med.config.PacsStorageProps;
import com.example.med.dto.DicomSeriesDto;
import com.example.med.dto.DicomStudyDto;
import com.example.med.dto.FilePathDto;
import com.example.med.dto.PatientInfoByModalityDto;
import com.example.med.mapper.DicomMapper;
import com.example.med.util.DicomUtil;
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
    private final DicomUtil dicomUtil;
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
        String full = dicomUtil.joinWindowsPath(base, sub, fp.getFname());

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


    public DicomStudyDto getStudyDicom(long studyKey) {
        List<Long> seriesKeys = dicomMapper.findSeriesKeys(studyKey);
        List<DicomSeriesDto> seriesList = seriesKeys.stream().map(seriesKey -> {
            // 2-1) 이미지 키들 (SQL에서 정렬: InstanceNumber 숫자 정렬, 없으면 imageKey)
            List<Long> imageKeys = dicomMapper.findImageKeys(studyKey, seriesKey);

            // 2-2) DTO 생성
            DicomSeriesDto seriesDto = new DicomSeriesDto();
            seriesDto.setSeriesKey(seriesKey);
            // Cornerstone volume 식별자 (나중에 "cornerstoneStreamingImageVolume:<여기>"로 씀)
            seriesDto.setVolumeId("study:" + studyKey + "|series:" + seriesKey);

            // 2-3) 각 이미지 키를 Cornerstone이 읽을 수 있는 wadouri: URL로 변환
            List<String> imageIds = imageKeys.stream()
                    .map(imgKey -> "wadouri:http://localhost:8080/api/v1/dicom/studies/"
                            + studyKey + "/series/" + seriesKey + "/images/" + imgKey)
                    .toList();

            seriesDto.setImageIds(imageIds);
            return seriesDto;
        }).toList();

        DicomStudyDto dto = new DicomStudyDto();
        dto.setStudyKey(studyKey);
        dto.setSeries(seriesList);
        return dto;
    }

    public List<PatientInfoByModalityDto> getPatientInfoByModality(String modality) {
        return dicomMapper.findPatientInfoByModality(modality);
    }
}
