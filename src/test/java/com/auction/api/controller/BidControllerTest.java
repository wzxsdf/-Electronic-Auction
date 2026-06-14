package com.auction.api.controller;

import com.auction.api.dto.request.PlaceBidRequest;
import com.auction.api.dto.response.BidResultResponse;
import com.auction.common.Result;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.AuctionItem;
import com.auction.domain.entity.Product;
import com.auction.domain.entity.User;
import com.auction.domain.enums.AuctionStatus;
import com.auction.domain.enums.ProductStatus;
import com.auction.infrastructure.security.UserPrincipal;
import com.auction.repository.AuctionItemRepository;
import com.auction.repository.AuctionRepository;
import com.auction.repository.ProductRepository;
import com.auction.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BidController功能测试
 * 测试完整出价流程和API兼容性
 */
@Slf4j
@SpringBootTest
@Transactional
public class BidControllerTest {

    @Autowired
    private BidController bidController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionItemRepository auctionItemRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Auction testAuction;
    private AuctionItem testAuctionItem;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    public void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("bidder_test");
        testUser.setNickname("测试出价者");
        testUser.setEmail("bidder@test.com");
        testUser.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMye/0VsIhHcjF/iLJp8PYG9P0BqLquN8Ea"); // 加密密码
        testUser.setStatus("ACTIVE");
        testUser = userRepository.save(testUser);

        testUserPrincipal = new UserPrincipal(
                testUser.getId(),
                testUser.getUsername(),
                "ROLE_USER"
        );

        // 创建测试拍卖活动
        testAuction = new Auction();
        testAuction.setTitle("测试拍卖活动");
        testAuction.setDescription("测试用拍卖活动");
        testAuction.setHostId(testUser.getId());
        testAuction.setStartTime(LocalDateTime.now().minusHours(1));
        testAuction.setEndTime(LocalDateTime.now().plusHours(2));
        testAuction.setStatusEnum(AuctionStatus.ACTIVE);
        testAuction = auctionRepository.save(testAuction);

        // 创建测试商品
        Product testProduct = new Product();
        testProduct.setName("测试商品");
        testProduct.setDescription("测试用商品");
        testProduct.setInitialPrice(new BigDecimal("100.00"));
        testProduct.setBidIncrement(new BigDecimal("10.00"));
        testProduct.setMaxPrice(new BigDecimal("1000.00"));
        testProduct.setStatus(ProductStatus.LISTED);
        testProduct.setMerchantId(testUser.getId());
        testProduct = productRepository.save(testProduct);

        // 创建测试拍品
        testAuctionItem = new AuctionItem();
        testAuctionItem.setAuctionId(testAuction.getId());
        testAuctionItem.setProductId(testProduct.getId());
        testAuctionItem.setTitle("测试拍品");
        testAuctionItem.setStartPrice(new BigDecimal("100.00"));
        testAuctionItem.setCurrentPrice(new BigDecimal("100.00"));
        testAuctionItem.setBidIncrement(new BigDecimal("10.00"));
        testAuctionItem.setMaxPrice(new BigDecimal("1000.00"));
        testAuctionItem.setDelaySeconds(300);
        testAuctionItem.setStartTime(LocalDateTime.now().minusHours(1));
        testAuctionItem.setEndTime(LocalDateTime.now().plusHours(2));
        testAuctionItem.setStatusEnum(AuctionStatus.ACTIVE);
        testAuctionItem.setBidCount(0);
        testAuctionItem = auctionItemRepository.save(testAuctionItem);
    }

    /**
     * 测试新版API：使用auctionItemId出价
     */
    @Test
    public void testPlaceBidWithAuctionItemId() {
        log.info("测试新版API：使用auctionItemId出价");

        PlaceBidRequest request = new PlaceBidRequest();
        request.setAuctionId(testAuction.getId());
        request.setAuctionItemId(testAuctionItem.getId());
        request.setUserId(testUser.getId());
        request.setAmount(new BigDecimal("150.00"));
        request.setIsAutoBid(false);

        Result<BidResultResponse> result = bidController.placeBid(request, testUserPrincipal);

        assertNotNull(result, "出价结果不应为null");
        assertEquals(200, result.getCode(), "出价应该成功");
        assertNotNull(result.getData(), "出价结果数据不应为null");

        BidResultResponse response = result.getData();
        assertNotNull(response.getBidId(), "出价记录ID不应为null");
        assertEquals(new BigDecimal("150.00"), response.getCurrentPrice(), "当前价格应该是150.00");
        assertEquals(1, response.getYourRank(), "用户排名应该是第1");
        assertTrue(response.getIsLeading(), "用户应该是领先者");
        assertEquals("出价成功", response.getMessage(), "消息应该是'出价成功'");

        log.info("出价成功: bidId={}, currentPrice={}, rank={}, isLeading={}",
                response.getBidId(), response.getCurrentPrice(), response.getYourRank(), response.getIsLeading());
    }

    /**
     * 测试旧版API兼容性：仅使用auctionId出价
     */
    @Test
    public void testPlaceBidWithAuctionIdOnly() {
        log.info("测试旧版API兼容性：仅使用auctionId出价");

        PlaceBidRequest request = new PlaceBidRequest();
        request.setAuctionId(testAuction.getId());
        request.setUserId(testUser.getId());
        request.setAmount(new BigDecimal("120.00"));
        request.setIsAutoBid(false);
        // 不设置auctionItemId，测试自动查找活跃拍品

        Result<BidResultResponse> result = bidController.placeBid(request, testUserPrincipal);

        assertNotNull(result, "出价结果不应为null");
        assertEquals(200, result.getCode(), "出价应该成功");
        assertNotNull(result.getData(), "出价结果数据不应为null");

        BidResultResponse response = result.getData();
        assertNotNull(response.getBidId(), "出价记录ID不应为null");
        assertEquals(new BigDecimal("120.00"), response.getCurrentPrice(), "当前价格应该是120.00");
        assertEquals(1, response.getYourRank(), "用户排名应该是第1");

        log.info("旧版API兼容测试成功: bidId={}, currentPrice={}",
                response.getBidId(), response.getCurrentPrice());
    }

    /**
     * 测试出价金额验证：低于当前价格+加价幅度应该失败
     */
    @Test
    public void testBidAmountTooLow() {
        log.info("测试出价金额验证");

        // 第一次出价成功
        testAuctionItem.setCurrentPrice(new BigDecimal("150.00"));
        testAuctionItem.setHighestBidder(testUser.getId());
        auctionItemRepository.save(testAuctionItem);

        // 创建第二个用户
        User user2 = new User();
        user2.setUsername("bidder2");
        user2.setNickname("出价者2");
        user2.setEmail("bidder2@test.com");
        user2.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMye/0VsIhHcjF/iLJp8PYG9P0BqLquN8Ea");
        user2.setStatus("ACTIVE");
        user2 = userRepository.save(user2);

        UserPrincipal user2Principal = new UserPrincipal(
                user2.getId(),
                user2.getUsername(),
                "ROLE_USER"
        );

        // 尝试用低于最小加价幅度的价格出价
        PlaceBidRequest request = new PlaceBidRequest();
        request.setAuctionId(testAuction.getId());
        request.setAuctionItemId(testAuctionItem.getId());
        request.setUserId(user2.getId());
        request.setAmount(new BigDecimal("155.00")); // 应该至少是160.00 (150.00 + 10.00)
        request.setIsAutoBid(false);

        Result<BidResultResponse> result = bidController.placeBid(request, user2Principal);

        assertNotNull(result, "出价结果不应为null");
        assertNotEquals(200, result.getCode(), "出价应该失败");
        assertTrue(result.getMessage().contains("出价必须 >= 当前价 + 加价幅度"),
                "错误消息应该包含价格验证信息");

        log.info("价格验证测试成功: {}", result.getMessage());
    }

    /**
     * 测试多个用户出价排名计算
     */
    @Test
    public void testMultipleUsersRanking() {
        log.info("测试多用户出价排名计算");

        // 创建多个测试用户
        User[] users = new User[5];
        UserPrincipal[] userPrincipals = new UserPrincipal[5];

        for (int i = 0; i < 5; i++) {
            users[i] = new User();
            users[i].setUsername("bidder" + (i + 1));
            users[i].setNickname("出价者" + (i + 1));
            users[i].setEmail("bidder" + (i + 1) + "@test.com");
            users[i].setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMye/0VsIhHcjF/iLJp8PYG9P0BqLquN8Ea");
            users[i].setStatus("ACTIVE");
            users[i] = userRepository.save(users[i]);

            userPrincipals[i] = new UserPrincipal(
                    users[i].getId(),
                    users[i].getUsername(),
                    "ROLE_USER"
            );
        }

        // 按顺序出价，每次递增
        BigDecimal[] bidAmounts = {
                new BigDecimal("150.00"),
                new BigDecimal("200.00"),
                new BigDecimal("180.00"),
                new BigDecimal("250.00"),
                new BigDecimal("300.00")
        };

        Integer[] expectedRanks = {4, 3, 5, 2, 1}; // 根据出价金额预期的排名

        for (int i = 0; i < 5; i++) {
            PlaceBidRequest request = new PlaceBidRequest();
            request.setAuctionId(testAuction.getId());
            request.setAuctionItemId(testAuctionItem.getId());
            request.setUserId(users[i].getId());
            request.setAmount(bidAmounts[i]);
            request.setIsAutoBid(false);

            Result<BidResultResponse> result = bidController.placeBid(request, userPrincipals[i]);

            assertEquals(200, result.getCode(), "用户" + (i + 1) + "出价应该成功");

            BidResultResponse response = result.getData();
            log.info("用户{} 出价 {} 元, 当前排名: {}, 是否领先: {}",
                    users[i].getUsername(), bidAmounts[i], response.getYourRank(), response.getIsLeading());

            // 注意：这里的排名计算基于当前时刻的最高出价
            // 最后一个出价300元的人应该是第1名
            if (i == 4) {
                assertEquals(1, response.getYourRank(), "最后一个出价者应该是第1名");
                assertTrue(response.getIsLeading(), "最后一个出价者应该领先");
            }
        }

        log.info("多用户排名测试完成");
    }

    /**
     * 测试缺少必要参数的错误处理
     */
    @Test
    public void testMissingParameters() {
        log.info("测试缺少必要参数的错误处理");

        PlaceBidRequest request = new PlaceBidRequest();
        // 不设置auctionId和auctionItemId
        request.setUserId(testUser.getId());
        request.setAmount(new BigDecimal("150.00"));

        Result<BidResultResponse> result = bidController.placeBid(request, testUserPrincipal);

        assertNotNull(result, "出价结果不应为null");
        assertNotEquals(200, result.getCode(), "缺少参数时出价应该失败");
        assertTrue(result.getMessage().contains("请指定auctionItemId或auctionId参数"),
                "错误消息应该提示缺少必要参数");

        log.info("参数验证测试成功: {}", result.getMessage());
    }

    /**
     * 测试活动无活跃拍品的场景
     */
    @Test
    public void testNoActiveItems() {
        log.info("测试活动无活跃拍品的场景");

        // 创建一个没有活跃拍品的活动
        Auction emptyAuction = new Auction();
        emptyAuction.setTitle("空活动");
        emptyAuction.setDescription("没有活跃拍品的活动");
        emptyAuction.setHostId(testUser.getId());
        emptyAuction.setStartTime(LocalDateTime.now().minusHours(1));
        emptyAuction.setEndTime(LocalDateTime.now().plusHours(2));
        emptyAuction.setStatusEnum(AuctionStatus.ACTIVE);
        emptyAuction = auctionRepository.save(emptyAuction);

        PlaceBidRequest request = new PlaceBidRequest();
        request.setAuctionId(emptyAuction.getId());
        request.setUserId(testUser.getId());
        request.setAmount(new BigDecimal("150.00"));
        // 不设置auctionItemId

        Result<BidResultResponse> result = bidController.placeBid(request, testUserPrincipal);

        assertNotNull(result, "出价结果不应为null");
        assertNotEquals(200, result.getCode(), "无活跃拍品时出价应该失败");
        assertTrue(result.getMessage().contains("没有活跃的拍品"),
                "错误消息应该提示无活跃拍品");

        log.info("无活跃拍品测试成功: {}", result.getMessage());
    }

    /**
     * 测试封顶价自动成交功能
     */
    @Test
    public void testMaxPriceAutoTransaction() {
        log.info("测试封顶价自动成交功能");

        // 创建一个测试商品
        Product maxPriceProduct = new Product();
        maxPriceProduct.setName("封顶价测试商品");
        maxPriceProduct.setDescription("封顶价测试用商品");
        maxPriceProduct.setInitialPrice(new BigDecimal("100.00"));
        maxPriceProduct.setBidIncrement(new BigDecimal("50.00"));
        maxPriceProduct.setMaxPrice(new BigDecimal("500.00"));
        maxPriceProduct.setStatus(ProductStatus.LISTED);
        maxPriceProduct.setMerchantId(testUser.getId());
        maxPriceProduct = productRepository.save(maxPriceProduct);

        // 创建一个测试拍品，设置封顶价为500元
        AuctionItem maxPriceItem = new AuctionItem();
        maxPriceItem.setAuctionId(testAuction.getId());
        maxPriceItem.setProductId(maxPriceProduct.getId());
        maxPriceItem.setTitle("封顶价测试拍品");
        maxPriceItem.setStartPrice(new BigDecimal("100.00"));
        maxPriceItem.setCurrentPrice(new BigDecimal("400.00"));
        maxPriceItem.setBidIncrement(new BigDecimal("50.00"));
        maxPriceItem.setMaxPrice(new BigDecimal("500.00")); // 封顶价500元
        maxPriceItem.setDelaySeconds(300);
        maxPriceItem.setStartTime(LocalDateTime.now().minusHours(1));
        maxPriceItem.setEndTime(LocalDateTime.now().plusHours(2));
        maxPriceItem.setStatusEnum(AuctionStatus.ACTIVE);
        maxPriceItem.setBidCount(0);
        maxPriceItem = auctionItemRepository.save(maxPriceItem);

        // 出价达到封顶价500元
        PlaceBidRequest request = new PlaceBidRequest();
        request.setAuctionId(testAuction.getId());
        request.setAuctionItemId(maxPriceItem.getId());
        request.setUserId(testUser.getId());
        request.setAmount(new BigDecimal("500.00")); // 正好等于封顶价
        request.setIsAutoBid(false);

        Result<BidResultResponse> result = bidController.placeBid(request, testUserPrincipal);

        assertNotNull(result, "出价结果不应为null");
        assertEquals(200, result.getCode(), "达到封顶价出价应该成功");

        BidResultResponse response = result.getData();
        assertNotNull(response, "出价结果数据不应为null");
        assertEquals(new BigDecimal("500.00"), response.getCurrentPrice(), "当前价格应该是500.00");
        assertTrue(response.getMaxPriceReached(), "应该标记为达到封顶价");
        assertTrue(response.getMessage().contains("自动成交") ||
                  response.getMessage().contains("封顶价"),
                  "消息应该包含自动成交信息");

        log.info("封顶价成交测试成功: currentPrice={}, maxPriceReached={}, message={}",
                response.getCurrentPrice(), response.getMaxPriceReached(), response.getMessage());
    }

    /**
     * 测试出价超过封顶价应该失败
     */
    @Test
    public void testBidExceedMaxPrice() {
        log.info("测试出价超过封顶价应该失败");

        // 创建一个测试商品
        Product exceedPriceProduct = new Product();
        exceedPriceProduct.setName("超过封顶价测试商品");
        exceedPriceProduct.setDescription("超过封顶价测试用商品");
        exceedPriceProduct.setInitialPrice(new BigDecimal("100.00"));
        exceedPriceProduct.setBidIncrement(new BigDecimal("50.00"));
        exceedPriceProduct.setMaxPrice(new BigDecimal("300.00"));
        exceedPriceProduct.setStatus(ProductStatus.LISTED);
        exceedPriceProduct.setMerchantId(testUser.getId());
        exceedPriceProduct = productRepository.save(exceedPriceProduct);

        // 创建一个测试拍品，设置封顶价为300元
        AuctionItem maxPriceItem = new AuctionItem();
        maxPriceItem.setAuctionId(testAuction.getId());
        maxPriceItem.setProductId(exceedPriceProduct.getId());
        maxPriceItem.setTitle("封顶价限制测试拍品");
        maxPriceItem.setStartPrice(new BigDecimal("100.00"));
        maxPriceItem.setCurrentPrice(new BigDecimal("200.00"));
        maxPriceItem.setBidIncrement(new BigDecimal("50.00"));
        maxPriceItem.setMaxPrice(new BigDecimal("300.00")); // 封顶价300元
        maxPriceItem.setDelaySeconds(300);
        maxPriceItem.setStartTime(LocalDateTime.now().minusHours(1));
        maxPriceItem.setEndTime(LocalDateTime.now().plusHours(2));
        maxPriceItem.setStatusEnum(AuctionStatus.ACTIVE);
        maxPriceItem.setBidCount(0);
        maxPriceItem = auctionItemRepository.save(maxPriceItem);

        // 尝试出价超过封顶价（350元 > 300元）
        PlaceBidRequest request = new PlaceBidRequest();
        request.setAuctionId(testAuction.getId());
        request.setAuctionItemId(maxPriceItem.getId());
        request.setUserId(testUser.getId());
        request.setAmount(new BigDecimal("350.00")); // 超过封顶价
        request.setIsAutoBid(false);

        Result<BidResultResponse> result = bidController.placeBid(request, testUserPrincipal);

        assertNotNull(result, "出价结果不应为null");
        assertNotEquals(200, result.getCode(), "超过封顶价的出价应该失败");
        assertTrue(result.getMessage().contains("封顶价") ||
                  result.getMessage().contains("超过"),
                  "错误消息应该提示超过封顶价");

        log.info("超过封顶价测试成功: {}", result.getMessage());
    }

    /**
     * 测试出价低于封顶价正常成交
     */
    @Test
    public void testBidBelowMaxPrice() {
        log.info("测试出价低于封顶价正常成交");

        // 创建一个测试商品
        Product belowPriceProduct = new Product();
        belowPriceProduct.setName("低于封顶价测试商品");
        belowPriceProduct.setDescription("低于封顶价测试用商品");
        belowPriceProduct.setInitialPrice(new BigDecimal("100.00"));
        belowPriceProduct.setBidIncrement(new BigDecimal("50.00"));
        belowPriceProduct.setMaxPrice(new BigDecimal("800.00"));
        belowPriceProduct.setStatus(ProductStatus.LISTED);
        belowPriceProduct.setMerchantId(testUser.getId());
        belowPriceProduct = productRepository.save(belowPriceProduct);

        // 创建一个测试拍品，设置封顶价为800元
        AuctionItem maxPriceItem = new AuctionItem();
        maxPriceItem.setAuctionId(testAuction.getId());
        maxPriceItem.setProductId(belowPriceProduct.getId());
        maxPriceItem.setTitle("低于封顶价测试拍品");
        maxPriceItem.setStartPrice(new BigDecimal("100.00"));
        maxPriceItem.setCurrentPrice(new BigDecimal("400.00"));
        maxPriceItem.setBidIncrement(new BigDecimal("50.00"));
        maxPriceItem.setMaxPrice(new BigDecimal("800.00")); // 封顶价800元
        maxPriceItem.setDelaySeconds(300);
        maxPriceItem.setStartTime(LocalDateTime.now().minusHours(1));
        maxPriceItem.setEndTime(LocalDateTime.now().plusHours(2));
        maxPriceItem.setStatusEnum(AuctionStatus.ACTIVE);
        maxPriceItem.setBidCount(0);
        maxPriceItem = auctionItemRepository.save(maxPriceItem);

        // 出价600元（低于封顶价800元）
        PlaceBidRequest request = new PlaceBidRequest();
        request.setAuctionId(testAuction.getId());
        request.setAuctionItemId(maxPriceItem.getId());
        request.setUserId(testUser.getId());
        request.setAmount(new BigDecimal("600.00")); // 低于封顶价
        request.setIsAutoBid(false);

        Result<BidResultResponse> result = bidController.placeBid(request, testUserPrincipal);

        assertNotNull(result, "出价结果不应为null");
        assertEquals(200, result.getCode(), "低于封顶价的出价应该成功");

        BidResultResponse response = result.getData();
        assertNotNull(response, "出价结果数据不应为null");
        assertEquals(new BigDecimal("600.00"), response.getCurrentPrice(), "当前价格应该是600.00");
        assertFalse(response.getMaxPriceReached(), "不应该标记为达到封顶价");
        assertEquals("出价成功", response.getMessage(), "消息应该是'出价成功'");

        log.info("低于封顶价测试成功: currentPrice={}, maxPriceReached={}",
                response.getCurrentPrice(), response.getMaxPriceReached());
    }
}