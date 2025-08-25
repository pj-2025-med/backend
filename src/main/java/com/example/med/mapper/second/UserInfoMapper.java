package com.example.med.mapper.second;

import com.example.med.dto.UserInfo;
import com.example.med.dto.UserInfoRespondDto;
import org.apache.ibatis.annotations.Param;

public interface UserInfoMapper {
    UserInfo findById(@Param("userId") String userId);
    int insert(UserInfo user);

    // 회원가입을 위한 중복 검사 메서드들
    int countByUserId(@Param("userId") String userId);
    int countByEmail(@Param("email") String email);

    int updateUserInfo(UserInfo user);

    // 사용자 정보 표시를 위한 메서드
    UserInfoRespondDto findByUserInfo(@Param("userId") String userId);

}
