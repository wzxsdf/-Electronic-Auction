package com.auction.api.dto.response;

import com.auction.domain.enums.ProductStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
     * 商家用户ID
     */
    private Long merchantId;

    /**
     * 商品编码
     */
    private String sku;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 商品图片URL（单个，用于向后兼容）
     */
    private String imageUrl;

    /**
     * 商品图片URL列表
     * 最多4张图片
     */
    private List<String> imageUrls;

    /**
     * 主图URL（第一张图片）
     */
    private String primaryImageUrl;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品分类
     */
    private String category;

    /**
     * 初始价格
     */
    private BigDecimal initialPrice;

    /**
     * 最低加价幅度
     */
    private BigDecimal bidIncrement;

    /**
     * 封顶价格
     */
    private BigDecimal maxPrice;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 商品状态
     */
    private ProductStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
