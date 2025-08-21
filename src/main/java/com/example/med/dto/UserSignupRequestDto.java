package com.example.med.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignupRequestDto {
    private String userId;
    private String password;
    private String userName;
    private String email;
}
