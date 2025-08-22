package com.example.med.jwt;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseCookie;
import java.time.Duration; // 시간의 양 조절

@Component
public class JwtCookieUtil {

    private static final String COOKIE_NAME = "jwt_token";
    private static final Duration COOKIE_MAX_AGE = Duration.ofHours(2);

    public static ResponseCookie CreateJwtCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME,token)
                .httpOnly(true) // 인증
                .secure(false)
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
    }

    public static ResponseCookie DeleteJwtCookie() {
        return ResponseCookie.from(COOKIE_NAME,"")
                .httpOnly(true) // 인증
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    public static void addJwtToCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = CreateJwtCookie(token);
        response.addHeader("Set-Cookie", cookie.toString());
    } //쿠키 헤더에 추가
}
