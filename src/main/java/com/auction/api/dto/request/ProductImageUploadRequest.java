package com.auction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品图片上传请求DTO
 */
@Data
public class ProductImageUploadRequest {

    /**
     * 图片URL（实际项目中应该是上传文件后返回的URL）
     */
    @NotBlank(message = "图片URL不能为空")
    @Size(max = 500, message = "图片URL长度不能超过500")
    private String imageUrl;

    /**
     * 是否设为主图
     */
    private Boolean isPrimary = false;
}
