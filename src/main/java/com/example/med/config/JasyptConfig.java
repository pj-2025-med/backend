package com.example.med.config;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class JasyptConfig {

    public static final String JASYPT_STRING_ENCRYPTOR = "jasyptStringEncryptor";
    // 더 간단한 알고리즘으로 변경
    private static final String ALGORITHM = "PBEWithMD5AndDES";

    @Value("${jasypt.encryptor.password}")
    private String encryptKey;

    @Bean(JASYPT_STRING_ENCRYPTOR)
    public StringEncryptor stringEncryptor() {
        log.info("[JasyptConfig] encryptKey 값: {}", encryptKey != null ? "설정됨 " : "null");

        // 암호화 테스트
        String testKey = encryptKey != null ? encryptKey : "TESTKEY12345678901234567890";

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(testKey);
        config.setAlgorithm(ALGORITHM);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);

        // 암호화 테스트
        try {
            String testText = "hello";
            String encrypted = encryptor.encrypt(testText);
            String decrypted = encryptor.decrypt(encrypted);

            log.info("[JasyptConfig] 암호화 테스트 성공");

            if (encrypted == null || decrypted == null || !testText.equals(decrypted)) {
                throw new RuntimeException("암호화/복호화 결과가 올바르지 않습니다.");
            }
        } catch (Exception e) {
            log.error("[JasyptConfig] 암호화 테스트 실패:{}",e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("StringEncryptor 초기화 실패", e);
        }

        log.info("[JasyptConfig] StringEncryptor 빈 생성 완료");
        return encryptor;
    }

}
