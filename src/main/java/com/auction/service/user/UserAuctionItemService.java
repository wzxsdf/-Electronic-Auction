package com.auction.service.user;

import com.auction.api.dto.response.UserAuctionItemResponse;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.AuctionItem;
import com.auction.domain.entity.Bid;
import com.auction.domain.entity.Product;
import com.auction.domain.enums.AuctionStatus;
import com.auction.repository.AuctionItemRepository;
import com.auction.repository.AuctionRepository;
import com.auction.repository.BidRepository;
import com.auction.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户参与拍品业务服务
 * <p>
 * 提供用户参与拍品查询业务逻辑，支持状态筛选、中标状态筛选
 * 关联查询拍品、商品、拍卖活动（直播间）信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuctionItemService {

    private final BidRepository bidRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final ProductRepository productRepository;
    private final AuctionRepository auctionRepository;

    /**
     * 查询用户参与拍品列表
     *
     * @param userId         用户ID
     * @param statusFilter   状态筛选（FINISHED/ONGOING/ALL）
     * @param winStatusFilter 中标状态筛选（WON/LOST/ALL）
     * @return 用户参与拍品响应列表
     */
    public List<UserAuctionItemResponse> getUserParticipatedItems(
            Long userId,
            String statusFilter,
            String winStatusFilter
    ) {
        log.info("查询用户参与拍品: userId={}, statusFilter={}, winStatusFilter={}",
                userId, statusFilter, winStatusFilter);

        // 1. 查询用户参与过的拍品ID列表
        List<Long> itemIds = bidRepository.findParticipatedItemIds(userId);

        if (itemIds.isEmpty()) {
            log.debug("用户未参与任何拍品: userId={}", userId);
            return Collections.emptyList();
        }

        log.debug("用户参与拍品数量: userId={}, count={}", userId, itemIds.size());

        // 2. 批量查询拍品详情
        List<AuctionItem> items = itemIds.stream()
                .map(auctionItemRepository::findById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3. 组装响应数据并应用筛选条件
        List<UserAuctionItemResponse> responses = items.stream()
                .map(item -> buildResponse(item, userId))
                .filter(response -> applyFilters(response, statusFilter, winStatusFilter))
                .sorted(Comparator.comparing(
                        UserAuctionItemResponse::getLastBidTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());

        log.info("查询用户参与拍品成功: userId={}, resultCount={}", userId, responses.size());
        return responses;
    }

    /**
     * 构建单个响应对象
     *
     * @param item  拍品实体
     * @param userId 用户ID
     * @return 用户参与拍品响应
     */
    private UserAuctionItemResponse buildResponse(AuctionItem item, Long userId) {
        // 查询关联实体
        Product product = productRepository.findById(item.getProductId());
        Auction auction = auctionRepository.findById(item.getAuctionId());

        // 统计用户出价信息
        Long bidCount = bidRepository.countBidByItemIdAndUserId(item.getId(), userId);
        Bid highestBid = bidRepository.findHighestBidByItemIdAndUserId(item.getId(), userId);

        // 计算状态标识
        AuctionStatus statusEnum = item.getStatusEnum();
        boolean isFinished = statusEnum != null && statusEnum != AuctionStatus.ACTIVE;
        boolean isWon = item.getHighestBidder() != null
                && item.getHighestBidder().equals(userId)
                && statusEnum == AuctionStatus.COMPLETED;
        boolean isLeading = item.getHighestBidder() != null
                && item.getHighestBidder().equals(userId)
                && statusEnum == AuctionStatus.ACTIVE;

        return UserAuctionItemResponse.builder()
                .auctionItemId(item.getId())
                .title(item.getTitle())
                .currentPrice(item.getCurrentPrice())
                .startPrice(item.getStartPrice())
                .status(statusEnum)
                .startTime(item.getStartTime())
                .endTime(item.getEndTime())
                .productId(item.getProductId())
                .productName(product != null ? product.getName() : null)
                .productImageUrl(product != null ? product.getPrimaryImageUrl() : null)
                .productDescription(product != null ? product.getDescription() : null)
                .auctionId(item.getAuctionId())
                .auctionTitle(auction != null ? auction.getTitle() : null)
                .auctionDescription(auction != null ? auction.getDescription() : null)
                .isWon(isWon)
                .isFinished(isFinished)
                .yourBidCount(bidCount != null ? bidCount.intValue() : 0)
                .yourHighestBid(highestBid != null ? highestBid.getAmount() : null)
                .lastBidTime(highestBid != null ? highestBid.getCreatedAt() : null)
                .isLeading(isLeading)
                .build();
    }

    /**
     * 应用筛选条件
     *
     * @param response       用户参与拍品响应
     * @param statusFilter   状态筛选（FINISHED/ONGOING/ALL）
     * @param winStatusFilter 中标状态筛选（WON/LOST/ALL）
     * @return 是否符合筛选条件
     */
    private boolean applyFilters(UserAuctionItemResponse response,
                                 String statusFilter,
                                 String winStatusFilter) {
        // 状态筛选
        if ("FINISHED".equalsIgnoreCase(statusFilter) && !response.getIsFinished()) {
            return false;
        }
        if ("ONGOING".equalsIgnoreCase(statusFilter) && response.getIsFinished()) {
            return false;
        }

        // 中标状态筛选（仅在已结束时才有意义）
        if ("WON".equalsIgnoreCase(winStatusFilter) && !response.getIsWon()) {
            return false;
        }
        if ("LOST".equalsIgnoreCase(winStatusFilter) && response.getIsWon()) {
            return false;
        }

        return true;
    }
}
