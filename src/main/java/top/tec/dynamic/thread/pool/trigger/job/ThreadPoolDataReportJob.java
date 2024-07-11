package top.tec.dynamic.thread.pool.trigger.job;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import top.tec.dynamic.thread.pool.domain.IDynamicThreadPoolService;
import top.tec.dynamic.thread.pool.domain.entity.ThreadPoolConfigEntity;
import top.tec.dynamic.thread.pool.registry.IRegistry;

import java.util.List;

/**
 * @author hujincheng
 */
public class ThreadPoolDataReportJob {

    private final Logger logger = LoggerFactory.getLogger(ThreadPoolDataReportJob.class);
    private final IDynamicThreadPoolService dynamicThreadPoolService;
    private final IRegistry dynamicRegistry;

    @Value("${dynamic-thread-pool.config.enable}")
    public boolean enable;

    public ThreadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry dynamicRegistry) {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.dynamicRegistry = dynamicRegistry;
    }

    @Scheduled(cron = "${dynamic-thread-pool.config.report.cron}")
    public void execReportThreadPoolList() {
        if(enable){
            List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.getThreadPoolList();
            dynamicRegistry.reportThreadPool(threadPoolConfigEntities);
            logger.info("动态线程池，上报线程池信息：{}", JSON.toJSONString(threadPoolConfigEntities));

            for (ThreadPoolConfigEntity threadPoolConfigEntity : threadPoolConfigEntities) {
                dynamicRegistry.reportThreadPoolConfigParameter(threadPoolConfigEntity);
                logger.info("动态线程池，上报线程池配置：{}", JSON.toJSONString(threadPoolConfigEntity));
            }
        }
    }
}
