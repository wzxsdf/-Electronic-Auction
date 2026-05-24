package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("products")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String imageUrl;
    private String description;
    private String category;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
