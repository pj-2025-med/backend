package com.example.med.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component @Getter
public class PacsStorageProps {
    @Value("${pacs.image.base-path}")
    private String basePath; // 예: \\WW210.94.241.9\STS\
}
