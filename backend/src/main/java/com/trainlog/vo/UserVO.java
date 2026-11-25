package com.trainlog.vo;

import com.trainlog.entity.SysUser;

import java.time.LocalDateTime;

public record UserVO(Long id, String username, String nickname,
                     LocalDateTime lastLoginAt, int loginCount) {

    public static UserVO from(SysUser user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getLastLoginAt(), user.getLoginCount());
    }
}
