package com.auction.api.dto.request;

import lombok.Data;

/**
 * 商品查询请求DTO
 */
@Data
public class ProductQueryRequest {

    /**
     * 关键词搜索（商品名称、描述、SKU）
     */
    private String keyword;

    /**
     * 商品分类
     */
    private String category;

    /**
     * 品牌筛选
     */
    private String brand;

    /**
     * 商品状态（如果是商家，可以查看所有状态；如果是普通用户，只能查看已上架）
     */
    private String status;

    /**
     * 商家ID筛选（管理员或用户查看特定商家的商品）
     */
    private Long merchantId;

    /**
     * 页码（从1开始）
     */
    private Integer page = 1;

    /**
     * 每页数量
     */
    private Integer size = 20;

    /**
     * 排序字段（createdAt, initialPrice, name）
     */
    private String sortBy = "createdAt";

    /**
     * 排序方向（asc, desc）
     */
    private String sortOrder = "desc";
}
