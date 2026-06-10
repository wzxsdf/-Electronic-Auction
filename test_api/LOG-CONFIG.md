### ================================================================================
### 📝 日志优化配置
### ================================================================================
### 说明：优化后的日志配置，减少不必要的DEBUG日志
### 目标：只显示重要的业务信息和错误日志
### ================================================================================

### 原始问题：
### ❌ 大量DEBUG日志扰乱控制台输出
### ❌ SQL查询详细信息过多
### ❌ 定时任务每次都输出DEBUG日志
### ❌ JWT认证过程过于详细

### 优化方案：
### ✅ 调整全局日志级别为INFO
### ✅ 关闭MyBatis SQL详细日志
### ✅ 定时任务只在有操作时输出日志
### ✅ 认证过滤只输出错误信息
### ✅ 保留重要的业务操作日志

### ================================================================================
### 📊 日志级别说明
### ================================================================================

### ERROR - 错误信息（必须关注）
### WARN  - 警告信息（需要注意）
### INFO  - 重要信息（正常关注）
### DEBUG - 调试信息（开发阶段）
### TRACE - 追踪信息（问题排查）

### 我们的策略：
### - 生产环境：ERROR + WARN
### - 测试环境：ERROR + WARN + INFO
### - 开发环境：ERROR + WARN + INFO + 部分DEBUG

### ================================================================================
### 🔧 优化措施
### ================================================================================

### 1. 全局日志配置（application.yml）
logging:
  level:
    root: INFO                          # 根日志级别
    com.auction: INFO                   # 项目日志级别
    com.baomidou.mybatisplus: WARN       # MyBatis Plus警告级别
    com.alibaba.druid: WARN             # Druid连接池警告级别
    org.springframework: WARN           # Spring框架警告级别
    org.springframework.web: INFO        # Spring Web信息级别
    org.springframework.security: WARN  # Spring Security警告级别
    org.springframework.scheduling: INFO # Spring调度信息级别

### 2. MyBatis SQL日志
### 原配置：log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
### 新配置：log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
### 效果：只在INFO级别以上输出SQL，不再显示详细参数绑定

### 3. 定时任务日志优化
### 移除了频繁的DEBUG日志，如：
### - "开始执行竞拍结算定时任务"
### - "开始检查竞拍自动延期"
### - "JWT认证成功：userId=xxx"
### 只在有实际业务操作时输出日志

### 4. 业务日志保留
### 保留了重要的业务操作日志：
### - ✅ "用户登录成功"
### - ✅ "商品创建成功"
### - ✅ "竞拍结算完成，结算数量: 3"
### - ✅ "竞拍延期检查完成，延长数量: 1"

### ================================================================================
### 📈 优化效果对比
### ================================================================================

### 优化前（每次定时任务执行）：
2026-06-07 23:02:40.588 [scheduling-1] DEBUG c.a.s.scheduler.AuctionScheduler - 开始执行竞拍延期检查定时任务
2026-06-07 23:02:40.599 [scheduling-1] DEBUG c.a.s.settlement.AuctionDelayService - 开始检查竞拍自动延期
2026-06-07 23:02:40.894 [scheduling-1] DEBUG c.a.s.settlement.AuctionDelayService - 无需要延期的竞拍
2026-06-07 23:02:42.578 [scheduling-1] DEBUG c.a.s.scheduler.AuctionScheduler - 开始执行竞拍结算定时任务
2026-06-07 23:02:42.585 [scheduling-1] INFO  c.a.s.s.AuctionSettlementService - 无到期竞拍需要结算

### 优化后（只显示重要信息）：
2026-06-07 23:02:42.585 [scheduling-1] INFO  c.a.s.s.AuctionSettlementService - 无到期竞拍需要结算

### 当有实际业务操作时：
2026-06-07 23:15:30.124 [scheduling-1] INFO  c.a.s.scheduler.AuctionScheduler - 竞拍结算定时任务执行完成，结算数量: 3
2026-06-07 23:15:35.456 [scheduling-1] INFO  c.a.s.scheduler.AuctionScheduler - 竞拍延期检查定时任务执行完成，延长数量: 1

### ================================================================================
### 🎯 不同环境的日志配置
### ================================================================================

### 开发环境（application-dev.yml）：
logging:
  level:
    com.auction: DEBUG              # 开发时可以看到详细调试信息
    com.baomidou.mybatisplus: DEBUG  # 开发时可以看到SQL执行

### 测试环境（application-test.yml）：
logging:
  level:
    com.auction: INFO               # 测试环境只看重要信息
    com.baomidou.mybatisplus: WARN   # 关闭SQL详细日志

### 生产环境（application-prod.yml）：
logging:
  level:
    com.auction: WARN               # 生产环境只看警告和错误
    com.baomidou.mybatisplus: ERROR  # 生产环境只看错误

### ================================================================================
### 📝 日志输出格式
### ================================================================================

### 当前格式：
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

### 格式说明：
### - %d{yyyy-MM-dd HH:mm:ss.SSS} - 时间戳（精确到毫秒）
### - [%thread] - 线程名称
### - %-5level - 日志级别（左对齐5个字符）
### - %logger{36} - 日志记录器名称（最长36个字符）
### - %msg - 日志消息
### - %n - 换行符

### 示例输出：
2026-06-07 23:15:30.124 [http-nio-8080-exec-1] INFO  c.a.api.controller.AuthController - 用户登录成功: userId=123

### ================================================================================
### 🛠️ 高级日志配置（可选）
### ================================================================================

### 如果需要更详细的控制，可以创建logback-spring.xml：

### 控制台输出（彩色支持）
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %highlight(%-5level) %cyan(%logger{36}) - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>

### 文件输出（按天滚动）
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/auction-system.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/auction-system.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>

### ================================================================================
### 💡 日志最佳实践
### ================================================================================

### 1. 选择合适的日志级别
### - ERROR：系统错误、异常情况
### - WARN：潜在问题、业务异常
### - INFO：重要业务操作、状态变更
### - DEBUG：开发调试信息（生产环境关闭）
### - TRACE：详细追踪信息（仅在特殊情况下使用）

### 2. 日志内容要简洁明了
### ✅ 好的日志："用户登录成功: userId=123, username=test"
### ❌ 差的日志："用户登录了"（缺少关键信息）

### 3. 避免频繁日志
### ✅ 好的做法：只在有业务操作时输出
### ❌ 差的做法：定时任务每次都输出DEBUG日志

### 4. 结构化日志
### 推荐使用JSON格式日志，便于解析和分析：
{
  "timestamp": "2026-06-07T23:15:30.124",
  "level": "INFO",
  "logger": "com.auction.api.controller.AuthController",
  "message": "用户登录成功",
  "userId": 123,
  "username": "test"
}

### ================================================================================
### 🎛️ 动态调整日志级别
### ================================================================================

### 如果运行时需要临时调整日志级别，可以使用Actuator：

### 调整特定包的日志级别
curl -X POST http://localhost:8080/actuator/loggers/com.auction \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

### 查看当前日志级别
curl http://localhost:8080/actuator/loggers/com.auction

### ================================================================================