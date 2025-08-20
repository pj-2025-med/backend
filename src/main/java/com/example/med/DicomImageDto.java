package com.example.med;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DicomImageDto {
    private Long imageKey;
    private String sopInstanceUid;
    private Integer instanceNumber; // 없으면 null
    private String httpUrl;         // 프론트에서 wadouri: 붙여 사용
}