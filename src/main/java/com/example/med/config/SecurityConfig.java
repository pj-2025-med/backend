package com.example.med.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile; // Profile 임포트
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // highlight-start
    /**
     * 개발 환경용 SecurityFilterChain (dev 프로필 활성화 시 사용)
     * 모든 요청을 허용하여 개발 및 테스트를 쉽게 합니다.
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/**").permitAll() // 모든 경로 허용
                );
        return http.build();
    }

    /**
     * 운영 환경용 SecurityFilterChain (dev 프로필이 아닐 때 사용)
     * API 접근에 인증이 필요한 기본 보안 설정을 유지합니다.
     */
    @Bean
    @Profile("!dev")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                // -- Swagger UI & API Docs
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",

                                // -- DB Ping
                                "/db/ping/**",

                                // -- 회원가입/로그인 등 공개 API
                                "/api/users/signup",
                                "/api/users/login"
                        ).permitAll()
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                );
        return http.build();
    }
    // highlight-end
}