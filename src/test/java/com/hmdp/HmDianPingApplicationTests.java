package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private ExecutorService es = Executors.newFixedThreadPool(500); // 创建线程池

    /**
     * 测试分布式ID生成器的性能，以及可用性
     */
    @Test
    public void testNextId() throws InterruptedException {
        // 使用CountDownLatch让线程同步等待
        CountDownLatch latch = new CountDownLatch(300); // 300个线程
        // 创建线程任务
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            // 等待次数-1
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        // 创建300个线程，每个线程创建100个id，总计生成3w个id
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        // 线程阻塞，直到计数器归0时才全部唤醒所有线程
        latch.await(); // 等待所有线程执行完毕
        long end = System.currentTimeMillis();
        System.out.println("生成3w个id共耗时" + (end - begin) + "ms");
    }
    /**
     * 预热店铺数据
     */
    @Test
    public void testSaveShopToCache(){
        shopService.saveShopToCache(1L,10L);
    }

    /**
     * 预热店铺数据，按照typeId进行分组，用于实现附近商户搜索功能
     */
    @Test
    public void loadShopListToCache() {
        // 1、获取店铺数据
        List<Shop> shopList = shopService.list();
        // 2、根据 typeId 进行分类
//        Map<Long, List<Shop>> shopMap = new HashMap<>();
//        for (Shop shop : shopList) {
//            Long shopId = shop.getId();
//            if (shopMap.containsKey(shopId)){
//                // 已存在，添加到已有的集合中
//                shopMap.get(shopId).add(shop);
//            }else{
//                // 不存在，直接添加
//                shopMap.put(shopId, Arrays.asList(shop));
//            }
//        }
        // 使用 Lambda 表达式，更加优雅（优雅永不过时）
        Map<Long, List<Shop>> shopMap = shopList.stream()
                .collect(Collectors.groupingBy(Shop::getTypeId));

        // 3、将分好类的店铺数据写入redis
        for (Map.Entry<Long, List<Shop>> shopMapEntry : shopMap.entrySet()) {
            // 3.1 获取 typeId
            Long typeId = shopMapEntry.getKey();
            List<Shop> values = shopMapEntry.getValue();
            // 3.2 将同类型的店铺的写入同一个GEO ( GEOADD key 经度 维度 member )
            String key = SHOP_GEO_KEY + typeId;
            // 方式一：单个写入(这种方式，一个请求一个请求的发送，十分耗费资源，我们可以进行批量操作)
//            for (Shop shop : values) {
//                stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()),
//                shop.getId().toString());
//            }
            // 方式二：批量写入
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>();
            for (Shop shop : values) {
                locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }


}


