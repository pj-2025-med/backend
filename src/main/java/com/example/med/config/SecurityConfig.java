package com.example.med.config;

import com.example.med.jwt.JwtAuthenticationFilter;
import com.example.med.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // 전역 CORS 설정을 사용하도록 명시
                .httpBasic(httpBasic -> httpBasic.disable()) // http basic auth 비활성화
                .csrf(csrf -> csrf.disable()) // csrf 비활성화
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 STATELESS 설정
                )
                .authorizeHttpRequests(authorize -> authorize
                        // -- CORS Preflight 요청은 항상 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                // -- Swagger UI & API Docs
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",

                                // -- DB Ping
                                "/db/ping/**",

                                // -- 회원가입/로그인 등 공개 API
                                "/api/register",
                                "/api/login"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/v1/dicom/studies/*/series/*/images/*").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/v1/dicom/studies/*/series/*/images/*").permitAll()
                        // -- 로그 조회 API는 ADMIN 역할만 접근 가능
                        .requestMatchers(HttpMethod.GET,  "/api/v1/dicom/studies/*/series/*/images/*").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/v1/dicom/studies/*/series/*/images/*").permitAll()
                        .requestMatchers("/api/v1/logs/**").hasRole("ADMIN")
                        // -- 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
