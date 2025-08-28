package com.example.med.mapper;

import com.example.med.dto.logDto.AnnoDto;
import com.example.med.dto.logDto.CommentDeleteLogDto;
import com.example.med.dto.logDto.CommentUpdateLogDto;
import com.example.med.dto.StudyCommentDto;
import com.example.med.dto.logDto.LogShowDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudyCommentMapper {

    List<StudyCommentDto> findCommentsByStudyKey(@Param("studyKey") long studyKey);

    int insertComment(StudyCommentDto comment);

    StudyCommentDto findCommentById(@Param("commentId") long commentId);

    int updateComment(StudyCommentDto comment);

    int deleteComment(@Param("commentId") long commentId);

    //로그찍기
    int updateLog(CommentUpdateLogDto updateDto);
    int deleteLog(CommentDeleteLogDto commentDeleteLogDto);
    int insertLog(StudyCommentDto commentDto);

    List<LogShowDto> showAllLogs();
    int insertAnnoLog(AnnoDto annoDto);
}
