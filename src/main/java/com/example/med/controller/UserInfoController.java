package com.example.med.controller;

import com.example.med.dto.UserLoginRequestDto;
import com.example.med.dto.UserSignupRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserInfoController {

    private final JdbcTemplate jdbcTemplate;

    // highlight-start
    // secondDataSource를 명시적으로 주입받도록 수정
    @Autowired
    public UserInfoController(@Qualifier("secondDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    // highlight-end

    @Operation(summary = "DB 연결 확인", description = "SELECT 1 FROM dual 실행")
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Integer v = jdbcTemplate.queryForObject("SELECT 1 FROM dual", Integer.class);
        return Map.of("status", "OK", "result", v);
    }

    @Operation(summary = "사용자 단건 조회", description = "USER_INFO.user_id로 사용자 조회")
    @GetMapping("/{userId}")
    public Map<String, Object> getOne(@PathVariable String userId) {
        return jdbcTemplate.queryForMap(
                "SELECT user_id, user_name, email, created_at, updated_at FROM USER_INFO WHERE user_id = ?",
                userId
        );
    }

    @Operation(summary = "회원가입", description = "사용자 정보를 받아 회원가입을 처리합니다.")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserSignupRequestDto requestDto) {
        // Check if user_id already exists
        String checkUserSql = "SELECT COUNT(*) FROM USER_INFO WHERE user_id = ?";
        int userCount = jdbcTemplate.queryForObject(checkUserSql, Integer.class, requestDto.getUserId());
        if (userCount > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User ID already exists");
        }

        // Check if email already exists
        String checkEmailSql = "SELECT COUNT(*) FROM USER_INFO WHERE email = ?";
        int emailCount = jdbcTemplate.queryForObject(checkEmailSql, Integer.class, requestDto.getEmail());
        if (emailCount > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }

        // Insert new user, letting the DB handle created_at and updated_at with default SYSTIMESTAMP
        String sql = "INSERT INTO USER_INFO (user_id, password, user_name, email) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, requestDto.getUserId(), requestDto.getPassword(), requestDto.getUserName(), requestDto.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @Operation(summary = "로그인", description = "사용자 아이디와 비밀번호로 로그인을 처리합니다.")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginRequestDto requestDto) {
        String sql = "SELECT password FROM USER_INFO WHERE user_id = ?";
        try {
            String storedPassword = jdbcTemplate.queryForObject(sql, String.class, requestDto.getUserId());
            if (requestDto.getPassword().equals(storedPassword)) {
                // In a real application, you would return a JWT token here.
                return ResponseEntity.ok("Login successful");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @Operation(summary = "회원탈퇴", description = "사용자 아이디를 받아 회원탈퇴를 처리합니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId) {
        String sql = "DELETE FROM USER_INFO WHERE user_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId);
        if (rowsAffected > 0) {
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
}