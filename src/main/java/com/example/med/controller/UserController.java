package com.example.med.controller;

import com.example.med.dto.*;
import com.example.med.jwt.JwtCookieUtil;
import com.example.med.jwt.JwtTokenProvider;
import com.example.med.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Tag(name = "User API", description = "회원 관련 API (회원가입, 로그인, 로그아웃 등)")
@RequestMapping("/api")
@RequiredArgsConstructor
@RestController
@Slf4j
public class UserController {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieUtil jwtCookieUtil;
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
            String token = jwtTokenProvider.CreateToken(user.getUserId(), user.getUserName(), user.getRole());

            // 쿠키에 토큰 설정
            JwtCookieUtil.addJwtToCookie(response, token);

            return ResponseEntity.ok("로그인 성공");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("로그인 처리 중 오류 발생: " + e.getMessage());
        }
    }

    @Operation(summary = "로그아웃", description = "JWT 쿠키를 삭제하여 로그아웃을 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(){
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

    @Operation(summary = "사용자 정보 업데이트", description = "로그인된 사용자 정보(이름, 비밀번호)를 업데이트합니다.")
    @PostMapping("/profile/update")
    public ResponseEntity<?> UpdateProfile(
            @RequestBody @Parameter(description = "사용자 정보 업데이트 요청 DTO", required = true) UserUpdateRequestDto requestDto, HttpServletRequest request) {
        try {
            // JWT 토큰에서 사용자 ID 추출
            String userId = jwtCookieUtil.getUserIdFromJwtCookie(request);
            // 사용자 정보 업데이트
            UserInfo updatedUser = userService.updateUser(userId, requestDto);

            return ResponseEntity.ok(Map.of(
                    "message", "사용자 정보가 업데이트되었습니다.",
                    "user", updatedUser
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "사용자 정보 업데이트 중 오류 발생: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "현재 로그인된 사용자 프로필 조회",
            description = "JWT 쿠키를 통해 인증된 사용자의 프로필 정보를 조회합니다. 반환값은 userId, userName, email, createdAt, updatedAt을 포함합니다."
    )
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request){
        try{
            // 1. JWT 쿠키에서 UserId 추출
            String userId = jwtCookieUtil.getUserIdFromJwtCookie(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "message", "JWT 쿠키가 없거나 유효하지 않습니다.",
                        "success", false
                ));
            }
            // 2. DB에서 사용자 정보 조회
            UserInfoRespondDto user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "사용자 정보를 찾을 수 없습니다.",
                        "success", false
                ));
            }
            // 3. 응답 DTO로 변환
            UserInfoRespondDto respondDto = new UserInfoRespondDto();
            respondDto.setUserId(user.getUserId());
            respondDto.setUserName(user.getUserName());
            respondDto.setEmail(user.getEmail());
            respondDto.setCreatedAt(user.getCreatedAt());
            respondDto.setUpdatedAt(user.getUpdatedAt());
            return ResponseEntity.ok(respondDto);
        } catch(Exception e){
            log.error("[사용자 프로필 조회 오류] 예외: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "사용자 프로필 조회 중 오류가 발생했습니다.",
                    "success", false
            ));
        }
    }
    @GetMapping("/showAll")
    public ResponseEntity<List<UserDto>> showAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    // DELETE /api/userInfo/delete/{userId}
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Void> delete(@PathVariable String userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

}
