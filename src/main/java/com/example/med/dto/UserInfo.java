package com.example.med.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserInfo {
    private String userId;
    private String password;
    private String userName;
    private String email;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}