package com.example.med.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private String userId;
    private String password;
    private String userName;
    private String email;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String role;
}