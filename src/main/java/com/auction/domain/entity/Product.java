package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.ProductStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 * <p>
 * 存储商品的基本信息，包括名称、图片、描述、库存、价格等
 * 商品可以被多个竞拍项目关联使用
 */
@Data
@TableName("products")
public class Product {
    /**
     * 商品ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商家用户ID
     */
    private Long merchantId;

    /**
     * 商品编码（唯一标识）
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
     * 初始价格（用于设置竞拍起拍价参考）
     */
    private BigDecimal initialPrice;

    /**
     * 最低加价幅度
     */
    private BigDecimal bidIncrement;

    /**
     * 封顶价格（最高限价）
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 获取商品状态枚举
     */
    public ProductStatus getStatusEnum() {
        return status;
    }

    /**
     * 设置商品状态枚举
     */
    public void setStatusEnum(ProductStatus statusEnum) {
        this.status = statusEnum;
    }

    /**
     * 获取图片URL列表
     * 从逗号分隔的字符串中解析出图片URL列表
     * @return 图片URL列表
     */
    public java.util.List<String> getImageList() {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.asList(imageUrl.split(","));
    }

    /**
     * 设置图片URL列表
     * 将图片URL列表转换为逗号分隔的字符串存储
     * @param imageList 图片URL列表
     */
    public void setImageList(java.util.List<String> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            this.imageUrl = null;
        } else {
            // 去重并过滤空值，然后用逗号连接
            this.imageUrl = imageList.stream()
                .filter(url -> url != null && !url.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        }
    }

    /**
     * 获取主图URL（第一张图片）
     * @return 主图URL，如果没有图片则返回null
     */
    public String getPrimaryImageUrl() {
        java.util.List<String> images = getImageList();
        return images.isEmpty() ? null : images.get(0);
    }
}
