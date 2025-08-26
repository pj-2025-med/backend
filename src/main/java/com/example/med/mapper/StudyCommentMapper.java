package com.example.med.mapper;

import com.example.med.dto.StudyCommentDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudyCommentMapper {

    List<StudyCommentDto> findCommentsByStudyKey(@Param("studyKey") long studyKey);

    int insertComment(StudyCommentDto comment);

    StudyCommentDto findCommentById(@Param("commentId") long commentId);

    int updateComment(StudyCommentDto comment);

    int deleteComment(@Param("commentId") long commentId);

}
