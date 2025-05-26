package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.hmdp.utils.RedisConstants;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public List<ShopType> queryTypeList() {
        // 1. 从redis中获取店铺类型缓存
        String typeJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_TYPE);
        if (typeJson != null) {
            // 存在，直接返回
            return JSONUtil.toList(typeJson, ShopType.class); // 将json字符串转换为ShopType对象列表
        }

        // 2. 缓存不存在，查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if (shopTypes == null) {
            // 3. 不存在，返回错误
            return null;
        }

        // 4. 将数据库中的店铺类型缓存到redis中
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_TYPE,
                JSONUtil.toJsonStr(shopTypes),
                RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES); // 将shopTypes对象转换为json字符串

        // 5. 返回店铺类型列表
        return shopTypes;
    }
}
