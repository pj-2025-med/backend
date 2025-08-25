package com.example.med.service;

import com.example.med.dto.PatientInfo;
import com.example.med.dto.PatientStudyFlatDto;
import com.example.med.dto.StudyListDto;
import com.example.med.mapper.DicomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private final DicomMapper dicomMapper;

    @Transactional(readOnly = true)
    public List<PatientInfo> searchByPatientName(String patientname) {
        List<PatientInfo> patients = dicomMapper.findPatientInfoByName(patientname);
        if (patients == null || patients.isEmpty()) {
            return List.of();
        }
        for (PatientInfo p : patients) {
            // Point: 이름이 아니라 PID로 조회!
            List<StudyListDto> studies = dicomMapper.findStudyListByPid(p.getPID());
            p.setStudyListDto(studies);
        }
        return patients;
    }

    @Transactional(readOnly = true)
    public List<PatientStudyFlatDto> searchByModality(String modality) {
        if (modality == null || modality.isBlank()) return List.of();
        String m = modality.trim();
        return Optional.ofNullable(dicomMapper.findPatientsWithStudiesByModality(m))
                .orElse(List.of());
    }

//    public List<PatientInfoByModalityDto> getPatientInfoByModality(String modality) {
//        return dicomMapper.findPatientInfoByModality(modality);
//    }
}
