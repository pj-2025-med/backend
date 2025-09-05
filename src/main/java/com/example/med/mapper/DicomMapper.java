package com.example.med.mapper;

import com.example.med.dto.FilePathDto;
import com.example.med.dto.PatientInfo;
import com.example.med.dto.PatientStudyFlatDto;
import com.example.med.dto.StudyListDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DicomMapper {

    // studyKey 내 seriesKey 목록
    List<Long> findSeriesKeys(@Param("studyKey") long studyKey);

    // 특정 시리즈의 이미지(프레임) 키 목록 (정렬: INSTANCENUM 숫자화 → 실패시 IMAGEKEY)
    List<Long> findImageKeys(
            @Param("studyKey") long studyKey,
            @Param("seriesKey") long seriesKey
    );

    // 실제 파일 경로 조합에 필요한 PATH, FNAME 조회
    FilePathDto findImagePath(
            @Param("studyKey") long studyKey,
            @Param("seriesKey") long seriesKey,
            @Param("imageKey") long imageKey
    );

    List<PatientInfo> findPatientInfoByName(@Param("patientname") String patientname);
    List<StudyListDto> findStudyListByPid(@Param("pid") String pid);
    //List<PatientInfoByModalityDto> findPatientInfoByModality(@Param("modality") String modality);
    List<PatientInfo> findPatientInfoByModality(@Param("modality") String modality);
    List<PatientStudyFlatDto> findPatientsWithStudiesByModality(@Param("modality") String modality);
    List<PatientStudyFlatDto> findPatientsWithStudiesByPatientID(@Param("patientID") String patientID);
}
