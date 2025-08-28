package com.example.med.mapper;

import com.example.med.dto.logDto.*;
import com.example.med.dto.StudyCommentDto;
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

//    List<LogShowDto> showAllLogs();
List<LogShowDto> showAllLogs(@Param("page") Integer page,
                             @Param("size") Integer size);
    int insertAnnoLog(AnnoDto annoDto);
    int insertAnnoUpdateLog(AnnoUpdateDto annoUpdateDto);

    int insertViewLog(ViewLogDto viewLogDto);
}
