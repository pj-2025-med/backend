package com.example.med.service;

import com.example.med.dto.logDto.ViewLogDto;
import com.example.med.mapper.StudyCommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViewLogService {

    private final StudyCommentMapper studyCommentMapper;

    public void viewLog(String userId, Long studyKey){
        ViewLogDto dto = new ViewLogDto();
        dto.setStudyKey(studyKey);
        dto.setUserId(userId);

        studyCommentMapper.insertViewLog(dto);

    }
}
