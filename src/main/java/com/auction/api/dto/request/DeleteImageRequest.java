package com.auction.api.dto.request;

import lombok.Data;

/**
 * 删除图片请求DTO
 */
@Data
public class DeleteImageRequest {
    /**
     * 图片URL
     */
    private String imageUrl;
}
