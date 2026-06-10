package com.auction.api.controller;

import com.auction.api.dto.request.CreateAuctionRequest;
import com.auction.api.dto.response.AuctionDetailResponse;
import com.auction.api.dto.response.AuctionStatisticsResponse;
import com.auction.common.PageResult;
import com.auction.common.Result;
import com.auction.domain.entity.Auction;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.security.CurrentUser;
import com.auction.infrastructure.security.UserPrincipal;
import com.auction.repository.AuctionRepository;
import com.auction.service.auction.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 拍卖活动控制器（重构后）
 * <p>
 * 完整实现拍卖活动的API接口，支持多拍品管理
 * 集成新的AuctionService和AuctionItemService
 * 提供完整的业务规则验证和权限控制
 * <p>
 * 路由顺序说明：
 * 1. 具体路径（如 /active, /my）必须放在通配符路径（如 /{id}）之前
 * 2. 根路径 @GetMapping 放在通配符路径之前
 * 3. 通配符路径 @GetMapping("/{id}") 放在最后作为默认路由
 */
@Slf4j
@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;
    private final AuctionRepository auctionRepository;

    /**
     * 创建拍卖活动
     * POST /auctions
     * <p>
     * 支持创建包含多个拍品的拍卖活动
     * 验证商品、时间、数量等业务规则
     */
    @PostMapping
    public Result<AuctionDetailResponse> create(
            @Valid @RequestBody CreateAuctionRequest request,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("创建拍卖活动: userId={}, request={}", currentUser.getUserId(), request);

            // 权限验证：只有商家和管理员可以创建活动
            if (!currentUser.isMerchant() && !currentUser.isAdmin()) {
                return Result.fail(403, "只有商家可以创建拍卖活动");
            }

            AuctionDetailResponse response = auctionService.createAuction(request, currentUser.getUserId());
            return Result.ok(response);

        } catch (Exception e) {
            log.error("创建拍卖活动失败: userId={}, error={}", currentUser.getUserId(), e.getMessage(), e);
            return Result.fail(500, "创建拍卖活动失败: " + e.getMessage());
        }
    }

    /**
     * 查询活跃活动列表
     * GET /auctions/active
     * <p>
     * 获取当前正在进行中的拍卖活动
     * 适用于首页展示和用户浏览
     */
    @GetMapping("/active")
    public Result<List<Auction>> listActiveAuctions() {
        try {
            log.info("查询活跃活动列表");

            List<Auction> auctions = auctionRepository.findByStatus(AuctionStatus.ACTIVE);
            return Result.ok(auctions);

        } catch (Exception e) {
            log.error("查询活跃活动失败: error={}", e.getMessage(), e);
            return Result.fail(500, "查询活跃活动失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户创建的活动
     * GET /auctions/my
     * <p>
     * 获取当前用户创建的所有活动
     * 支持分页和状态筛选
     * 防御措施：
     * 1. 参数验证
     * 2. 分页参数限制
     */
    @GetMapping("/my")
    public Result<PageResult<Auction>> getMyAuctions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) AuctionStatus status,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("查询用户活动: userId={}, page={}, size={}, status={}",
                    currentUser.getUserId(), page, size, status);

            // 1. 验证分页参数
            if (page < 1 || page > 9999) {
                return Result.fail(400, "页码无效");
            }
            if (size < 1 || size > 100) {
                return Result.fail(400, "每页数量必须在1-100之间");
            }

            // 2. 执行分页查询
            com.baomidou.mybatisplus.core.metadata.IPage<Auction> pageResult;
            if (status != null) {
                // 先查询用户所有活动，再按状态过滤（简化处理）
                List<Auction> allAuctions = auctionRepository.findByHostId(currentUser.getUserId());
                List<Auction> filtered = allAuctions.stream()
                        .filter(auction -> auction.getStatusEnum() == status)
                        .toList();

                // 手动分页
                int start = (page - 1) * size;
                int end = Math.min(start + size, filtered.size());
                List<Auction> pageData = start < filtered.size() ?
                        filtered.subList(start, end) : List.of();

                com.baomidou.mybatisplus.extension.plugins.pagination.Page<Auction> myPage =
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
                myPage.setRecords(pageData);
                myPage.setTotal(filtered.size());
                pageResult = myPage;
            } else {
                // 使用分页查询用户活动
                pageResult = auctionRepository.findByHostIdPage(page, size, currentUser.getUserId());
            }

            // 3. 构建返回结果
            PageResult<Auction> result = new PageResult<>(
                    pageResult.getRecords(),
                    pageResult.getTotal(),
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    (long) pageResult.getPages()
            );

            log.info("查询用户活动成功: total={}, pages={}", result.getTotal(), result.getPages());
            return Result.ok(result);

        } catch (Exception e) {
            log.error("查询用户活动失败: userId={}, error={}", currentUser.getUserId(), e.getMessage(), e);
            return Result.fail(500, "查询用户活动失败: " + e.getMessage());
        }
    }

    /**
     * 查询活动列表
     * GET /auctions?page=1&size=20&status=&keyword=
     * <p>
     * 支持分页、状态筛选和关键词搜索
     * 适用于列表展示页面
     * 防御措施：
     * 1. 参数验证
     * 2. 分页参数限制
     * 3. 关键词安全过滤
     */
    @GetMapping
    public Result<PageResult<Auction>> listAuctions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) String keyword,
            @CurrentUser(required = false) UserPrincipal currentUser) {
        try {
            log.info("查询活动列表: page={}, size={}, status={}, keyword={}, userId={}",
                    page, size, status, keyword, currentUser != null ? currentUser.getUserId() : null);

            // 1. 验证分页参数
            if (page < 1 || page > 9999) {
                return Result.fail(400, "页码无效");
            }
            if (size < 1 || size > 100) {
                return Result.fail(400, "每页数量必须在1-100之间");
            }

            // 2. 过滤关键词中的危险字符（防止SQL注入）
            String safeKeyword = null;
            if (keyword != null && !keyword.trim().isEmpty()) {
                safeKeyword = keyword.trim().replaceAll("[';\"--]", "");
                if (safeKeyword.length() > 100) {
                    safeKeyword = safeKeyword.substring(0, 100);
                }
            }

            // 3. 执行分页查询
            com.baomidou.mybatisplus.core.metadata.IPage<Auction> pageResult =
                    auctionRepository.findByPage(page, size, status, safeKeyword);

            // 4. 构建返回结果
            PageResult<Auction> result = new PageResult<>(
                    pageResult.getRecords(),
                    pageResult.getTotal(),
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    (long) pageResult.getPages()
            );

            log.info("查询活动列表成功: total={}, pages={}", result.getTotal(), result.getPages());
            return Result.ok(result);

        } catch (Exception e) {
            log.error("查询活动列表失败: error={}", e.getMessage(), e);
            return Result.fail(500, "查询活动列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询活动详情
     * GET /auctions/{id}
     * <p>
     * 获取活动的完整信息，包含所有拍品列表和统计数据
     * 支持Redis缓存优化
     * <p>
     * 注意：此路由必须放在所有具体路径之后，作为默认路由
     */
    @GetMapping("/{id}")
    public Result<AuctionDetailResponse> getById(@PathVariable Long id) {
        try {
            log.info("查询活动详情: auctionId={}", id);

            AuctionDetailResponse response = auctionService.getAuctionDetail(id);
            return Result.ok(response);

        } catch (Exception e) {
            log.error("查询活动详情失败: auctionId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "查询活动详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新活动信息
     * PUT /auctions/{id}
     * <p>
     * 更新活动的基本信息（仅PENDING状态可更新）
     * 验证权限和业务规则
     */
    @PutMapping("/{id}")
    public Result<AuctionDetailResponse> updateAuction(
            @PathVariable Long id,
            @Valid @RequestBody CreateAuctionRequest request,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("更新活动: auctionId={}, userId={}", id, currentUser.getUserId());

            // 验证活动存在
            Auction auction = auctionRepository.findById(id);
            if (auction == null) {
                return Result.fail(404, "活动不存在");
            }

            // 验证权限：只有创建者和管理员可以更新
            if (!auction.getHostId().equals(currentUser.getUserId()) && !currentUser.isAdmin()) {
                return Result.fail(403, "只有创建者可以更新活动");
            }

            // 验证状态：只有PENDING状态可更新
            if (auction.getStatusEnum() != AuctionStatus.PENDING) {
                return Result.fail(400, "只有待开始状态的活动可以更新");
            }

            // 调用Service更新活动
            AuctionDetailResponse response = auctionService.updateAuction(id, request, currentUser.getUserId());
            return Result.ok(response);

        } catch (Exception e) {
            log.error("更新活动失败: auctionId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "更新活动失败: " + e.getMessage());
        }
    }

    /**
     * 开始活动
     * POST /auctions/{id}/start
     * <p>
     * 启动拍卖活动，批量启动所有拍品
     * 验证时间、权限、状态等业务规则
     */
    @PostMapping("/{id}/start")
    public Result<Void> startAuction(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("开始活动: auctionId={}, userId={}", id, currentUser.getUserId());

            auctionService.startAuction(id, currentUser.getUserId());
            return Result.ok();

        } catch (Exception e) {
            log.error("开始活动失败: auctionId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "开始活动失败: " + e.getMessage());
        }
    }

    /**
     * 结束活动
     * POST /auctions/{id}/end
     * <p>
     * 结束拍卖活动，生成订单
     * 验证所有拍品状态和业务规则
     */
    @PostMapping("/{id}/end")
    public Result<Void> endAuction(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("结束活动: auctionId={}, userId={}", id, currentUser.getUserId());

            auctionService.endAuction(id, currentUser.getUserId());
            return Result.ok();

        } catch (Exception e) {
            log.error("结束活动失败: auctionId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "结束活动失败: " + e.getMessage());
        }
    }

    /**
     * 取消活动
     * POST /auctions/{id}/cancel
     * <p>
     * 取消拍卖活动，退还保证金
     * 支持取消原因记录
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelAuction(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("取消活动: auctionId={}, userId={}, reason={}", id, currentUser.getUserId(), reason);

            auctionService.cancelAuction(id, reason, currentUser.getUserId());
            return Result.ok();

        } catch (Exception e) {
            log.error("取消活动失败: auctionId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "取消活动失败: " + e.getMessage());
        }
    }

    /**
     * 删除活动
     * DELETE /auctions/{id}
     * <p>
     * 删除拍卖活动（仅PENDING状态）
     * 级联删除所有拍品
     * 验证权限和数据一致性
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAuction(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("删除活动: auctionId={}, userId={}", id, currentUser.getUserId());

            // 验证活动存在
            Auction auction = auctionRepository.findById(id);
            if (auction == null) {
                return Result.fail(404, "活动不存在");
            }

            // 验证权限：只有创建者和管理员可以删除
            if (!auction.getHostId().equals(currentUser.getUserId()) && !currentUser.isAdmin()) {
                return Result.fail(403, "只有创建者可以删除活动");
            }

            // 验证状态：只有PENDING状态可删除
            if (auction.getStatusEnum() != AuctionStatus.PENDING) {
                return Result.fail(400, "只有待开始状态的活动可以删除");
            }

            // 调用Service删除活动（级联删除拍品）
            auctionService.deleteAuction(id, currentUser.getUserId());

            log.info("活动删除成功: auctionId={}", id);
            return Result.ok();

        } catch (Exception e) {
            log.error("删除活动失败: auctionId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "删除活动失败: " + e.getMessage());
        }
    }

    /**
     * 获取活动统计
     * GET /auctions/{id}/statistics
     * <p>
     * 获取活动的详细统计数据
     * 包含拍品统计、出价统计、参与人数等
     */
    @GetMapping("/{id}/statistics")
    public Result<AuctionStatisticsResponse> getStatistics(@PathVariable Long id) {
        try {
            log.info("获取活动统计: auctionId={}", id);

            AuctionStatisticsResponse response = auctionService.getStatistics(id);
            return Result.ok(response);

        } catch (Exception e) {
            log.error("获取活动统计失败: auctionId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "获取活动统计失败: " + e.getMessage());
        }
    }

    /**
     * 延长活动时间
     * POST /auctions/{id}/extend
     * <p>
     * 紧急延长活动结束时间
     * 仅ACTIVE状态，需要管理员权限
     */
    @PostMapping("/{id}/extend")
    public Result<Void> extendAuction(
            @PathVariable Long id,
            @RequestParam Integer extendMinutes,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("延长活动时间: auctionId={}, extendMinutes={}, userId={}",
                    id, extendMinutes, currentUser.getUserId());

            // 验证权限：仅管理员可延长
            if (!currentUser.isAdmin()) {
                return Result.fail(403, "只有管理员可以延长活动时间");
            }

            // 验证活动状态
            Auction auction = auctionRepository.findById(id);
            if (auction == null) {
                return Result.fail(404, "活动不存在");
            }

            if (auction.getStatusEnum() != AuctionStatus.ACTIVE) {
                return Result.fail(400, "只有进行中的活动可以延长");
            }

            // 验证延长时长
            if (extendMinutes <= 0 || extendMinutes > 120) {
                return Result.fail(400, "延长时长必须在1-120分钟之间");
            }

            // 调用Service延长活动时间
            auctionService.extendAuction(id, extendMinutes, currentUser.getUserId());

            log.info("活动时间延长成功: auctionId={}, extendMinutes={}", id, extendMinutes);
            return Result.ok();

        } catch (Exception e) {
            log.error("延长活动时间失败: auctionId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "延长活动时间失败: " + e.getMessage());
        }
    }

    /**
     * 查询活动参与用户
     * GET /auctions/{id}/participants
     * <p>
     * 获取参与活动的所有用户列表
     * 仅创建者和管理员可查询
     */
    @GetMapping("/{id}/participants")
    public Result<List<Object>> getParticipants(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("查询活动参与用户: auctionId={}, page={}, size={}", id, page, size);

            // 验证活动存在
            Auction auction = auctionRepository.findById(id);
            if (auction == null) {
                return Result.fail(404, "活动不存在");
            }

            // 验证权限：只有创建者和管理员可查询
            if (!auction.getHostId().equals(currentUser.getUserId()) && !currentUser.isAdmin()) {
                return Result.fail(403, "只有创建者可以查询参与用户");
            }

            // TODO: 实现用户查询逻辑
            // List<User> participants = auctionService.getParticipants(id, page, size);

            return Result.ok(List.of());

        } catch (Exception e) {
            log.error("查询参与用户失败: auctionId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "查询参与用户失败: " + e.getMessage());
        }
    }
}
