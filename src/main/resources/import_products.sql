-- 导入奢侈品商品数据
-- 注意：执行前请确保users表中存在id=1的商家用户

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO `products` (`merchant_id`, `sku`, `name`, `brand`, `image_url`, `description`, `category`, `initial_price`, `bid_increment`, `max_price`, `stock`, `status`) VALUES
(8, 'SP569390202615996416', '【UNIBUY】BURBERRY 黄色女士围巾 4076700', 'BURBERRY', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/pic2d911b68-0829-4fd6-a114-df7592a43cc9.GIF', NULL, '奢侈品/服饰配饰/围巾-丝巾-披肩/披肩', 1950.00, 50.00, 4700.00, 1, 'PENDING_REVIEW'),

(8, 'SP569390198406799360', '【UNIBUY】GUCCI 淡粉色女士围巾 165904-3G646-6900', '古驰/GUCCI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/pice63b8f92-5c68-44ef-8e86-56efcfcc4b07.GIF', '羊毛70%丝绸30%', '奢侈品/服饰配饰/围巾-丝巾-披肩/披肩', 1590.00, 50.00, 2800.00, 1, 'PENDING_REVIEW'),

(8, 'SP569390197406670848', '【UNIBUY】BURBERRY 拼色女士围巾 8015550', 'BURBERRY', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/piccc59032d-91a7-452c-82ef-8ccb6e35faf3.GIF', NULL, '奢侈品/服饰配饰/围巾-丝巾-披肩/披肩', 3420.00, 100.00, 5100.00, 2, 'PENDING_REVIEW'),

(8, 'SP569390113645477888', '【UNIBUY】EMPORIO ARMANI男士腰带 YEM909-YCB91-88001', '阿玛尼/GIORGIO ARMANI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/pic75abf1d7-96fc-4766-9c82-bbd970967591.GIF', NULL, '奢侈品/服饰配饰/男士腰带/男士腰带', 790.00, 30.00, 2190.00, 1, 'PENDING_REVIEW'),

(8, 'SP569390106906173440', '【UNIBUY】GUCCI 黑色女士围巾 165904-3G646-1000', '古驰/GUCCI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/picaa016e79-93d2-4bbf-a8e0-36b8bcd8a6dd.GIF', '羊毛70%丝绸30%', '奢侈品/服饰配饰/围巾-丝巾-披肩/披肩', 1590.00, 50.00, 2800.00, 1, 'PENDING_REVIEW'),

(8, 'SP569390100102070272', '【UNIBUY】GUCCI黑色双G压纹商务休闲男士腰带 411924-CWC1N-1000', '古驰/GUCCI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/pic87aff8dd-8bee-4455-b1b3-ddf6bb7937b5.GIF', NULL, '奢侈品/服饰配饰/男士腰带/男士腰带', 3160.00, 100.00, 4500.00, 1, 'PENDING_REVIEW'),

(8, 'SP569390072549687296', '【UNIBUY】ZEGNA 黑色男士腰带 LHAVM-B007JZ-TBN', '杰尼亚/Zegna', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/pic93c03205-06ca-4a9b-8cf9-480a29010950.GIF', NULL, '奢侈品/服饰配饰/男士腰带/男士腰带', 1700.00, 50.00, 3880.00, 1, 'PENDING_REVIEW'),

(8, 'SP569390067028615168', '【UNIBUY】BURBERRY 拼色男士腰带 8052779', 'BURBERRY', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/pic69113528-9009-473e-a9f8-b921b1f16b6c.GIF', '75%聚氨酯 25%棉', '奢侈品/服饰配饰/男士腰带/男士腰带', 1850.00, 50.00, 4500.00, 1, 'PENDING_REVIEW'),

(8, 'SP569390037492326400', '【UNIBUY】BURBERRY 红色女士围巾 3993742', 'BURBERRY', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/picb62560c4-8545-431c-8f58-263eb0a7da40.GIF', '羊绒', '奢侈品/服饰配饰/围巾-丝巾-披肩/披肩', 1780.00, 50.00, 3900.00, 1, 'PENDING_REVIEW'),

(8, 'SP569390022813630464', '【UNIBUY】GUCCI 黑色男士腰带 703147-US10N-1000', '古驰/GUCCI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/pic54a6718f-4d8b-48ef-b354-5f0ba7c34f0b.GIF', NULL, '奢侈品/服饰配饰/男士腰带/男士腰带', 3800.00, 100.00, 4500.00, 3, 'PENDING_REVIEW'),

(8, 'SP569390143258206208', '【UNIBUY】BVLGARI 女士时尚项链 356910 【仅支持香港自提】', '宝格丽/BVLGARI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/picc94ceed3-5e44-4eaa-b71f-6f15a0e49412.GIF', NULL, '奢侈品/时尚饰品/项链/项坠-吊坠', 4300.00, 100.00, 6400.00, 1, 'PENDING_REVIEW'),

(8, 'SP569390041472720896', '【UNIBUY】SWAROVSKI 黑色女士项链 5429737', '施华洛世奇/SWAROVSKI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/picf1898b6d-4f61-406a-af1a-287b04d886be.GIF', NULL, '奢侈品/时尚饰品/项链/项坠-吊坠', 310.00, 10.00, 999.00, 1, 'PENDING_REVIEW'),

(8, 'SP569389841609940992', '【UNIBUY】SWAROVSKI 女士银色仿水晶项链 5511041', '施华洛世奇/SWAROVSKI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/picf820b6c0-0c58-406a-833c-bfbcf22d0f3a.GIF', '仿水晶', '奢侈品/时尚饰品/项链/项坠-吊坠', 300.00, 10.00, 800.00, 1, 'PENDING_REVIEW'),

(8, 'SP569389734019964928', '【UNIBUY】Swarovski 施华洛世奇 银色女士项链 5563897', '施华洛世奇/SWAROVSKI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/pice7aa54f2-204d-478f-9243-b1c2e1ea8c94.GIF', NULL, '奢侈品/时尚饰品/项链/项坠-吊坠', 310.00, 10.00, 999.00, 1, 'PENDING_REVIEW'),

(8, 'SP569389679326240768', '【UNIBUY】AMBUSH 男士金色项链 BMOB018-F20MET001-7610', 'AMBUSH', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/picfd83c083-bc39-4087-8d81-df5c4591c3f2.GIF', NULL, '奢侈品/时尚饰品/项链/项坠-吊坠', 1780.00, 50.00, 3500.00, 1, 'PENDING_REVIEW'),

(8, 'SP569389470414036992', '【UNIBUY】GUCCI 女士银色纯银项链 YBB627757001', '古驰/GUCCI', 'https://ywwl-supply-img.oss-cn-hangzhou.aliyuncs.com/ywwl-supply-chain/2024/01/pic499f25cc-f568-4d63-b1fd-16f9d733ef7e.GIF', NULL, '奢侈品/时尚饰品/项链/项坠-吊坠', 1650.00, 50.00, 2400.00, 1, 'PENDING_REVIEW');

SET FOREIGN_KEY_CHECKS = 1;

-- 导入说明：
-- 1. merchant_id 设置为 1，请确保users表中存在id=1的商家用户
-- 2. sku 映射自 spuUniqueNo（商品唯一编码）
-- 3. initial_price 映射自 livePriceRange（直播价格，作为起拍价参考）
-- 4. max_price 映射自 weiMarketPrice（市场价，作为最高限价）
-- 5. bid_increment 根据价格区间设置合理的加价幅度
-- 6. category 映射自 categoryNamePath（分类路径）
-- 7. image_url 取 imageList 的第一张图片
-- 8. description 包含材质信息（outMaterial字段）
-- 9. status 默认为 'PENDING_REVIEW'（待审核状态）
-- 10. stock 映射自 stock 字段

-- 如需修改商家ID，请批量替换 merchant_id 值
-- 如需修改商品状态，可将 'PENDING_REVIEW' 改为：
--   - 'LISTED'（已上架）
--   - 'DELISTED'（已下架）
--   - 'SOLD_OUT'（已售罄）
