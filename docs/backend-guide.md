# 后端分层与练习指南

后端按「苍穹外卖」常见的分层方式组织在一个 Spring Boot 模块里，目录和职责如下。

```text
com.trainlog
├── controller      接收请求、参数校验、返回 Result
├── service         业务接口
│   └── impl        业务实现
├── mapper          数据访问（当前是 Spring Data JPA）
├── entity          数据库实体
├── dto             前端传入的参数
├── vo              返回给前端的对象
├── config          密码编码器、MVC、初始化数据
├── interceptor     JWT 登录拦截
├── context         BaseContext，保存当前请求的用户
├── utils           JWT、验证码、IP 工具
├── constant        训练类型、JWT 常量
├── exception       BizException
├── handler         全局异常处理
├── properties      application.yml 的配置映射
└── result          Result、PageResult
```

## 一次请求怎么走

以新增训练记录为例：

```text
POST /api/training/records
  -> JwtTokenInterceptor 校验 token，把 userId 放进 BaseContext
  -> TrainingController 接收 TrainingRecordDTO
  -> TrainingService（接口）
  -> TrainingServiceImpl 校验类型、日期，组装 TrainingRecord
  -> TrainingRecordMapper 保存
  -> 返回 TrainingRecordVO，再包成 Result
```

这样 Controller 不碰数据库，ServiceImpl 不碰 HTTP，Mapper 只负责持久化。

## 各层怎么写

### Controller

- 使用 `@RestController` 和 `@RequestMapping`
- 参数用 `@Valid` 触发 DTO 上的校验
- 返回 `Result.success(...)` 或 `Result.error(...)`
- 当前用户从 `BaseContext.getCurrentId()` 取，不在 Controller 里解析 token

### Service

- 先写接口，例如 `TrainingService`
- 实现放在 `service/impl`，类名以 `ServiceImpl` 结尾
- 业务规则放在这里：日期不能晚于今天、记录只能操作自己的、统计口径等
- 需要事务的方法加 `@Transactional`

### Mapper

- 当前使用 Spring Data JPA，继承 `JpaRepository` 即可获得增删改查
- 复杂查询可以用 `JpaSpecificationExecutor` 或 `@Query`
- 想练 MyBatis 时，把 `UserMapper`、`TrainingRecordMapper` 换成 MyBatis 接口，并在 `resources/mapper` 下写 XML

### DTO / VO / Entity

- DTO 只放请求字段和校验注解，例如 `TrainingRecordDTO`
- VO 是对外返回结构，例如 `TrainingRecordVO`，不要把 Entity 直接返回
- Entity 与表结构一一对应，字段变化时同步考虑数据库迁移

## 新增一个业务模块的步骤

下面以「训练计划」为例：

1. 在 `entity` 建 `TrainingPlan`
2. 在 `mapper` 建 `TrainingPlanMapper`
3. 在 `dto` 建 `TrainingPlanDTO`，在 `vo` 建 `TrainingPlanVO`
4. 在 `service` 建接口，在 `service/impl` 写实现
5. 在 `controller` 建 `TrainingPlanController`
6. 在 `WebMvcConfig` 的拦截器规则里确认新接口需要登录
7. 用 `scripts/smoke-test.ps1` 或接口工具补一条测试

## 可以拿来练手的方向

- 给训练记录加「标签」：多对多表、DTO 增加标签、查询按标签筛选
- 做「个人纪录」：按动作统计最大重量、最快配速、最长距离
- 做「训练计划」：计划表 + 计划动作表，支持生成当天的训练
- 把统计接口拆成周报 / 月报，并加缓存
- 给训练记录加图片或附件
- 把 `LoginGuard` 的本地限流换成 Redis
- 给 Service 写单元测试，给 Mapper 写集成测试
- 把当前 JPA Mapper 替换成 MyBatis + XML，练习动态 SQL

## 当前训练模块的改写建议

现在的训练模块是完整可运行的参考实现。想自己练，可以按下面的顺序重写：

1. 先保留 `TrainingController` 和 `TrainingRecordDTO` 的接口契约
2. 清空 `TrainingServiceImpl` 的方法体，按自己的思路实现分页、新增、更新、删除
3. 再实现 `stats`：按日期聚合、按类型聚合、连续打卡
4. 最后调整 `TrainingRecordMapper` 的查询方式

接口路径和返回字段保持不变，前端的概览、记录、统计三个页面就能继续使用。

## 调试

- 启动后端：`powershell -ExecutionPolicy Bypass -File scripts/start-backend.ps1`
- H2 控制台：<http://localhost:8081/h2-console>，JDBC URL 是 `jdbc:h2:file:./db/trainlog`
- 演示账号：admin / 123456
- 后端日志：`backend/app.log`
- 接口测试：后端以 local 配置启动后运行 `scripts/smoke-test.ps1`

## 容易踩的坑

- 训练记录必须带 `userId` 条件，不能只按 id 查询，否则会读到别人的数据
- 统计接口的 `days` 要限制范围，避免一次查出过多数据
- 日期参数用 `LocalDate`，前端传 `yyyy-MM-dd`；时间戳和时区不要混用
- 新增字段后同步更新 DTO、VO、Entity 和 MySQL 建表脚本
- 密码只存 BCrypt 哈希，任何接口都不要返回 `passwordHash`
