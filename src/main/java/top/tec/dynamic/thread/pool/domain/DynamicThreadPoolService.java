package top.tec.dynamic.thread.pool.domain;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import top.tec.dynamic.thread.pool.domain.entity.ThreadPoolConfigEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author hujincheng
 * @description DynamicThreadPoolService
 * @create 2024-07-11 13:52
 */
public class DynamicThreadPoolService implements IDynamicThreadPoolService {
    private final Logger logger = LoggerFactory.getLogger(DynamicThreadPoolService.class);

    private final String appName;
    private final Map<String, ThreadPoolExecutor> threadPoolExecutorMap;

    public DynamicThreadPoolService(String appName, Map<String, ThreadPoolExecutor> threadPoolExecutorMap) {
        this.appName = appName;
        this.threadPoolExecutorMap = threadPoolExecutorMap;
    }

    @Override
    public List<ThreadPoolConfigEntity> getThreadPoolList() {

        return threadPoolExecutorMap.entrySet().stream()
                .map(threadPool -> {
                    String poolName = threadPool.getKey();
                    ThreadPoolExecutor poolExecutor = threadPool.getValue();
                    if (StringUtils.hasLength(poolName) && Objects.nonNull(poolExecutor)) {
                        ThreadPoolConfigEntity configEntity = new ThreadPoolConfigEntity(appName, poolName);
                        configEntity.setCorePoolSize(poolExecutor.getCorePoolSize());
                        configEntity.setMaximumPoolSize(poolExecutor.getMaximumPoolSize());
                        configEntity.setActiveCount(poolExecutor.getActiveCount());
                        configEntity.setPoolSize(poolExecutor.getPoolSize());
                        configEntity.setQueueType(poolExecutor.getQueue().getClass().getSimpleName());
                        configEntity.setQueueSize(poolExecutor.getQueue().size());
                        configEntity.setRemainingCapacity(poolExecutor.getQueue().remainingCapacity());
                        return configEntity;
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public ThreadPoolConfigEntity getThreadPoolByName(String threadPoolName) {
        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolName);
        if (null == threadPoolExecutor) {
            return new ThreadPoolConfigEntity(appName, threadPoolName);
        }
        // 线程池配置数据
        ThreadPoolConfigEntity threadPoolConfigVO = new ThreadPoolConfigEntity(appName, threadPoolName);
        threadPoolConfigVO.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
        threadPoolConfigVO.setMaximumPoolSize(threadPoolExecutor.getMaximumPoolSize());
        threadPoolConfigVO.setActiveCount(threadPoolExecutor.getActiveCount());
        threadPoolConfigVO.setPoolSize(threadPoolExecutor.getPoolSize());
        threadPoolConfigVO.setQueueType(threadPoolExecutor.getQueue().getClass().getSimpleName());
        threadPoolConfigVO.setQueueSize(threadPoolExecutor.getQueue().size());
        threadPoolConfigVO.setRemainingCapacity(threadPoolExecutor.getQueue().remainingCapacity());

        if (logger.isDebugEnabled()) {
            logger.info("动态线程池，配置查询 应用名:{} 线程名:{} 池化配置:{}", appName, threadPoolName, JSON.toJSONString(threadPoolConfigVO));
        }
        return threadPoolConfigVO;
    }

    @Override
    public void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity) {
        if (null == threadPoolConfigEntity || !appName.equals(threadPoolConfigEntity.getAppName())) {
            return;
        }

        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolConfigEntity.getThreadPoolName());
        if (null == threadPoolExecutor) {
            return;
        }
        // 设置参数 「调整核心线程数和最大线程数」
        threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
        threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
    }
}
