package com.example.med.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String userId;          // USER_ID
    private String userName;        // USER_NAME
    private String email;           // EMAIL
    private LocalDateTime createdAt;// CREATED_AT
    private LocalDateTime updatedAt;// UPDATED_AT
    private Integer createdYear;    // EXTRACT(YEAR FROM CREATED_AT) 편의용
}