package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import cn.shopex.ecshopx.config.DispatchBusConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

class DispatchBusConfigTest {

    @Configuration
    static class DispatchBusTestRedisStub {
        @Bean(name = "dispatchBusStringRedisTemplate")
        StringRedisTemplate dispatchBusStringRedisTemplate() {
            return Mockito.mock(StringRedisTemplate.class);
        }
    }

    @Test
    void configExposesCoreBeans() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                DispatchBusConfig.class, DispatchBusTestRedisStub.class)) {
            assertNotNull(ctx.getBean(DispatchRegistry.class));
            assertNotNull(ctx.getBean(SyncDispatchDriver.class));
            assertNotNull(ctx.getBean(DispatchCore.class));
            assertNotNull(ctx.getBean(DispatchFacade.class));
            assertNotNull(ctx.getBean(DispatchStructuredLogger.class));
            assertNotNull(ctx.getBean(DispatchConsumerRuntime.class));
            assertNotNull(ctx.getBean(FailedJobMapper.class));
        }
    }
}
