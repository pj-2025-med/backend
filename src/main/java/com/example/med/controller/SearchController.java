package com.example.med.controller;

import com.example.med.dto.PatientInfo;
import com.example.med.dto.PatientStudyFlatDto;
import com.example.med.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/patientName/{patientname}")
    public ResponseEntity<List<PatientInfo>> getStudyListByPatientName(@PathVariable String patientname) {
        List<PatientInfo> infoList = searchService.searchByPatientName(patientname);
        if (infoList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(infoList);
    }

//    @GetMapping("/modality/{modality}")
//    public ResponseEntity<List<PatientInfoByModalityDto>> getPatientInfoByModality(@PathVariable String modality) {
//        return ResponseEntity.ok(searchService.getPatientInfoByModality(modality));
//    }

    @GetMapping("/modality/{modality}")
    public ResponseEntity<List<PatientStudyFlatDto>> getStudyListByModality(@PathVariable String modality) {
        return ResponseEntity.ok(searchService.searchByModality(modality));
    }
}
