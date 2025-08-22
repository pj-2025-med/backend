package com.example.med.mapper.second;

import com.example.med.dto.UserInfo;
import org.apache.ibatis.annotations.Param;

public interface UserInfoMapper {
    UserInfo findById(@Param("userId") String userId);
    int insert(UserInfo user);
}