package org.examplexxx.trainlog.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.examplexxx.trainlog.entity.SysUser;

public interface AuthMapper {

    /**
     * 按用户名查用户。application.yml 里开了 map-underscore-to-camel-case，
     * 所以 password_hash 会自动映射到 passwordHash。
     */
    @Select("SELECT id, username, password_hash, nickname, height_cm, weight_kg, age, gender, "
            + "daily_calories, enabled, created_at, last_login_at, login_count "
            + "FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser findByUsername(String username);

    @Select("SELECT id, username, password_hash, nickname, height_cm, weight_kg, age, gender, "
            + "daily_calories, enabled, created_at, last_login_at, login_count "
            + "FROM sys_user WHERE id = #{id}")
    SysUser findById(Long id);

    /**
     * 登录成功后更新最后登录时间和累计登录次数。
     */
    @Update("UPDATE sys_user SET last_login_at = #{lastLoginAt}, login_count = #{loginCount} WHERE id = #{id}")
    int updateLoginInfo(SysUser user);

    /**
     * 保存个人资料。
     */
    @Update("UPDATE sys_user SET height_cm = #{heightCm}, weight_kg = #{weightKg}, "
            + "age = #{age}, gender = #{gender}, daily_calories = #{dailyCalories} "
            + "WHERE id = #{id}")
    int updateProfile(SysUser user);

    /**
     * 插入用户，供初始化演示账号使用。
     */
    @Insert("INSERT INTO sys_user (username, password_hash, nickname, enabled, created_at, last_login_at, login_count) "
            + "VALUES (#{username}, #{passwordHash}, #{nickname}, #{enabled}, #{createdAt}, #{lastLoginAt}, #{loginCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);
}
