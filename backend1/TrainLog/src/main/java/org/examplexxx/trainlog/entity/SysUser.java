package org.examplexxx.trainlog.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应数据库表 sys_user
 */
@Data
public class SysUser {

    /** 主键 */
    private Long id;

    /** 登录名（唯一） */
    private String username;

    /** 密码（Argon2id 密文，对应数据库字段 password_hash） */
    private String passwordHash;

    /** 昵称 */
    private String nickname;

    /** 是否启用：1-启用，0-禁用 */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 累计登录次数 */
    private Integer loginCount;
}
