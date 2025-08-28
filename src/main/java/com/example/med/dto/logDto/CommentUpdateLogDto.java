package com.example.med.dto.logDto;

import com.example.med.dto.StudyCommentDto;
import lombok.Data;

@Data
public class CommentUpdateLogDto {
    //url
    private Long commentId;
    //url
    private Long studyKey;
    //cookie
    private String userId;
    //body
    private String commentTitle;
    //body
    private String commentContent;
    //body
    private String createdAt;
    //body
    private String originalTitle;
    //body
    private String originalContent;


    public StudyCommentDto toStudyCommentDto(Long commentId, Long studyKey, String userId) {
        StudyCommentDto dto = new StudyCommentDto();
        dto.setCommentId(commentId);
        dto.setStudyKey(studyKey);
        dto.setUserId(userId);
        dto.setCommentTitle(this.commentTitle);
        dto.setCommentContent(this.commentContent);
        return dto;
    }

}
