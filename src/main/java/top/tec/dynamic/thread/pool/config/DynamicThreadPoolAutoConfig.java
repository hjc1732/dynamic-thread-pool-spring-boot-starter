package top.tec.dynamic.thread.pool.config;

import io.micrometer.core.instrument.util.StringUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.tec.dynamic.thread.pool.domain.DynamicThreadPoolService;
import top.tec.dynamic.thread.pool.domain.IDynamicThreadPoolService;
import top.tec.dynamic.thread.pool.domain.entity.ThreadPoolConfigEntity;
import top.tec.dynamic.thread.pool.domain.valobj.RegistryEnum;
import top.tec.dynamic.thread.pool.registry.IRegistry;
import top.tec.dynamic.thread.pool.registry.Registry;
import top.tec.dynamic.thread.pool.trigger.job.ThreadPoolDataReportJob;
import top.tec.dynamic.thread.pool.trigger.listener.ThreadPoolAdjustListener;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author hujincheng
 */
@EnableScheduling
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
@Configuration
public class DynamicThreadPoolAutoConfig {
    private final Logger logger = LoggerFactory.getLogger(DynamicThreadPoolAutoConfig.class);

    /**
     * 初始化Redisson
     */
    @Bean("dynamicRedissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoProperties properties) {
        Config config = new Config();
        // 根据需要可以设定编解码器；https://github.com/redisson/redisson/wiki/4.-%E6%95%B0%E6%8D%AE%E5%BA%8F%E5%88%97%E5%8C%96
        config.setCodec(JsonJacksonCodec.INSTANCE);

        DynamicThreadPoolAutoProperties.RedissonConfig redissonConfig = properties.getRedisson();
        config.useSingleServer()
                .setAddress("redis://" + redissonConfig.getHost() + ":" + redissonConfig.getPort())
                .setPassword(redissonConfig.getPassword())
                .setConnectionPoolSize(redissonConfig.getPoolSize())
                .setConnectionMinimumIdleSize(redissonConfig.getMinIdleSize())
                .setIdleConnectionTimeout(redissonConfig.getIdleTimeout())
                .setConnectTimeout(redissonConfig.getConnectTimeout())
                .setRetryAttempts(redissonConfig.getRetryAttempts())
                .setRetryInterval(redissonConfig.getRetryInterval())
                .setPingConnectionInterval(redissonConfig.getPingInterval())
                .setKeepAlive(redissonConfig.isKeepAlive())
        ;

        RedissonClient redissonClient = Redisson.create(config);
        logger.info("动态线程池，注册器（redisson）链接初始化完成。{} {} {}", redissonConfig.getHost(), redissonConfig.getPoolSize(), !redissonClient.isShutdown());
        return redissonClient;
    }

    /**
     * 初始化动态线程池服务
     */
    @Bean("dynamicThreadPoolService")
    public IDynamicThreadPoolService dynamicThreadPoolService(
            DynamicThreadPoolAutoProperties properties,
            ApplicationContext applicationContext,
            Map<String, ThreadPoolExecutor> threadPoolExecutorMap,
            RedissonClient dynamicRedissonClient) {
        String appName = properties.getAppName();
        if (StringUtils.isEmpty(appName)) {
            appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        }

        if (StringUtils.isEmpty(appName)) {
            appName = "default-application-name-" + RandomStringUtils.randomAlphanumeric(5).toLowerCase(Locale.ROOT);
        }



        //重新启动项目时,加载配置好的线程池,获取缓存数据，设置本地线程池配置
        Set<String> threadPoolKeys = threadPoolExecutorMap.keySet();
        for (String threadPoolKey : threadPoolKeys) {
            ThreadPoolConfigEntity threadPoolConfigEntity = dynamicRedissonClient.<ThreadPoolConfigEntity>getBucket(RegistryEnum.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + appName + "_" + threadPoolKey).get();
            if (null == threadPoolConfigEntity) {
                continue;
            }
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolKey);
            if(null == threadPoolExecutor){
                continue;
            }

            if(threadPoolConfigEntity.getMaximumPoolSize() < threadPoolConfigEntity.getCorePoolSize()){
                continue;
            }

            threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
            threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
        }


        return new DynamicThreadPoolService(appName, threadPoolExecutorMap);
    }

    /**
     * 初始化上报服务
     */
    @Bean("dynamicRegistry")
    public IRegistry registry(RedissonClient dynamicRedissonClient) {
        return new Registry(dynamicRedissonClient);
    }


    /**
     * 初始化上报任务
     */
    @Bean
    public ThreadPoolDataReportJob threadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry dynamicRegistry) {
        return new ThreadPoolDataReportJob(dynamicThreadPoolService, dynamicRegistry);
    }


    @Bean
    public ThreadPoolAdjustListener threadPoolConfigAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry dynamicRegistry) {
        return new ThreadPoolAdjustListener(dynamicThreadPoolService, dynamicRegistry);
    }

    /**
     * 初始化redis上报主题
     */
    @Bean("dynamicInitTopic")
    public RTopic initTopic(RedissonClient dynamicRedissonClient, ThreadPoolAdjustListener threadPoolConfigAdjustListener) {
        RTopic topic = dynamicRedissonClient.getTopic(RegistryEnum.DYNAMIC_THREAD_POOL_REDIS_TOPIC.getKey());
        topic.addListener(ThreadPoolConfigEntity.class, threadPoolConfigAdjustListener);
        return topic;
    }

}
