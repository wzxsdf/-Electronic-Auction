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

    /**
     * 创建新商品
     */
    @PostMapping
    public Result<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return Result.ok(productService.create(request));
    }

    /**
     * 根据ID查询商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductResponse> getById(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    /**
     * 查询所有商品列表
     */
    @GetMapping
    public Result<List<ProductResponse>> listAll() {
        return Result.ok(productService.listAll());
    }
}
