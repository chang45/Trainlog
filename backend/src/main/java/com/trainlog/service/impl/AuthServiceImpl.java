package com.trainlog.service.impl;

import com.trainlog.dto.LoginDTO;
import com.trainlog.dto.RegisterDTO;
import com.trainlog.entity.SysUser;
import com.trainlog.exception.BizException;
import com.trainlog.mapper.UserMapper;
import com.trainlog.service.AuthService;
import com.trainlog.service.CaptchaService;
import com.trainlog.service.LoginGuard;
import com.trainlog.utils.JwtUtil;
import com.trainlog.vo.LoginVO;
import com.trainlog.vo.RegisterConfigVO;
import com.trainlog.vo.UserVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;
    private final LoginGuard loginGuard;
    private final boolean registerEnabled;
    private final String registerInviteCode;
    private final String dummyHash;

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           CaptchaService captchaService,
                           LoginGuard loginGuard,
                           @Value("${app.register.enabled:true}") boolean registerEnabled,
                           @Value("${app.register.invite-code:}") String registerInviteCode) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.captchaService = captchaService;
        this.loginGuard = loginGuard;
        this.registerEnabled = registerEnabled;
        this.registerInviteCode = registerInviteCode;
        // 用户不存在时也跑一次 BCrypt，避免通过响应时间猜出账号是否存在
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public LoginVO login(LoginDTO dto, String ip) {
        String username = dto.username().trim();

        loginGuard.checkLoginRate(ip);
        loginGuard.assertNotLocked(username);

        if (!captchaService.verify(dto.captchaId(), dto.captchaCode())) {
            throw new BizException(400, "验证码错误或已失效，请刷新后重试");
        }

        SysUser user = userMapper.findByUsernameIgnoreCase(username).orElse(null);
        String hash = user == null ? dummyHash : user.getPasswordHash();
        if (!passwordEncoder.matches(dto.password(), hash) || user == null) {
            int remain = loginGuard.registerFailure(username);
            if (remain <= 0) {
                throw new BizException(429, "密码错误次数过多，账号已临时锁定，请稍后再试");
            }
            throw new BizException(401, "用户名或密码错误，还可尝试 " + remain + " 次");
        }

        if (!user.isEnabled()) {
            throw new BizException(403, "该账号已被禁用，请联系管理员");
        }

        loginGuard.onSuccess(username);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() + 1);
        userMapper.save(user);

        return new LoginVO(jwtUtil.createToken(user), jwtUtil.getTtlSeconds(), UserVO.from(user));
    }

    @Override
    public RegisterConfigVO registerConfig() {
        return new RegisterConfigVO(registerEnabled, inviteRequired());
    }

    @Override
    @Transactional
    public LoginVO register(RegisterDTO dto, String ip) {
        if (!registerEnabled) {
            throw new BizException(403, "当前未开放注册，请联系管理员开通账号");
        }
        loginGuard.checkRegisterRate(ip);
        loginGuard.assertRegisterQuota(ip);

        if (!captchaService.verify(dto.captchaId(), dto.captchaCode())) {
            throw new BizException(400, "验证码错误或已失效，请刷新后重试");
        }
        if (inviteRequired() && !inviteMatches(dto.inviteCode())) {
            throw new BizException(403, "邀请码不正确，请确认后再试");
        }

        String username = dto.username().trim();
        if (userMapper.findByUsernameIgnoreCase(username).isPresent()) {
            throw new BizException(409, "该用户名已被注册，请换一个");
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setNickname(dto.nickname() == null || dto.nickname().isBlank()
                ? username : dto.nickname().trim());
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setLastLoginAt(now);
        user.setLoginCount(1);
        try {
            userMapper.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BizException(409, "该用户名已被注册，请换一个");
        }
        loginGuard.recordRegistration(ip);

        return new LoginVO(jwtUtil.createToken(user), jwtUtil.getTtlSeconds(), UserVO.from(user));
    }

    @Override
    public UserVO getCurrentUser(String username) {
        SysUser user = userMapper.findByUsername(username)
                .orElseThrow(() -> new BizException(401, "账号不存在，请重新登录"));
        if (!user.isEnabled()) {
            throw new BizException(403, "该账号已被禁用");
        }
        return UserVO.from(user);
    }

    private boolean inviteRequired() {
        return registerInviteCode != null && !registerInviteCode.isBlank();
    }

    private boolean inviteMatches(String provided) {
        if (provided == null || provided.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.trim().getBytes(StandardCharsets.UTF_8),
                registerInviteCode.trim().getBytes(StandardCharsets.UTF_8));
    }
}
