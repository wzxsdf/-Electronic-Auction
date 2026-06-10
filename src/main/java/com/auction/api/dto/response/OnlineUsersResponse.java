package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 在线用户响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUsersResponse {

    /**
     * 拍卖活动ID
     */
    private Long auctionId;

    /**
     * 在线用户总数
     */
    private Integer total;

    /**
     * 在线用户列表
     */
    private List<OnlineUser> users;

    /**
     * 在线用户信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnlineUser {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名
         */
        private String username;

        /**
         * 头像
         */
        private String avatar;

        /**
         * 加入时间
         */
        private String joinTime;
    }
}
