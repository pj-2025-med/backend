package com.example.med.service;

import com.example.med.dto.*;
import com.example.med.mapper.second.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserInfoMapper userInfoMapper;
    private final PasswordEncoder passwordEncoder;

    public UserInfo authenticateUser(UserLoginRequestDto requestDto) {
        // 사용자 ID로 사용자 정보 조회
        UserInfo user = userInfoMapper.findById(requestDto.getUserId());

        if (user == null) {
            log.warn("[사용자 로그인 실패] 사용자를 찾을 수 없음. userId: {}", requestDto.getUserId());
            return null;
        }
        // 비밀번호 검증
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            log.warn("[사용자 로그인 실패] 비밀번호 불일치. userId: {}", requestDto.getPassword());
            return null;
        }

        log.info("[사용자 로그인 성공] userId: {}", requestDto.getUserId());

        // role  할당
        if ("admin".equals(user.getUserId())) {
            user.setRole("ADMIN");
        } else {
            user.setRole("USER");
        }
        return user;
    }

    @Transactional
    public UserInfo registerUser(UserSignupRequestDto requestDto) {
        // 아이디 중복 검사
        if (userInfoMapper.countByUserId(requestDto.getUserId()) > 0) {
            log.warn("[사용자 회원가입 실패] 이미 존재하는 아이디: {}", requestDto.getUserId());
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 이메일 중복 검사
        if (userInfoMapper.countByEmail(requestDto.getEmail()) > 0) {
            log.warn("[사용자 회원가입 실패] 이미 존재하는 이메일: {}", requestDto.getEmail());
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // DTO → UserInfo 변환
        UserInfo user = UserInfo.builder()
                .userId(requestDto.getUserId())
                .password(encodedPassword)
                .userName(requestDto.getUserName())
                .email(requestDto.getEmail())
                .build();

        // DB에 저장
        userInfoMapper.insert(user);

        log.info("[사용자 회원가입 성공] userId: {}", user.getUserId());
        return user;
    }

    @Transactional
    public UserInfo updateUser(String userId, UserUpdateRequestDto requestDto){

        UserInfo existingUser = userInfoMapper.findById(userId);
        if (existingUser == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 빈 값이면 기존 값 사용
        String newUserName = (requestDto.getUserName() == null || requestDto.getUserName().isEmpty())
                ? existingUser.getUserName() : requestDto.getUserName();

        String newEmail = (requestDto.getEmail() == null || requestDto.getEmail().isEmpty())
                ? existingUser.getEmail() : requestDto.getEmail();

        String newPassword = (requestDto.getPassword() == null || requestDto.getPassword().isEmpty())
                ? existingUser.getPassword() : passwordEncoder.encode(requestDto.getPassword());

        UserInfo user = UserInfo.builder()
                .userId(userId)
                .password(newPassword)
                .userName(newUserName)
                .email(newEmail)
                .updatedAt(java.time.OffsetDateTime.now())
                .build();

        userInfoMapper.updateUserInfo(user);
        log.info("[사용자 정보 업데이트 성공] userId: {}", user.getUserId());
        return user;
    }

    public UserInfoRespondDto getUserById(String userId){
        return userInfoMapper.findByUserInfo(userId);
    }


    public List<UserDto> findAll() {
        return userInfoMapper.findAll();
    }

    public void delete(String userId) {
        int deleted = userInfoMapper.deleteUser(userId);
        if (deleted == 0) {
            throw new IllegalArgumentException("존재하지 않는 사용자: " + userId);
        }
    }

}
