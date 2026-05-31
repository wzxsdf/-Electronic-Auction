package com.auction.api.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品信息响应DTO
 * 用于返回商品基本信息
 */
@Data
public class ProductResponse {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品图片URL
     */
    private String imageUrl;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品分类
     */
    private String category;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
