package com.example.med.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 스프링의 설정 클래스임을 명시
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // "/**"는 모든 경로에 대해 CORS 설정을 적용한다는 의미입니다.
                .allowedOrigins("http://localhost:5173") // 자원 공유를 허락할 출처(origin)를 지정합니다. React 앱의 주소입니다.
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 허용할 HTTP 메서드를 지정합니다.
                .allowedHeaders("*") // 모든 HTTP 헤더를 허용합니다.
                .allowCredentials(true) // 쿠키나 인증 정보를 함께 보낼 수 있도록 허용합니다.
                .maxAge(3600); // 브라우저가 이 CORS 설정을 캐시할 시간을 초(second) 단위로 설정합니다.
    }
}