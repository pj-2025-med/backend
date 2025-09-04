package com.example.med.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JasyptConfig {

    public static final String JASYPT_STRING_ENCRYPTOR = "jasyptStringEncryptor";
    // 더 간단한 알고리즘으로 변경
    private static final String ALGORITHM = "PBEWithMD5AndDES";

    @Value("${jasypt.encryptor.password}")
    private String encryptKey;

    @Bean(JASYPT_STRING_ENCRYPTOR)
    public StringEncryptor stringEncryptor() {
        System.out.println("[JasyptConfig] encryptKey 값: " + (encryptKey != null ? "설정됨 (길이: " + encryptKey.length() + ")" : "null"));

        // 임시 테스트: 하드코딩된 키 사용
        String testKey = encryptKey != null ? encryptKey : "TESTKEY12345678901234567890";
        System.out.println("[JasyptConfig] 실제 사용할 키: " + testKey);

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
            System.out.println("[JasyptConfig] 암호화 테스트 성공 - 원본: " + testText + ", 암호화: " + encrypted + ", 복호화: " + decrypted);

            if (encrypted == null || decrypted == null || !testText.equals(decrypted)) {
                throw new RuntimeException("암호화/복호화 결과가 올바르지 않습니다.");
            }
        } catch (Exception e) {
            System.err.println("[JasyptConfig] 암호화 테스트 실패: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("StringEncryptor 초기화 실패", e);
        }

        System.out.println("[JasyptConfig] StringEncryptor 빈 생성 완료");
        return encryptor;
    }

}
