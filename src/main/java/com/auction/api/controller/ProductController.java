package com.auction.api.controller;

import com.auction.api.dto.request.CreateProductRequest;
import com.auction.api.dto.response.ProductResponse;
import com.auction.common.Result;
import com.auction.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器：管理拍卖商品信息，提供商品创建、查询等功能
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 创建商品：验证商品信息 → 保存商品数据 → 返回包含ID的商品信息
     */
    @PostMapping
    public Result<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return Result.ok(productService.create(request));
    }

    /**
     * 查询商品详情：根据ID获取商品的完整信息（名称、图片、描述、分类）
     */
    @GetMapping("/{id}")
    public Result<ProductResponse> getById(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    /**
     * 查询所有商品：返回系统中所有可拍卖的商品列表
     */
    @GetMapping
    public Result<List<ProductResponse>> listAll() {
        return Result.ok(productService.listAll());
    }
}
