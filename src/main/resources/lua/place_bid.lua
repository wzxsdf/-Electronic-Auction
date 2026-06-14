-- ============================================
-- 竞拍出价 Redis Lua 脚本（原子操作）
-- 功能：验证出价 + 更新价格 + 触发延时
-- 返回：{code, message, newPrice, newEndTime}
-- ============================================

local itemId = KEYS[1]
local userId = tonumber(ARGV[1])
local bidAmount = tonumber(ARGV[2])
local bidIncrement = tonumber(ARGV[3])
local maxPrice = tonumber(ARGV[4])
local endTime = tonumber(ARGV[5])
local delaySeconds = tonumber(ARGV[6])
local now = tonumber(ARGV[7])

-- Redis key 定义
local itemKey = 'auction:item:' .. itemId
local bidQueueKey = 'auction:item:' .. itemId .. ':bid_queue'

-- 1. 获取拍品当前状态
local currentPrice = tonumber(redis.call('HGET', itemKey, 'currentPrice')) or 0
local highestBidder = tonumber(redis.call('HGET', itemKey, 'highestBidder'))
local status = redis.call('HGET', itemKey, 'status')
local delayCount = tonumber(redis.call('HGET', itemKey, 'delayCount')) or 0

-- 2. 验证拍品状态
if status ~= 'ACTIVE' then
    return {-1, '拍品未进行中', currentPrice, endTime}
end

-- 3. 验证结束时间
if endTime and now >= endTime then
    -- 自动标记为已结束
    redis.call('HSET', itemKey, 'status', 'COMPLETED')
    return {-2, '拍品已结束', currentPrice, endTime}
end

-- 4. 验证是否是当前最高出价者
if highestBidder and highestBidder == userId then
    return {-3, '您已是最高出价者', currentPrice, endTime}
end

-- 5. 验证出价金额（当前价 + 加价幅度）
local minBid = currentPrice + bidIncrement
if bidAmount < minBid then
    return {-4, '出价过低，最低需: ' .. minBid, currentPrice, endTime}
end

-- 6. 验证封顶价
if maxPrice and maxPrice > 0 and bidAmount > maxPrice then
    return {-5, '超过封顶价: ' .. maxPrice, currentPrice, endTime}
end

-- 6.5 检查是否达到封顶价（精确匹配触发自动成交）
local isMaxPriceBid = false
if maxPrice and maxPrice > 0 and bidAmount == maxPrice then
    isMaxPriceBid = true
end

-- 7. 原子性更新拍品状态
local newBidCount = (tonumber(redis.call('HGET', itemKey, 'bidCount')) or 0) + 1

-- 如果达到封顶价，标记拍卖完成并提前结束
if isMaxPriceBid then
    redis.call('HMSET', itemKey,
        'currentPrice', bidAmount,
        'highestBidder', userId,
        'bidCount', newBidCount,
        'lastBidTime', now,
        'lastBidderId', userId,
        'status', 'COMPLETED'
    )
else
    redis.call('HMSET', itemKey,
        'currentPrice', bidAmount,
        'highestBidder', userId,
        'bidCount', newBidCount,
        'lastBidTime', now,
        'lastBidderId', userId
    )
end

-- 8. 将出价记录写入队列（用于异步持久化）
local bidRecord = userId .. ':' .. bidAmount .. ':' .. now .. ':' .. newBidCount
redis.call('RPUSH', bidQueueKey, bidRecord)
redis.call('EXPIRE', bidQueueKey, 86400) -- 24小时过期

-- 9. 检查并触发延时机制（达到封顶价时不触发延时）
local newEndTime = endTime
if not isMaxPriceBid and endTime and delaySeconds and delaySeconds > 0 then
    local remainingTime = endTime - now
    -- 剩余时间小于延时阈值时触发
    if remainingTime <= delaySeconds and delayCount < 3 then
        newEndTime = endTime + delaySeconds
        redis.call('HSET', itemKey, 'endTime', newEndTime)
        redis.call('HINCRBY', itemKey, 'delayCount', 1)
        delayCount = delayCount + 1
    end
end

-- 10. 返回结果
if isMaxPriceBid then
    -- 返回码2表示达到封顶价自动成交
    return {2, '达到封顶价，自动成交', bidAmount, endTime, delayCount, newBidCount}
else
    -- 正常出价成功
    return {1, '出价成功', bidAmount, newEndTime, delayCount, newBidCount}
end
