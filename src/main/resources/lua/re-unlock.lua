---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by ghp.
--- DateTime: 2023/2/14 16:11
---
-- 获取锁的key，即: KEY_PREFIX + name
local key = KEYS[1];
-- 获取当前线程的标识, 即: ID_PREFIX + Thread.currentThread().getId()
local threadId = ARGV[1];
-- 锁的有效期
local releaseTime = ARGV[2];

-- 判断当前线程的锁是否还在缓存中
if (redis.call('HEXISTS', key, threadId) == 0) then
    -- 缓存中没找到自己的锁，说明锁已过期，则直接返回空
    return nil; -- 返回nil，表示啥也不干
end
-- 缓存中找到了自己的锁，则重入次数-1
local count = redis.call('HINCRBY', key, threadId, -1);

-- 进一步判断是否需要释放锁
if (count > 0) then
    -- 重入次数大于0，说明不能释放锁，且刷新锁的有效期
    redis.call('EXPIRE', key, releaseTime);
    return nil;
else
    -- 重入次数等于0，说明可以释放锁
    redis.call('DEL', key);
    return nil;
end
