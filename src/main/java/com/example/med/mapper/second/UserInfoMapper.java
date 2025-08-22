package com.example.med.mapper.second;

import com.example.med.dto.UserInfo;
import org.apache.ibatis.annotations.Param;

public interface UserInfoMapper {
    UserInfo findById(@Param("userId") String userId);
    int insert(UserInfo user);

    // 회원가입을 위한 중복 검사 메서드들
    int countByUserId(@Param("userId") String userId);
    int countByEmail(@Param("email") String email);
}
