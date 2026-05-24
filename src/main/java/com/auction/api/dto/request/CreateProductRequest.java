package com.auction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProductRequest {
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称长度不能超过200")
    private String name;

    private String imageUrl;
    private String description;
    @Size(max = 50, message = "分类长度不能超过50")
    private String category;
}
