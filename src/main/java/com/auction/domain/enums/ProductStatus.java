package com.auction.domain.enums;

/**
 * 商品状态枚举
 */
public enum ProductStatus {
    /**
     * 待审核
     */
    PENDING_REVIEW,

    /**
     * 已上架
     */
    LISTED,

    /**
     * 已下架
     */
    DELISTED,

    /**
     * 已售罄
     */
    SOLD_OUT
}
