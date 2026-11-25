package com.trainlog.mapper;

import com.trainlog.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户数据访问。当前使用 Spring Data JPA；若改成 MyBatis，只需替换本接口与实现方式。
 */
public interface UserMapper extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    Optional<SysUser> findByUsernameIgnoreCase(String username);
}
