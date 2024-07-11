package top.tec.dynamic.thread.pool.trigger.listener;

import com.alibaba.fastjson.JSON;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.tec.dynamic.thread.pool.domain.IDynamicThreadPoolService;
import top.tec.dynamic.thread.pool.domain.entity.ThreadPoolConfigEntity;
import top.tec.dynamic.thread.pool.registry.IRegistry;

import java.util.List;

/**
 * @author hujincheng
 * @description DynamicThreadPoolListener 线程池变更监听器
 * @create 2024-07-11 11:19
 */
public class ThreadPoolAdjustListener implements MessageListener<ThreadPoolConfigEntity> {
    private final Logger log = LoggerFactory.getLogger(ThreadPoolAdjustListener.class);

    private final IDynamicThreadPoolService dynamicThreadPoolService;

    private final IRegistry registry;

    public ThreadPoolAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registry = registry;
    }

    @Override
    public void onMessage(CharSequence charSequence, ThreadPoolConfigEntity threadPoolConfigEntity) {
        log.info("动态线程池，调整线程池配置。线程池名称:{} 核心线程数:{} 最大线程数:{}", threadPoolConfigEntity.getThreadPoolName(), threadPoolConfigEntity.getCorePoolSize(), threadPoolConfigEntity.getMaximumPoolSize());

        if (threadPoolConfigEntity.getCorePoolSize() <= 0 || threadPoolConfigEntity.getMaximumPoolSize() <= 0 || threadPoolConfigEntity.getMaximumPoolSize() < threadPoolConfigEntity.getCorePoolSize()) {
            return;
        }

        dynamicThreadPoolService.updateThreadPoolConfig(threadPoolConfigEntity);

        // 更新后上报最新数据
        List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.getThreadPoolList();
        registry.reportThreadPool(threadPoolConfigEntities);

        ThreadPoolConfigEntity threadPoolConfigEntityCurrent = dynamicThreadPoolService.getThreadPoolByName(threadPoolConfigEntity.getThreadPoolName());
        registry.reportThreadPoolConfigParameter(threadPoolConfigEntityCurrent);
        log.info("动态线程池，上报线程池配置：{}", JSON.toJSONString(threadPoolConfigEntity));
    }
}
