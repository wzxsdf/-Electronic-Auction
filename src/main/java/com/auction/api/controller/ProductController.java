package com.auction.api.controller;

import com.auction.api.dto.request.*;
import com.auction.api.dto.response.ProductResponse;
import com.auction.common.Result;
import com.auction.infrastructure.security.CurrentUser;
import com.auction.infrastructure.security.UserPrincipal;
import com.auction.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器（防御性编程版本 - JWT认证）
 * 提供商品创建、查询、更新、状态管理、图片管理等完整业务接口
 * <p>
 * 认证方式：使用JWT Token + @CurrentUser注解
 * 安全性：token存储在HTTP Authorization头部，无法伪造
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 创建商品
     * POST /products
     * <p>
     * 认证方式：从JWT Token自动提取用户信息
     * 防御措施：
     * 1. JWT token验证
     * 2. 请求体验证
     * 3. 业务规则验证
     * 4. 异常处理
     *
     * @param request 商品创建请求
     * @param currentUser 当前登录用户（从JWT自动注入）
     * @return 创建的商品信息
     */
    @PostMapping
    public Result<ProductResponse> create(@Valid @RequestBody CreateProductRequest request,
                                          @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("创建商品请求: userId={}, userRole={}, request={}",
                     currentUser.getUserId(), currentUser.getRole(), request);

            // 权限验证：只有商家和管理员可以创建商品
            if (!currentUser.isMerchant() && !currentUser.isAdmin()) {
                return Result.fail(403, "只有商家可以创建商品");
            }

            ProductResponse response = productService.create(request, currentUser.getUserId());
            return Result.ok(response);

        } catch (Exception e) {
            log.error("创建商品失败: userId={}, error={}", currentUser.getUserId(), e.getMessage(), e);
            return Result.fail(500, "创建商品失败: " + e.getMessage());
        }
    }

    /**
     * 查询商品详情
     * GET /products/{id}
     * <p>
     * 认证方式：公开接口，无需认证（可选）
     * 防御措施：
     * 1. ID有效性验证
     * 2. 商品存在性验证
     * 3. 数据脱敏（如果需要）
     *
     * @param id 商品ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductResponse> getById(@PathVariable Long id) {
        try {
            log.info("查询商品详情: productId={}", id);

            ProductResponse response = productService.getById(id);
            return Result.ok(response);

        } catch (Exception e) {
            log.error("查询商品详情失败: productId={}, error={}", id, e.getMessage());
            return Result.fail(500, "查询商品详情失败: " + e.getMessage());
        }
    }

    /**
     * 查询商品列表（带筛选和分页）
     * GET /products?page=1&size=20&keyword=&category=&status=
     * <p>
     * 认证方式：公开接口，根据角色过滤数据
     * 防御措施：
     * 1. 分页参数验证
     * 2. 排序字段安全性验证
     * 3. 根据用户角色过滤数据
     *
     * @param request 查询请求参数
     * @param currentUser 当前登录用户（可选）
     * @return 商品列表
     */
    @GetMapping
    public Result<List<ProductResponse>> queryProducts(ProductQueryRequest request,
                                                       @CurrentUser(required = false) UserPrincipal currentUser) {
        try {
            Long userId = currentUser != null ? currentUser.getUserId() : null;
            String userRole = currentUser != null ? currentUser.getRole() : "USER";

            log.info("查询商品列表: request={}, userId={}, userRole={}", request, userId, userRole);

            List<ProductResponse> responses = productService.queryProducts(request, userId, userRole);
            return Result.ok(responses);

        } catch (Exception e) {
            log.error("查询商品列表失败: error={}", e.getMessage(), e);
            return Result.fail(500, "查询商品列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品信息
     * PUT /products/{id}
     * <p>
     * 认证方式：需要商家或管理员权限
     * 防御措施：
     * 1. 商品存在性验证
     * 2. 资源所有权验证
     * 3. 乐观锁防并发
     * 4. 敏感字段保护
     *
     * @param id 商品ID
     * @param request 更新请求
     * @param currentUser 当前登录用户
     * @return 更新后的商品信息
     */
    @PutMapping("/{id}")
    public Result<ProductResponse> update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateProductRequest request,
                                         @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("更新商品: productId={}, userId={}, request={}", id, currentUser.getUserId(), request);

            ProductResponse response = productService.update(id, request, currentUser.getUserId());
            return Result.ok(response);

        } catch (Exception e) {
            log.error("更新商品失败: productId={}, userId={}, error={}",
                     id, currentUser.getUserId(), e.getMessage());
            return Result.fail(500, "更新商品失败: " + e.getMessage());
        }
    }

    /**
     * 上架商品
     * POST /products/{id}/list
     * <p>
     * 认证方式：需要商家或管理员权限
     * 防御措施：
     * 1. 商品存在性验证
     * 2. 资源所有权验证
     * 3. 状态转换合法性验证
     *
     * @param id 商品ID
     * @param currentUser 当前登录用户
     * @return 操作结果
     */
    @PostMapping("/{id}/list")
    public Result<Void> listProduct(@PathVariable Long id,
                                    @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("上架商品: productId={}, userId={}", id, currentUser.getUserId());

            productService.listProduct(id, currentUser.getUserId());
            return Result.ok();

        } catch (Exception e) {
            log.error("上架商品失败: productId={}, userId={}, error={}",
                     id, currentUser.getUserId(), e.getMessage());
            return Result.fail(500, "上架商品失败: " + e.getMessage());
        }
    }

    /**
     * 下架商品
     * POST /products/{id}/delist
     * <p>
     * 认证方式：需要商家或管理员权限
     * 防御措施：
     * 1. 商品存在性验证
     * 2. 资源所有权验证
     * 3. 状态转换合法性验证
     *
     * @param id 商品ID
     * @param currentUser 当前登录用户
     * @return 操作结果
     */
    @PostMapping("/{id}/delist")
    public Result<Void> delistProduct(@PathVariable Long id,
                                      @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("下架商品: productId={}, userId={}", id, currentUser.getUserId());

            productService.delistProduct(id, currentUser.getUserId());
            return Result.ok();

        } catch (Exception e) {
            log.error("下架商品失败: productId={}, userId={}, error={}",
                     id, currentUser.getUserId(), e.getMessage());
            return Result.fail(500, "下架商品失败: " + e.getMessage());
        }
    }

    /**
     * 上传商品图片
     * POST /products/{id}/images
     * <p>
     * 认证方式：需要商家或管理员权限
     * 防御措施：
     * 1. 商品存在性验证
     * 2. 资源所有权验证
     * 3. 图片URL安全性验证
     * 4. 文件大小限制
     *
     * @param id 商品ID
     * @param request 图片上传请求
     * @param currentUser 当前登录用户
     * @return 操作结果
     */
    @PostMapping("/{id}/images")
    public Result<Void> uploadImage(@PathVariable Long id,
                                    @Valid @RequestBody ProductImageUploadRequest request,
                                    @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("上传商品图片: productId={}, userId={}, imageUrl={}",
                     id, currentUser.getUserId(), request.getImageUrl());

            productService.uploadImage(id, request, currentUser.getUserId());
            return Result.ok();

        } catch (Exception e) {
            log.error("上传商品图片失败: productId={}, userId={}, error={}",
                     id, currentUser.getUserId(), e.getMessage());
            return Result.fail(500, "上传商品图片失败: " + e.getMessage());
        }
    }
}
