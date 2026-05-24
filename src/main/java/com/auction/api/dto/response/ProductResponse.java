package com.auction.api.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private String description;
    private String category;
    private LocalDateTime createdAt;
}
