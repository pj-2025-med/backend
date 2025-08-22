package com.example.med.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKeyEncoded;

    @Value("${jwt.access-token-expiration-millis}")
    @Getter
    private long accessTokenExpirationMillis;

    @Value("${jwt.refresh-token-expiration-millis}")
    @Getter
    private long refreshTokenExpirationMillis;

    private Key key;

    @PostConstruct
    protected void init(){
        this.key = Keys.hmacShaKeyFor(secretKeyEncoded.getBytes());
    }

    /**
     * JWT 토큰 생성
     * @param userId 사용자 ID
     * @param name 사용자 이름
     * @param role 사용자 역할
     */
    public String CreateToken(String userId, String name, String role){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpirationMillis);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        claims.put("name", name);
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    /**
     * JWT 토큰 검증
     * @param token JWT 토큰
     * @return 유효한 경우 true, 그렇지 않으면 false
     */
    public boolean ValidateToken(String token){
        try{
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch ( JwtException | IllegalArgumentException e) {
            log.info("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JWT 토큰에서 원하는 클레임 추출 (userId, name, role)
     */
    public String getClaim(String token, String claimName) {
        Claims claims = ExtractAllClaims(token);
        return claims.get(claimName, String.class);
    }

    private Claims ExtractAllClaims(String token){
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token");
        }
    }

}
