-- 导入珠宝类商品数据
-- 注意：执行前请确保users表中存在id=8的商家用户

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO `products` (`merchant_id`, `sku`, `name`, `brand`, `image_url`, `description`, `category`, `initial_price`, `bid_increment`, `max_price`, `stock`, `status`) VALUES
(8, 'SP650548853693792256', '【金乌兆】海月珠宝 天然金发晶红玛瑙长款水晶佛珠叠戴手链项链', '春江海月', 'https://p3-aio.ecombdimg.com/obj/ecom-shop-material/mKEtnMEv_m_d3a538de6127292be4064ec87eda7138_sx_2201781_www4000-4000', NULL, '珠宝/饰品/手链/项链', 388.00, 20.00, 776.00, 1, 'PENDING_REVIEW'),

(8, 'SP650548879828500480', '【海棠玉】海月珠宝 铜钱貔貅天然白水晶钻切水晶手串', '春江海月', 'https://p3-aio.ecombdimg.com/obj/ecom-shop-material/mKEtnMEv_m_aaae86aaa4273fbeb8ff10ddc87228d8_sx_399546_www886-886', NULL, '珠宝/饰品/手串/水晶', 188.00, 10.00, 376.00, 1, 'PENDING_REVIEW'),

(8, 'SP650550540805644288', '【扶摇上】海月珠宝 天然黄水晶扎基拉姆紫水晶手串', '春江海月', 'https://p9-aio.ecombdimg.com/obj/ecom-shop-material/mKEtnMEv_m_6c25301c4f0a9c2bc189fe0cf21f1995_sx_955951_www4032-4032', NULL, '珠宝/饰品/手串/水晶', 288.00, 15.00, 576.00, 1, 'PENDING_REVIEW'),

(8, 'SP567545496016683008', '龙年新款元旦装饰横彩门帘拉旗商场珠宝店春节过年装扮', '晟洁馨宝', 'https://p3-aio.ecombdimg.com/obj/ecom-shop-material/SMMZEzEv_m_8edb33650b7e0f5182f831af6a88be1e_sx_257923_www1000-1000', NULL, '装饰用品/节庆用品/春节装饰', 8.80, 1.00, 17.60, 1, 'PENDING_REVIEW'),

(8, 'SP635647343518818304', '黑花梨木串 WBX0630088 多样性发一件', '小刘珠宝', 'https://p3-aio.ecombdimg.com/obj/ecom-shop-material/WZbvSdhG_m_97d031b73e5adb332768a7727be34175_sx_465373_www800-800', NULL, '珠宝/饰品/手串/木质', 19.90, 1.00, 39.80, 1, 'PENDING_REVIEW'),

(8, 'SP629886732184203264', '黑曜石手镯小刘珠宝【七爷直播间】冰曜石叮当镯', '小刘珠宝', 'https://u2-203.ecukwai.com/bs2/image-kwaishop-product/ITEM_IMAGE-3714133893-f8ab87eb58cc40fe9c78def1609f88bf.jpg', NULL, '珠宝/饰品/手镯/黑曜石', 99999.00, 1000.00, 199998.00, 1, 'PENDING_REVIEW'),

(8, 'SP635376060151115776', '黄金虎眼DIY手链 WBX0630310', '小刘珠宝', 'https://p3-aio.ecombdimg.com/obj/ecom-shop-material/WZbvSdhG_m_91442eb7c2c41ebb24f541863ae80189_sx_1055503_www800-800', NULL, '珠宝/饰品/手链/虎眼石', 118.00, 5.00, 236.00, 1, 'PENDING_REVIEW'),

(8, 'SP650548853693792257', '冰透玛瑙玉髓手镯 冰种飘花', '春江海月', 'https://p3-aio.ecombdimg.com/obj/ecom-shop-material/mKEtnMEv_m_d3a538de6127292be4064ec87eda7138_sx_2201781_www4000-4000', NULL, '珠宝/饰品/手镯/玛瑙', 88.00, 5.00, 176.00, 1, 'PENDING_REVIEW'),

(8, 'SP650548879828500481', '天然和田玉手串 晴水色', '春江海月', 'https://p3-aio.ecombdimg.com/obj/ecom-shop-material/mKEtnMEv_m_aaae86aaa4273fbeb8ff10ddc87228d8_sx_399546_www886-886', NULL, '珠宝/饰品/手串/和田玉', 399.00, 20.00, 798.00, 1, 'PENDING_REVIEW'),

(8, 'SP650550540805644289', '蜜蜡吊坠 琥珀平安扣', '春江海月', 'https://p9-aio.ecombdimg.com/obj/ecom-shop-material/mKEtnMEv_m_6c25301c4f0a9c2bc189fe0cf21f1995_sx_955951_www4032-4032', NULL, '珠宝/饰品/吊坠/蜜蜡', 168.00, 10.00, 336.00, 1, 'PENDING_REVIEW');

SET FOREIGN_KEY_CHECKS = 1;

-- 导入说明：
-- 1. merchant_id 设置为 8，请确保users表中存在id=8的商家用户
-- 2. sku 映射自 ywGoodsId（商品唯一编码）
-- 3. name 映射自 itemName（商品名称）
-- 4. brand 映射自 brandName（品牌名称）
-- 5. image_url 映射自 goodsImg（商品图片）
-- 6. initial_price 映射自 maxLivePrice（最高直播价格，作为起拍价参考）
-- 7. max_price 设置为 initial_price 的 2 倍（作为最高限价）
-- 8. bid_increment 根据价格区间设置合理的加价幅度：
--    - 价格 < 100元: 加价幅度 1-5元
--    - 价格 100-500元: 加价幅度 5-20元
--    - 价格 > 500元: 加价幅度 20-1000元
-- 9. category 根据商品名称自动分类
-- 10. stock 默认设置为 1
-- 11. status 默认为 'PENDING_REVIEW'（待审核状态）

-- 如需修改商家ID，请批量替换 merchant_id 值
-- 如需修改商品状态，可将 'PENDING_REVIEW' 改为：
--   - 'LISTED'（已上架）
--   - 'DELISTED'（已下架）
--   - 'SOLD_OUT'（已售罄）

-- 特殊说明：
-- 第6条商品（黑曜石手镯）价格为 99999.00 元，属于高价商品，加价幅度设置为 1000 元
-- 第4条商品（装饰门帘）价格为 8.80 元，属于低价商品，加价幅度设置为 1 元
