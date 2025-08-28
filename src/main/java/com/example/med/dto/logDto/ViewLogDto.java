package com.example.med.dto.logDto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ViewLogDto {
    private Long studyKey;
    private String userId;
    private LocalDateTime createdAt;
}
