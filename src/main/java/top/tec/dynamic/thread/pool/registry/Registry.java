package top.tec.dynamic.thread.pool.registry;

import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import top.tec.dynamic.thread.pool.domain.entity.ThreadPoolConfigEntity;
import top.tec.dynamic.thread.pool.domain.valobj.RegistryEnum;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hujincheng
 * @description Registry
 * @create 2024-07-11 14:53
 */
public class Registry implements IRegistry {
    private final RedissonClient redissonClient;

    public Registry(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities) {
        if (CollectionUtils.isEmpty(threadPoolEntities)) {
            throw new RuntimeException("上报线程池列表时,未初始化线程池,需初始化线程池或配置文件中禁用上报组件");
        }

        ThreadPoolConfigEntity threadPoolConfigEntity = threadPoolEntities.stream().findFirst().orElse(null);
        if (ObjectUtils.isEmpty(threadPoolConfigEntity)) {
            throw new RuntimeException("上报线程池列表时,未初始化线程池,需初始化线程池或配置文件中禁用上报组件");
        }
        String appName = threadPoolConfigEntity.getAppName();

        RLock lock = redissonClient.getLock(RegistryEnum.THREAD_POOL_CONFIG_LIST_KEY_LOCK.getKey());

        try {
            lock.lock();
            RList<ThreadPoolConfigEntity> list = redissonClient.getList(RegistryEnum.THREAD_POOL_CONFIG_LIST_KEY.getKey());

            List<ThreadPoolConfigEntity> applicationPool = list.stream().filter(item -> !appName.equals(item.getAppName()))
                    .collect(Collectors.toList());

            applicationPool.addAll(threadPoolEntities);
            //删除全部
            list.delete();
            list.addAll(applicationPool);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity) {
        String cacheKey = RegistryEnum.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + threadPoolConfigEntity.getAppName() + "_" + threadPoolConfigEntity.getThreadPoolName();
        RBucket<ThreadPoolConfigEntity> bucket = redissonClient.getBucket(cacheKey);
        bucket.set(threadPoolConfigEntity, Duration.ofDays(30));
    }
}
