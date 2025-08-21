package com.example.med.mapper;

import com.example.med.dto.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserInfoMapper {
    UserInfo findById(@Param("userId") String userId);
    int insert(UserInfo user);
}