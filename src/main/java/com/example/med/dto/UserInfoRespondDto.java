package com.example.med.dto;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoRespondDto {
    private String userId;
    private String userName;
    private String email;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
