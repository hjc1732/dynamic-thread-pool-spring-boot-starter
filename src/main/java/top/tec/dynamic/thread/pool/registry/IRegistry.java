package top.tec.dynamic.thread.pool.registry;

import top.tec.dynamic.thread.pool.domain.entity.ThreadPoolConfigEntity;

import java.util.List;

/**
 * @author hujincheng
 */
public interface IRegistry {

    /**
     * 上报线程池列表
     */
    void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities);

    /**
     * 上报单个线程池
     */
    void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity);



}
