package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注状态响应
 * <p>
 * 返回用户对特定活动的关注状态和统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatusResponse {
    /**
     * 是否关注
     */
    private Boolean following;

    /**
     * 关注时间
     */
    private String followedAt;

    /**
     * 关注人数统计
     */
    private Integer totalFollowers;
}