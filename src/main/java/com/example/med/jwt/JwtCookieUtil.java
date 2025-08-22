package com.example.med.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseCookie;
import java.time.Duration; // 시간의 양 조절

@Component
public class JwtCookieUtil {

    private static final String COOKIE_NAME = "jwt_token";
    private static final Duration COOKIE_MAX_AGE = Duration.ofHours(2);
    private final JwtTokenProvider jwtTokenProvider;

    public JwtCookieUtil(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

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
    }

    public String getUserIdFromJwtCookie(HttpServletRequest request){
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    // JwtTokenProvider를 사용해 userId 추출
                    return jwtTokenProvider.getClaim(token, "sub");
                }
            }
        }
        return null; // 쿠키가 없거나 토큰이 없는 경우
    }

}
