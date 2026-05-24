# 直播竞拍全栈系统 - Plan 2: 竞拍核心功能

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现竞拍核心功能，包括商品管理、竞拍 CRUD、状态机驱动、定时任务（开始/结束/自动延时）、基础订单生成。

**Architecture:** Controller → Service → Repository 分层架构，状态机模式管理竞拍状态，Spring Task 定时任务处理时间相关逻辑。

**Tech Stack:** Spring Boot, MyBatis-Plus, Redis, Spring Task

---

## 文件结构

```
auction-system/
├── src/main/java/com/auction/
│   ├── api/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── CreateProductRequest.java
│   │   │   │   ├── CreateAuctionRequest.java
│   │   │   │   ├── UpdateAuctionRequest.java
│   │   │   │   └── StartAuctionRequest.java
│   │   │   └── response/
│   │   │       ├── ProductResponse.java
│   │   │       ├── AuctionResponse.java
│   │   │       └── AuctionDetailResponse.java
│   │   ├── controller/
│   │   │   ├── ProductController.java
│   │   │   └── AuctionController.java
│   │   └── assembler/
│   │       ├── ProductAssembler.java
│   │       └── AuctionAssembler.java
│   ├── service/
│   │   ├── product/
│   │   │   └── ProductService.java
│   │   └── auction/
│   │       ├── AuctionService.java
│   │       ├── AuctionStateMachine.java
│   │       └── AuctionScheduler.java
│   └── repository/
│       ├── ProductRepository.java
│       └── AuctionRepository.java
```

---

## Task 1: 创建 DTO 类

**Files:**
- Create: `src/main/java/com/auction/api/dto/request/CreateProductRequest.java`
- Create: `src/main/java/com/auction/api/dto/request/CreateAuctionRequest.java`
- Create: `src/main/java/com/auction/api/dto/request/UpdateAuctionRequest.java`
- Create: `src/main/java/com/auction/api/dto/request/StartAuctionRequest.java`
- Create: `src/main/java/com/auction/api/dto/response/ProductResponse.java`
- Create: `src/main/java/com/auction/api/dto/response/AuctionResponse.java`
- Create: `src/main/java/com/auction/api/dto/response/AuctionDetailResponse.java`

- [ ] **Step 1: 创建 CreateProductRequest**

```java
package com.auction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProductRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称长度不能超过200")
    private String name;

    private String imageUrl;

    private String description;

    @Size(max = 50, message = "分类长度不能超过50")
    private String category;
}
```

- [ ] **Step 2: 创建 CreateAuctionRequest**

```java
package com.auction.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateAuctionRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotBlank(message = "竞拍标题不能为空")
    private String title;

    @NotNull(message = "起拍价不能为空")
    @Positive(message = "起拍价必须大于0")
    private BigDecimal startPrice;

    @NotNull(message = "加价幅度不能为空")
    @Positive(message = "加价幅度必须大于0")
    private BigDecimal bidIncrement;

    private BigDecimal maxPrice;

    private Integer delaySeconds = 15;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
```

- [ ] **Step 3: 创建 UpdateAuctionRequest**

```java
package com.auction.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateAuctionRequest {

    @NotNull(message = "竞拍ID不能为空")
    private Long id;

    private String title;

    private BigDecimal startPrice;

    private BigDecimal bidIncrement;

    private BigDecimal maxPrice;

    private Integer delaySeconds;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
```

- [ ] **Step 4: 创建 StartAuctionRequest**

```java
package com.auction.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartAuctionRequest {

    @NotNull(message = "竞拍ID不能为空")
    private Long auctionId;
}
```

- [ ] **Step 5: 创建 ProductResponse**

```java
package com.auction.api.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private String description;
    private String category;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 6: 创建 AuctionResponse**

```java
package com.auction.api.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuctionResponse {
    private Long id;
    private String title;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal bidIncrement;
    private BigDecimal maxPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String statusDesc;
    private Long highestBidder;
    private Integer bidCount;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 7: 创建 AuctionDetailResponse**

```java
package com.auction.api.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuctionDetailResponse extends AuctionResponse {
    private String description;
    private Integer delaySeconds;
    private LocalDateTime originalEndTime;
    private Long participantCount;
    private Boolean isExtendable;
}
```

- [ ] **Step 8: 提交**

```bash
git add .
git commit -m "feat: 添加 DTO 类"
```

---

## Task 2: 创建 Assembler 转换器

**Files:**
- Create: `src/main/java/com/auction/api/assembler/ProductAssembler.java`
- Create: `src/main/java/com/auction/api/assembler/AuctionAssembler.java`

- [ ] **Step 1: 创建 ProductAssembler**

```java
package com.auction.api.assembler;

import com.auction.api.dto.response.ProductResponse;
import com.auction.domain.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductAssembler {

    public ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setImageUrl(product.getImageUrl());
        response.setDescription(product.getDescription());
        response.setCategory(product.getCategory());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }
}
```

- [ ] **Step 2: 创建 AuctionAssembler**

```java
package com.auction.api.assembler;

import com.auction.api.dto.response.AuctionResponse;
import com.auction.api.dto.response.AuctionDetailResponse;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.Product;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionAssembler {

    private final RedisService redisService;

    public AuctionResponse toResponse(Auction auction, Product product) {
        AuctionResponse response = new AuctionResponse();
        response.setId(auction.getId());
        response.setTitle(auction.getTitle());
        response.setProductId(auction.getProductId());
        response.setProductName(product != null ? product.getName() : null);
        response.setProductImageUrl(product != null ? product.getImageUrl() : null);
        response.setStartPrice(auction.getStartPrice());
        response.setCurrentPrice(auction.getCurrentPrice());
        response.setBidIncrement(auction.getBidIncrement());
        response.setMaxPrice(auction.getMaxPrice());
        response.setStartTime(auction.getStartTime());
        response.setEndTime(auction.getEndTime());
        response.setStatus(auction.getStatus());

        AuctionStatus statusEnum = auction.getStatusEnum();
        response.setStatusDesc(statusEnum != null ? statusEnum.getDescription() : null);

        response.setHighestBidder(auction.getHighestBidder());
        response.setCreatedAt(auction.getCreatedAt());

        // 从 Redis 获取出价次数
        String bidCountKey = "auction:" + auction.getId() + ":bid_count";
        Object bidCountObj = redisService.get(bidCountKey);
        response.setBidCount(bidCountObj != null ? Integer.parseInt(bidCountObj.toString()) : 0);

        return response;
    }

    public AuctionDetailResponse toDetailResponse(Auction auction, Product product) {
        AuctionDetailResponse response = new AuctionDetailResponse();
        response.setId(auction.getId());
        response.setTitle(auction.getTitle());
        response.setProductId(auction.getProductId());
        response.setProductName(product != null ? product.getName() : null);
        response.setProductImageUrl(product != null ? product.getImageUrl() : null);
        response.setStartPrice(auction.getStartPrice());
        response.setCurrentPrice(auction.getCurrentPrice());
        response.setBidIncrement(auction.getBidIncrement());
        response.setMaxPrice(auction.getMaxPrice());
        response.setDescription(product != null ? product.getDescription() : null);
        response.setDelaySeconds(auction.getDelaySeconds());
        response.setStartTime(auction.getStartTime());
        response.setEndTime(auction.getEndTime());
        response.setOriginalEndTime(auction.getOriginalEndTime());
        response.setStatus(auction.getStatus());

        AuctionStatus statusEnum = auction.getStatusEnum();
        response.setStatusDesc(statusEnum != null ? statusEnum.getDescription() : null);

        response.setHighestBidder(auction.getHighestBidder());
        response.setCreatedAt(auction.getCreatedAt());

        // 从 Redis 获取出价次数
        String bidCountKey = "auction:" + auction.getId() + ":bid_count";
        Object bidCountObj = redisService.get(bidCountKey);
        response.setBidCount(bidCountObj != null ? Integer.parseInt(bidCountObj.toString()) : 0);

        // 判断是否可延时（活跃状态且剩余时间小于延时秒数）
        response.setIsExtendable(statusEnum == AuctionStatus.ACTIVE);

        return response;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "feat: 添加 Assembler 转换器"
```

---

## Task 3: 创建 Repository 层

**Files:**
- Create: `src/main/java/com/auction/repository/ProductRepository.java`
- Create: `src/main/java/com/auction/repository/AuctionRepository.java`

- [ ] **Step 1: 创建 ProductRepository**

```java
package com.auction.repository;

import com.auction.domain.entity.Product;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.auction.infrastructure.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepository {

    private final ProductMapper productMapper;

    public Product save(Product product) {
        productMapper.insert(product);
        return product;
    }

    public Product findById(Long id) {
        return productMapper.selectById(id);
    }

    public List<Product> findAll() {
        return productMapper.selectList(null);
    }

    public Product updateById(Product product) {
        productMapper.updateById(product);
        return product;
    }

    public boolean deleteById(Long id) {
        return productMapper.deleteById(id) > 0;
    }

    public boolean existsById(Long id) {
        return productMapper.selectCount(
            new LambdaQueryWrapper<Product>().eq(Product::getId, id)
        ) > 0;
    }

    public List<Product> findByCategory(String category) {
        return productMapper.selectList(
            new LambdaQueryWrapper<Product>().eq(Product::getCategory, category)
        );
    }
}
```

- [ ] **Step 2: 创建 AuctionRepository**

```java
package com.auction.repository;

import com.auction.domain.entity.Auction;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.mapper.AuctionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuctionRepository {

    private final AuctionMapper auctionMapper;

    public Auction save(Auction auction) {
        if (auction.getId() == null) {
            auctionMapper.insert(auction);
        } else {
            auctionMapper.updateById(auction);
        }
        return auction;
    }

    public Auction findById(Long id) {
        return auctionMapper.selectById(id);
    }

    public List<Auction> findAll() {
        return auctionMapper.selectList(null);
    }

    public IPage<Auction> findByPage(int pageNum, int pageSize) {
        Page<Auction> page = new Page<>(pageNum, pageSize);
        return auctionMapper.selectPage(page, null);
    }

    public List<Auction> findByStatus(AuctionStatus status) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, status.name())
                .orderByDesc(Auction::getCreatedAt)
        );
    }

    public List<Auction> findActiveAuctions() {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.ACTIVE.name())
                .orderByAsc(Auction::getEndTime)
        );
    }

    public List<Auction> findPendingAuctions(LocalDateTime before) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.PENDING.name())
                .le(Auction::getStartTime, before)
        );
    }

    public List<Auction> findEndingSoonAuctions(LocalDateTime after, LocalDateTime before) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.ACTIVE.name())
                .ge(Auction::getEndTime, after)
                .le(Auction::getEndTime, before)
        );
    }

    public Auction updateById(Auction auction) {
        auctionMapper.updateById(auction);
        return auction;
    }

    public boolean deleteById(Long id) {
        return auctionMapper.deleteById(id) > 0;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "feat: 添加 Repository 层"
```

---

## Task 4: 实现状态机

**Files:**
- Create: `src/main/java/com/auction/service/auction/AuctionStateMachine.java`

- [ ] **Step 1: 创建 AuctionStateMachine**

```java
package com.auction.service.auction;

import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Auction;
import com.auction.domain.enums.AuctionStatus;
import com.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionStateMachine {

    private final AuctionRepository auctionRepository;

    private static final EnumMap<AuctionStatus, Set<AuctionStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(AuctionStatus.class);
        ALLOWED_TRANSITIONS.put(AuctionStatus.PENDING, Set.of(AuctionStatus.ACTIVE, AuctionStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(AuctionStatus.ACTIVE, Set.of(AuctionStatus.PAUSED, AuctionStatus.CANCELLED, AuctionStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(AuctionStatus.PAUSED, Set.of(AuctionStatus.ACTIVE, AuctionStatus.CANCELLED, AuctionStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(AuctionStatus.CANCELLED, Set.of());
        ALLOWED_TRANSITIONS.put(AuctionStatus.COMPLETED, Set.of());
    }

    public boolean canTransition(AuctionStatus from, AuctionStatus to) {
        Set<AuctionStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public AuctionStatus transition(Auction auction, AuctionStatus to, String reason) {
        AuctionStatus from = auction.getStatusEnum();

        if (from == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "竞拍状态为空");
        }

        if (!canTransition(from, to)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                String.format("非法状态转换: %s -> %s", from.getDescription(), to.getDescription()));
        }

        log.info("竞拍状态转换: auctionId={}, {} -> {}, reason={}",
            auction.getId(), from, to, reason);

        // 执行转换前处理
        onExit(auction, from);

        // 更新状态
        auction.setStatusEnum(to);
        auctionRepository.updateById(auction);

        // 执行转换后处理
        onEnter(auction, to, reason);

        return to;
    }

    private void onExit(Auction auction, AuctionStatus from) {
        switch (from) {
            case ACTIVE:
            case PAUSED:
                // 清理定时任务（在 Scheduler 中实现）
                break;
            default:
                break;
        }
    }

    private void onEnter(Auction auction, AuctionStatus to, String reason) {
        switch (to) {
            case ACTIVE:
                // 初始化 Redis 状态
                initializeAuctionState(auction);
                break;
            case COMPLETED:
                // 生成订单（后续实现）
                break;
            case CANCELLED:
                // 取消处理（清理 Redis 等）
                cleanupAuctionState(auction);
                break;
            default:
                break;
        }
    }

    private void initializeAuctionState(Auction auction) {
        // 在 Scheduler 中实现
    }

    private void cleanupAuctionState(Auction auction) {
        // 在 Scheduler 中实现
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加状态机"
```

---

## Task 5: 实现竞拍调度器

**Files:**
- Create: `src/main/java/com/auction/service/auction/AuctionScheduler.java`
- Create: `src/main/java/com/auction/service/auction/ScheduledTask.java`

- [ ] **Step 1: 创建 ScheduledTask 辅助类**

```java
package com.auction.service.auction;

import lombok.Data;
import java.util.concurrent.ScheduledFuture;

@Data
public class ScheduledTask {
    private String taskId;
    private ScheduledFuture<?> future;
    private String type; // COUNTDOWN, CHECK_END, AUTO_START

    public void cancel() {
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
        }
    }

    public boolean isCancelled() {
        return future != null && future.isCancelled();
    }
}
```

- [ ] **Step 2: 创建 AuctionScheduler**

```java
package com.auction.service.auction;

import com.auction.domain.entity.Auction;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final TaskScheduler taskScheduler;
    private final AuctionRepository auctionRepository;
    private final AuctionStateMachine stateMachine;
    private final RedisService redisService;

    private final Map<Long, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();

    // ==================== 竞拍开始调度 ====================

    public void scheduleAutoStart(Long auctionId, LocalDateTime startTime) {
        String taskId = "auto_start:" + auctionId;
        cancelTask(taskId);

        Date startDate = Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
            () -> checkAndStartAuction(auctionId),
            startDate,
            1000 // 每秒检查一次
        );

        ScheduledTask task = new ScheduledTask();
        task.setTaskId(taskId);
        task.setFuture(future);
        task.setType("AUTO_START");
        scheduledTasks.put(auctionId, task);

        log.info("调度竞拍自动开始: auctionId={}, startTime={}", auctionId, startTime);
    }

    private void checkAndStartAuction(Long auctionId) {
        try {
            Auction auction = auctionRepository.findById(auctionId);
            if (auction == null) {
                cancelAllTasks(auctionId);
                return;
            }

            if (auction.getStatusEnum() == AuctionStatus.PENDING) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(auction.getStartTime()) || now.isEqual(auction.getStartTime())) {
                    startAuction(auction);
                }
            } else {
                // 状态已变更，取消调度
                cancelAllTasks(auctionId);
            }
        } catch (Exception e) {
            log.error("检查竞拍开始失败: auctionId={}", auctionId, e);
        }
    }

    // ==================== 竞拍倒计时调度 ====================

    public void scheduleCountdown(Long auctionId, LocalDateTime endTime) {
        String taskId = "countdown:" + auctionId;
        cancelTask(taskId);

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
            () -> updateCountdown(auctionId),
            Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()),
            100 // 每 100ms 更新一次
        );

        ScheduledTask task = new ScheduledTask();
        task.setTaskId(taskId);
        task.setFuture(future);
        task.setType("COUNTDOWN");
        scheduledTasks.put(auctionId, task);

        log.info("启动竞拍倒计时: auctionId={}, endTime={}", auctionId, endTime);
    }

    private void updateCountdown(Long auctionId) {
        try {
            Auction auction = auctionRepository.findById(auctionId);
            if (auction == null || auction.getStatusEnum() != AuctionStatus.ACTIVE) {
                cancelAllTasks(auctionId);
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = auction.getEndTime();

            if (now.isAfter(endTime) || now.isEqual(endTime)) {
                // 竞拍结束
                endAuction(auction);
            } else {
                // 更新剩余时间到 Redis
                long remainingMs = Duration.between(now, endTime).toMillis();
                redisService.set("auction:" + auctionId + ":remaining_ms", remainingMs);
            }
        } catch (Exception e) {
            log.error("更新倒计时失败: auctionId={}", auctionId, e);
        }
    }

    // ==================== 状态操作 ====================

    public void startAuction(Auction auction) {
        stateMachine.transition(auction, AuctionStatus.ACTIVE, "定时任务自动开始");
        initializeAuctionState(auction);
        scheduleCountdown(auction.getId(), auction.getEndTime());
    }

    public void endAuction(Auction auction) {
        stateMachine.transition(auction, AuctionStatus.COMPLETED, "时间到期自动结束");
        finalizeAuction(auction);
        cancelAllTasks(auction.getId());
    }

    public void pauseAuction(Auction auction, String reason) {
        stateMachine.transition(auction, AuctionStatus.PAUSED, reason);
        cancelCountdownTask(auction.getId());
    }

    public void resumeAuction(Auction auction) {
        stateMachine.transition(auction, AuctionStatus.ACTIVE, "恢复竞拍");
        scheduleCountdown(auction.getId(), auction.getEndTime());
    }

    public void cancelAuction(Auction auction, String reason) {
        stateMachine.transition(auction, AuctionStatus.CANCELLED, reason);
        cleanupAuctionState(auction);
        cancelAllTasks(auction.getId());
    }

    // ==================== 自动延时 ====================

    public boolean checkAndExtend(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();
        long remainingSeconds = Duration.between(now, endTime).getSeconds();

        if (remainingSeconds <= auction.getDelaySeconds()) {
            // 延长竞拍时间
            LocalDateTime newEndTime = endTime.plusSeconds(auction.getDelaySeconds());
            auction.setEndTime(newEndTime);
            auctionRepository.updateById(auction);

            // 更新 Redis
            redisService.set("auction:" + auction.getId() + ":end_time", newEndTime.toString());

            log.info("竞拍自动延时: auctionId={}, oldEndTime={}, newEndTime={}",
                auction.getId(), endTime, newEndTime);

            return true;
        }
        return false;
    }

    // ==================== Redis 状态管理 ====================

    private void initializeAuctionState(Auction auction) {
        String auctionKey = "auction:" + auction.getId();

        redisService.set(auctionKey + ":current_price", auction.getCurrentPrice().toString());
        redisService.set(auctionKey + ":bid_count", "0");
        redisService.set(auctionKey + ":end_time", auction.getEndTime().toString());

        if (auction.getHighestBidder() != null) {
            redisService.set(auctionKey + ":highest_bidder", auction.getHighestBidder().toString());
        }

        log.info("初始化竞拍 Redis 状态: auctionId={}", auction.getId());
    }

    private void cleanupAuctionState(Auction auction) {
        String auctionKey = "auction:" + auction.getId();

        redisService.delete(auctionKey + ":current_price");
        redisService.delete(auctionKey + ":bid_count");
        redisService.delete(auctionKey + ":highest_bidder");
        redisService.delete(auctionKey + ":end_time");
        redisService.delete(auctionKey + ":remaining_ms");

        log.info("清理竞拍 Redis 状态: auctionId={}", auction.getId());
    }

    private void finalizeAuction(Auction auction) {
        // TODO: 生成订单，通知获胜者
        log.info("竞拍结束处理: auctionId={}, finalPrice={}",
            auction.getId(), auction.getCurrentPrice());
    }

    // ==================== 任务管理 ====================

    private void cancelTask(String taskId) {
        scheduledTasks.values().removeIf(task -> {
            if (task.getTaskId().equals(taskId)) {
                task.cancel();
                return true;
            }
            return false;
        });
    }

    private void cancelCountdownTask(Long auctionId) {
        ScheduledTask task = scheduledTasks.get(auctionId);
        if (task != null && "COUNTDOWN".equals(task.getType())) {
            task.cancel();
        }
    }

    public void cancelAllTasks(Long auctionId) {
        ScheduledTask task = scheduledTasks.remove(auctionId);
        if (task != null) {
            task.cancel();
            log.info("取消所有调度任务: auctionId={}", auctionId);
        }
    }
}
```

- [ ] **Step 3: 创建 TaskScheduler 配置**

```java
package com.auction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("auction-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add .
git commit -m "feat: 添加竞拍调度器"
```

---

## Task 6: 实现 ProductService

**Files:**
- Create: `src/main/java/com/auction/service/product/ProductService.java`

- [ ] **Step 1: 创建 ProductService**

```java
package com.auction.service.product;

import com.auction.api.assembler.ProductAssembler;
import com.auction.api.dto.request.CreateProductRequest;
import com.auction.api.dto.response.ProductResponse;
import com.auction.domain.entity.Product;
import com.auction.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductAssembler productAssembler;

    public ProductResponse create(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());

        product = productRepository.save(product);
        return productAssembler.toResponse(product);
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new com.auction.common.BizException(
                com.auction.common.ErrorCode.PRODUCT_NOT_FOUND);
        }
        return productAssembler.toResponse(product);
    }

    public List<ProductResponse> listAll() {
        return productRepository.findAll().stream()
            .map(productAssembler::toResponse)
            .collect(Collectors.toList());
    }

    public ProductResponse update(Long id, CreateProductRequest request) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new com.auction.common.BizException(
                com.auction.common.ErrorCode.PRODUCT_NOT_FOUND);
        }

        product.setName(request.getName());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());

        product = productRepository.updateById(product);
        return productAssembler.toResponse(product);
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new com.auction.common.BizException(
                com.auction.common.ErrorCode.PRODUCT_NOT_FOUND);
        }
        productRepository.deleteById(id);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加 ProductService"
```

---

## Task 7: 实现 AuctionService

**Files:**
- Create: `src/main/java/com/auction/service/auction/AuctionService.java`

- [ ] **Step 1: 创建 AuctionService**

```java
package com.auction.service.auction;

import com.auction.api.assembler.AuctionAssembler;
import com.auction.api.dto.request.*;
import com.auction.api.dto.response.AuctionDetailResponse;
import com.auction.api.dto.response.AuctionResponse;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.Product;
import com.auction.domain.enums.AuctionStatus;
import com.auction.repository.AuctionRepository;
import com.auction.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final AuctionAssembler auctionAssembler;
    private final AuctionScheduler auctionScheduler;

    @Transactional
    public AuctionResponse create(CreateAuctionRequest request) {
        // 验证商品存在
        Product product = productRepository.findById(request.getProductId());
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 验证时间
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "结束时间不能早于开始时间");
        }

        // 验证价格
        if (request.getMaxPrice() != null &&
            request.getMaxPrice().compareTo(request.getStartPrice()) <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "封顶价必须高于起拍价");
        }

        Auction auction = new Auction();
        auction.setProductId(request.getProductId());
        auction.setTitle(request.getTitle());
        auction.setStartPrice(request.getStartPrice());
        auction.setBidIncrement(request.getBidIncrement());
        auction.setMaxPrice(request.getMaxPrice());
        auction.setDelaySeconds(request.getDelaySeconds() != null ? request.getDelaySeconds() : 15);
        auction.setStartTime(request.getStartTime());
        auction.setEndTime(request.getEndTime());
        auction.setOriginalEndTime(request.getEndTime());
        auction.setCurrentPrice(request.getStartPrice());
        auction.setStatusEnum(AuctionStatus.PENDING);

        auction = auctionRepository.save(auction);

        // 调度自动开始
        auctionScheduler.scheduleAutoStart(auction.getId(), auction.getStartTime());

        log.info("创建竞拍: auctionId={}, productId={}", auction.getId(), auction.getProductId());

        return auctionAssembler.toResponse(auction, product);
    }

    public AuctionDetailResponse getById(Long id) {
        Auction auction = auctionRepository.findById(id);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        Product product = productRepository.findById(auction.getProductId());
        return auctionAssembler.toDetailResponse(auction, product);
    }

    public List<AuctionResponse> listAll() {
        List<Auction> auctions = auctionRepository.findAll();

        return auctions.stream()
            .map(auction -> {
                Product product = productRepository.findById(auction.getProductId());
                return auctionAssembler.toResponse(auction, product);
            })
            .collect(Collectors.toList());
    }

    public List<AuctionResponse> listActive() {
        List<Auction> auctions = auctionRepository.findByStatus(AuctionStatus.ACTIVE);

        return auctions.stream()
            .map(auction -> {
                Product product = productRepository.findById(auction.getProductId());
                return auctionAssembler.toResponse(auction, product);
            })
            .collect(Collectors.toList());
    }

    public List<AuctionResponse> listPending() {
        List<Auction> auctions = auctionRepository.findByStatus(AuctionStatus.PENDING);

        return auctions.stream()
            .map(auction -> {
                Product product = productRepository.findById(auction.getProductId());
                return auctionAssembler.toResponse(auction, product);
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public AuctionResponse update(UpdateAuctionRequest request) {
        Auction auction = auctionRepository.findById(request.getId());
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 只有待开始状态可以修改
        if (auction.getStatusEnum() != AuctionStatus.PENDING) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有待开始状态的竞拍可以修改");
        }

        if (request.getTitle() != null) {
            auction.setTitle(request.getTitle());
        }
        if (request.getStartPrice() != null) {
            auction.setStartPrice(request.getStartPrice());
            auction.setCurrentPrice(request.getStartPrice());
        }
        if (request.getBidIncrement() != null) {
            auction.setBidIncrement(request.getBidIncrement());
        }
        if (request.getMaxPrice() != null) {
            auction.setMaxPrice(request.getMaxPrice());
        }
        if (request.getDelaySeconds() != null) {
            auction.setDelaySeconds(request.getDelaySeconds());
        }
        if (request.getStartTime() != null) {
            auction.setStartTime(request.getStartTime());
            // 重新调度自动开始
            auctionScheduler.scheduleAutoStart(auction.getId(), request.getStartTime());
        }
        if (request.getEndTime() != null) {
            auction.setEndTime(request.getEndTime());
            auction.setOriginalEndTime(request.getEndTime());
        }

        auction = auctionRepository.updateById(auction);

        Product product = productRepository.findById(auction.getProductId());
        return auctionAssembler.toResponse(auction, product);
    }

    @Transactional
    public void start(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        auctionScheduler.startAuction(auction);

        log.info("手动开始竞拍: auctionId={}", auctionId);
    }

    @Transactional
    public void pause(Long auctionId, String reason) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        auctionScheduler.pauseAuction(auction, reason != null ? reason : "管理员暂停");

        log.info("暂停竞拍: auctionId={}, reason={}", auctionId, reason);
    }

    @Transactional
    public void resume(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        auctionScheduler.resumeAuction(auction);

        log.info("恢复竞拍: auctionId={}", auctionId);
    }

    @Transactional
    public void cancel(Long auctionId, String reason) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        auctionScheduler.cancelAuction(auction, reason != null ? reason : "管理员取消");

        log.info("取消竞拍: auctionId={}, reason={}", auctionId, reason);
    }

    public void delete(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 只有待开始和已取消状态可以删除
        AuctionStatus status = auction.getStatusEnum();
        if (status == AuctionStatus.ACTIVE || status == AuctionStatus.PAUSED) {
            throw new BizException(ErrorCode.BAD_REQUEST, "进行中的竞拍不能删除");
        }

        auctionScheduler.cancelAllTasks(auctionId);
        auctionRepository.deleteById(auctionId);

        log.info("删除竞拍: auctionId={}", auctionId);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加 AuctionService"
```

---

## Task 8: 创建 Controller

**Files:**
- Create: `src/main/java/com/auction/api/controller/ProductController.java`
- Create: `src/main/java/com/auction/api/controller/AuctionController.java`

- [ ] **Step 1: 创建 ProductController**

```java
package com.auction.api.controller;

import com.auction.api.dto.request.CreateProductRequest;
import com.auction.api.dto.response.ProductResponse;
import com.auction.common.Result;
import com.auction.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public Result<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return Result.ok(productService.create(request));
    }

    @GetMapping("/{id}")
    public Result<ProductResponse> getById(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @GetMapping
    public Result<List<ProductResponse>> listAll() {
        return Result.ok(productService.listAll());
    }

    @PutMapping("/{id}")
    public Result<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest request) {
        return Result.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}
```

- [ ] **Step 2: 创建 AuctionController**

```java
package com.auction.api.controller;

import com.auction.api.dto.request.*;
import com.auction.api.dto.response.AuctionDetailResponse;
import com.auction.api.dto.response.AuctionResponse;
import com.auction.common.Result;
import com.auction.service.auction.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    public Result<AuctionResponse> create(@Valid @RequestBody CreateAuctionRequest request) {
        return Result.ok(auctionService.create(request));
    }

    @GetMapping("/{id}")
    public Result<AuctionDetailResponse> getById(@PathVariable Long id) {
        return Result.ok(auctionService.getById(id));
    }

    @GetMapping
    public Result<List<AuctionResponse>> listAll() {
        return Result.ok(auctionService.listAll());
    }

    @GetMapping("/active")
    public Result<List<AuctionResponse>> listActive() {
        return Result.ok(auctionService.listActive());
    }

    @GetMapping("/pending")
    public Result<List<AuctionResponse>> listPending() {
        return Result.ok(auctionService.listPending());
    }

    @PutMapping
    public Result<AuctionResponse> update(@Valid @RequestBody UpdateAuctionRequest request) {
        return Result.ok(auctionService.update(request));
    }

    @PostMapping("/start")
    public Result<Void> start(@Valid @RequestBody StartAuctionRequest request) {
        auctionService.start(request.getAuctionId());
        return Result.ok();
    }

    @PostMapping("/{id}/pause")
    public Result<Void> pause(@PathVariable Long id, @RequestParam(required = false) String reason) {
        auctionService.pause(id, reason);
        return Result.ok();
    }

    @PostMapping("/{id}/resume")
    public Result<Void> resume(@PathVariable Long id) {
        auctionService.resume(id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        auctionService.cancel(id, reason);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        auctionService.delete(id);
        return Result.ok();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "feat: 添加 Controller"
```

---

## Task 9: 测试 API

- [ ] **Step 1: 启动应用**

```bash
mvn spring-boot:run
```

- [ ] **Step 2: 测试创建商品**

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "翡翠手镯",
    "description": "天然A货翡翠手镯",
    "category": "珠宝",
    "imageUrl": "https://example.com/jade.jpg"
  }'
```

Expected: 返回创建的商品信息

- [ ] **Step 3: 测试创建竞拍**

```bash
curl -X POST http://localhost:8080/api/auctions \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "title": "翡翠手镯竞拍",
    "startPrice": 100,
    "bidIncrement": 50,
    "delaySeconds": 15,
    "startTime": "2026-05-22T10:00:00",
    "endTime": "2026-05-22T11:00:00"
  }'
```

Expected: 返回创建的竞拍信息

- [ ] **Step 4: 测试获取竞拍详情**

```bash
curl http://localhost:8080/api/auctions/1
```

Expected: 返回竞拍详情

- [ ] **Step 5: 测试开始竞拍**

```bash
curl -X POST http://localhost:8080/api/auctions/start \
  -H "Content-Type: application/json" \
  -d '{"auctionId": 1}'
```

Expected: 竞拍状态变为 ACTIVE

- [ ] **Step 6: 测试暂停/恢复竞拍**

```bash
curl -X POST http://localhost:8080/api/auctions/1/pause
curl -X POST http://localhost:8080/api/auctions/1/resume
```

Expected: 状态正确切换

- [ ] **Step 7: 验证 Redis 数据**

```bash
redis-cli
> GET auction:1:current_price
> GET auction:1:bid_count
> GET auction:1:remaining_ms
```

Expected: Redis 中有正确的数据

- [ ] **Step 8: 提交**

```bash
git add .
git commit -m "test: 验证竞拍核心功能 API"
```

---

## 验收标准

完成本计划后，应该能够：

1. ✅ 创建商品并获取商品列表
2. ✅ 创建竞拍并设置规则（起拍价、加价幅度、延时等）
3. ✅ 修改待开始的竞拍
4. ✅ 手动开始/暂停/恢复/取消竞拍
5. ✅ 定时任务自动开始待开始的竞拍
6. ✅ 倒计时正确运行
7. ✅ 状态机正确转换状态
8. ✅ Redis 中维护竞拍状态

---

## 下一步

完成本计划后，继续 **Plan 3: 出价服务 + 实时通信**，实现出价功能和 WebSocket 推送。
