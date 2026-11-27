package org.examplexxx.trainlog.service.Impl;

import jakarta.annotation.PostConstruct;
import org.examplexxx.trainlog.constant.JwtClaimsConstant;
import org.examplexxx.trainlog.context.BaseContext;
import org.examplexxx.trainlog.dto.LoginDTO;
import org.examplexxx.trainlog.dto.ProfileDTO;
import org.examplexxx.trainlog.entity.SysUser;
import org.examplexxx.trainlog.entity.UserNutritionTarget;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.mapper.AuthMapper;
import org.examplexxx.trainlog.mapper.UserNutritionTargetMapper;
import org.examplexxx.trainlog.properties.JwtProperties;
import org.examplexxx.trainlog.service.AuthService;
import org.examplexxx.trainlog.service.CaptchaService;
import org.examplexxx.trainlog.utils.JwtUtil;
import org.examplexxx.trainlog.vo.LoginVO;
import org.examplexxx.trainlog.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthMapper authMapper;
    @Autowired
    private UserNutritionTargetMapper nutritionTargetMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 用户不存在时也跑一次哈希，避免攻击者通过响应时间判断账号是否存在。
     */
    private String dummyHash;

    @PostConstruct
    private void initDummyHash() {
        // 注意：字段注入发生在对象构造之后，所以 dummyHash 不能写成字段初始化表达式
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 参数检查（项目没引 spring-boot-starter-validation，这里手写）
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new BizException(400, "请输入用户名");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BizException(400, "请输入密码");
        }

        // 2. 验证码校验（verify 内部会删除记录，一次性，防重放）
        if (!captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
            throw new BizException(400, "验证码错误或已失效，请刷新后重试");
        }

        // 3. 查用户
        String username = dto.getUsername().trim();
        SysUser user = authMapper.findByUsername(username);

        // 4. 密码比对：Argon2id 在这里生效；用户不存在也要比对一次 dummyHash
        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean matched = passwordEncoder.matches(dto.getPassword(), hash);
        if (!matched || user == null) {
            // 不区分"用户不存在"和"密码错误"，防止用户名枚举
            throw new BizException(401, "用户名或密码错误");
        }

        // 5. 账号是否启用（enabled 是 Integer，1 表示启用）
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new BizException(403, "该账号已被禁用");
        }

        // 6. 更新登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() == null ? 1 : user.getLoginCount() + 1);
        authMapper.updateLoginInfo(user);

        // 7. 签发 JWT
        return issueToken(user);
    }

    @Override
    public LoginVO register(LoginDTO dto) {
        // 1. 参数检查
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new BizException(400, "请输入用户名");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BizException(400, "请输入密码");
        }

        // 2. 验证码校验（一次性，注册会把这张验证码消费掉）
        if (!captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
            throw new BizException(400, "验证码错误或已失效，请刷新后重试");
        }

        // 3. 先查重（并发情况下靠唯一索引兜底）
        String username = dto.getUsername().trim();
        if (authMapper.findByUsername(username) != null) {
            throw new BizException(409, "该用户名已被注册");
        }

        // 4. 插入用户（@Options 会把自增 id 回填到 user.id，签 token 依赖它）
        String nickname = dto.getNickname() == null || dto.getNickname().isBlank()
                ? username : dto.getNickname().trim();
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(nickname);
        user.setEnabled(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        user.setLoginCount(1);   // 注册即登录，算第一次登录
        try {
            authMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 两个请求同时注册同一个用户名时，唯一索引会拒绝后来的那个
            throw new BizException(409, "该用户名已被注册");
        }

        // 5. 注册成功直接签发 token，前端不用再调 /auth/login
        return issueToken(user);
    }

    /**
     * 登录和注册共用：根据用户签发 jwt，组装成前端要的返回结构。
     */
    private LoginVO issueToken(SysUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        String token = JwtUtil.createJWT(jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(), claims);
        return new LoginVO(token, jwtProperties.getAdminTokenName(),
                jwtProperties.getAdminTtl() / 1000, toUserVO(user));
    }

    /** 用户 + 营养目标拼成前端需要的结构 */
    private UserVO toUserVO(SysUser user) {
        UserNutritionTarget target = nutritionTargetMapper.findByUserId(user.getId());
        return UserVO.from(user, target);
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        SysUser user = authMapper.findById(userId);
        if (user == null) {
            throw new BizException(401, "账号不存在，请重新登录");
        }
        return toUserVO(user);
    }

    @Override
    public UserVO updateProfile(ProfileDTO dto) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        if (dto == null || dto.getHeightCm() == null || dto.getWeightKg() == null) {
            throw new BizException(400, "请填写身高和体重");
        }
        BigDecimal height = dto.getHeightCm();
        BigDecimal weight = dto.getWeightKg();
        if (height.compareTo(BigDecimal.valueOf(50)) < 0
                || height.compareTo(BigDecimal.valueOf(250)) > 0) {
            throw new BizException(400, "身高需在 50-250 厘米之间");
        }
        if (weight.compareTo(BigDecimal.valueOf(20)) < 0
                || weight.compareTo(BigDecimal.valueOf(300)) > 0) {
            throw new BizException(400, "体重需在 20-300 公斤之间");
        }
        Integer age = dto.getAge();
        if (age == null || age < 10 || age > 100) {
            throw new BizException(400, "请填写年龄（10-100 岁）");
        }
        String gender = requireCode(dto.getGender(), "性别", Set.of("MALE", "FEMALE"));

        SysUser user = authMapper.findById(userId);
        if (user == null) {
            throw new BizException(401, "账号不存在，请重新登录");
        }
        user.setHeightCm(height.setScale(2, RoundingMode.HALF_UP));
        user.setWeightKg(weight.setScale(2, RoundingMode.HALF_UP));
        user.setAge(age);
        user.setGender(gender);
        authMapper.updateProfile(user);

        int dailyCalories = calculateDailyCalories(height, weight, age, gender);
        int proteinNormal = (int) Math.round(weight.doubleValue() * 0.9);
        int proteinBulk = (int) Math.round(weight.doubleValue() * 1.8);
        int proteinCut = (int) Math.round(weight.doubleValue() * 2.0);
        proteinNormal = Math.max(30, Math.min(proteinNormal, 400));
        proteinBulk = Math.max(proteinNormal, Math.min(proteinBulk, 400));
        proteinCut = Math.max(proteinNormal, Math.min(proteinCut, 400));

        LocalDateTime now = LocalDateTime.now();
        UserNutritionTarget target = new UserNutritionTarget();
        target.setUserId(userId);
        target.setDailyCalories(dailyCalories);
        target.setProteinNormal(proteinNormal);
        target.setProteinBulk(proteinBulk);
        target.setProteinCut(proteinCut);
        target.setCreatedAt(now);
        target.setUpdatedAt(now);
        nutritionTargetMapper.upsert(target);

        return toUserVO(user);
    }

    /** 保存资料时顺带算好每日总消耗（Mifflin-St Jeor × 轻度活动） */
    private int calculateDailyCalories(BigDecimal height, BigDecimal weight, int age, String gender) {
        double bmr = 10 * weight.doubleValue() + 6.25 * height.doubleValue() - 5 * age
                + ("FEMALE".equals(gender) ? -161 : 5);
        int daily = (int) Math.round(bmr * 1.375);
        return Math.max(800, Math.min(daily, 10000));
    }

    private String requireCode(String value, String label, Set<String> allowed) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, "请选择" + label);
        }
        String code = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(code)) {
            throw new BizException(400, label + "取值不合法");
        }
        return code;
    }
}
