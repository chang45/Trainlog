package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.examplexxx.trainlog.entity.SysUser;

import java.time.LocalDateTime;

/**
 * 返回给前端的用户信息，注意不能包含 passwordHash。
 */
@Data
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private LocalDateTime lastLoginAt;
    private Integer loginCount;

    public static UserVO from(SysUser user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getLastLoginAt(), user.getLoginCount());
    }
}
