package com.auction.api.dto.request;

import com.auction.domain.enums.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 更新商品请求DTO
 * 所有字段均为可选，仅更新提供的字段
 */
@Data
public class UpdateProductRequest {

    /**
     * 商品编码（唯一标识）
     */
    @Size(max = 100, message = "商品编码长度不能超过100")
    private String sku;

    /**
     * 商品名称
     */
    @Size(max = 200, message = "商品名称长度不能超过200")
    private String name;

    /**
     * 品牌
     */
    @Size(max = 100, message = "品牌长度不能超过100")
    private String brand;

    /**
     * 商品图片URL（单个，向后兼容）
     */
    @Size(max = 500, message = "图片URL长度不能超过500")
    private String imageUrl;

    /**
     * 商品图片URL列表（可选）
     * 最多支持4张图片
     */
    @Size(max = 4, message = "最多只能上传4张图片")
    private List<String> imageUrls;

    /**
     * 商品描述
     */
    @Size(max = 2000, message = "商品描述长度不能超过2000")
    private String description;

    /**
     * 商品分类
     */
    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    /**
     * 初始价格（起拍价参考）
     */
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确")
    private BigDecimal initialPrice;

    /**
     * 最低加价幅度
     */
    @DecimalMin(value = "0.01", message = "加价幅度必须大于0")
    @Digits(integer = 8, fraction = 2, message = "加价幅度格式不正确")
    private BigDecimal bidIncrement;

    /**
     * 封顶价格（最高限价）
     */
    @DecimalMin(value = "0.01", message = "封顶价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "封顶价格格式不正确")
    private BigDecimal maxPrice;

    /**
     * 库存数量
     */
    @Min(value = 0, message = "库存不能为负数")
    @Max(value = 999999, message = "库存超出范围")
    private Integer stock;

    /**
     * 商品状态
     */
    private ProductStatus status;
}
