package com.auction.api.dto.request;

import com.auction.domain.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {
    /**
     * 商品编码（唯一标识）
     */
    @Size(max = 100, message = "商品编码长度不能超过100")
    private String sku;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称长度不能超过200")
    private String name;

    /**
     * 品牌
     */
    @Size(max = 100, message = "品牌长度不能超过100")
    private String brand;

    /**
     * 商品图片URL列表（可选）
     * 最多支持4张图片
     * 每个图片URL必须以 http:// 或 https:// 开头
     */
    @Size(max = 4, message = "最多只能上传4张图片")
    private List<String> imageUrls;

    private String description;

    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    /**
     * 初始价格（起拍价参考）
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
}
