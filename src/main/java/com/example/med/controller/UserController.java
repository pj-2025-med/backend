package com.example.med.controller;

import com.example.med.dto.UserLoginRequestDto;
import com.example.med.dto.UserSignupRequestDto;
import com.example.med.dto.UserInfo;
import com.example.med.jwt.JwtCookieUtil;
import com.example.med.jwt.JwtTokenProvider;
import com.example.med.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Tag(name = "User API", description = "회원 관련 API (회원가입, 로그인, 로그아웃 등)")
@RequestMapping("/api")
@RequiredArgsConstructor
@RestController
@Slf4j
public class UserController {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Operation(summary = "회원가입", description = "사용자 정보를 받아 회원가입을 처리합니다.\n\n예시 요청 JSON:\n{\n  \"userId\": \"testuser\",\n  \"password\": \"testpass\",\n  \"userName\": \"홍길동\",\n  \"email\": \"test@example.com\"\n}")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Parameter(description = "회원가입 요청 DTO", required = true) UserSignupRequestDto requestDto) {
        try {
            UserInfo user = userService.registerUser(requestDto);

            return ResponseEntity.ok(Map.of(
                    "message", "회원가입이 완료되었습니다.",
                    "userId", user.getUserId(),
                    "email", user.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "회원가입 처리 중 오류 발생: " + e.getMessage()));
        }
    }

    @Operation(summary = "로그인", description = "사용자 아이디와 비밀번호로 로그인을 처리합니다.\n\n예시 요청 JSON:\n{\n  \"userId\": \"testuser\",\n  \"password\": \"testpass\"\n}")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Parameter(description = "로그인 요청 DTO", required = true) UserLoginRequestDto requestDto, HttpServletResponse response){
        try{
            UserInfo user = userService.authenticateUser(requestDto);

            if(user == null){
                return ResponseEntity.status(401).body("로그인 실패: 사용자 정보가 일치하지 않습니다.");
            }

            // JWT 토큰 생성
            String token = jwtTokenProvider.CreateToken(user.getUserId(), user.getUserName());

            // 쿠키에 토큰 설정
            JwtCookieUtil.addJwtToCookie(response, token);

            return ResponseEntity.ok("로그인 성공");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("로그인 처리 중 오류 발생: " + e.getMessage());
        }
    }
    @Operation(summary = "로그아웃", description = "JWT 쿠키를 삭제하여 로그아웃을 처리합니다.\n\n예시: 로그인 후 받은 쿠키를 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response){
        try {
            // JWT 쿠키 삭제
            JwtCookieUtil.DeleteJwtCookie();

             return ResponseEntity.ok(Map.of(
                "message", "로그아웃이 완료되었습니다.",
                "success", true
            ));
        } catch (Exception e) {
            log.error("[로그아웃 오류] 예외: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "로그아웃 처리 중 오류가 발생했습니다.",
                "success", false
            ));
        }
    }
}
