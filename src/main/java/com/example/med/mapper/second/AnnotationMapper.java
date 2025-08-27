package com.example.med.mapper.second;

import com.example.med.dto.AnnotationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnotationMapper {
    List<AnnotationDto> findAnnotations(@Param("studyKey") long studyKey, 
                                        @Param("seriesKey") long seriesKey, 
                                        @Param("imageKey") long imageKey, 
                                        @Param("frameNo") Integer frameNo);

    void upsertAnnotations(AnnotationDto annotationDto);
}
