package top.tec.dynamic.thread.pool.domain;

import top.tec.dynamic.thread.pool.domain.entity.ThreadPoolConfigEntity;

import java.util.List;

/**
 * @author hujincheng
 * @description IDynamicThreadPoolService 动态线程池服务
 * @create 2024-07-11 11:13
 */
public interface IDynamicThreadPoolService {

    /**
     *  获取线程池列表
     */
    List<ThreadPoolConfigEntity> getThreadPoolList();

    /**
     * 根据名称获取线程池
     */
    ThreadPoolConfigEntity getThreadPoolByName(String threadPoolName);

    /**
     * 变更线程池配置
     */
    void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity);

}
