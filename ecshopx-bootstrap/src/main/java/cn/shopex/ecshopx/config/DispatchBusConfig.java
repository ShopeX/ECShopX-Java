/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobMapper;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.dispatch.rabbit.RabbitDispatchMessageConverter;
import cn.shopex.ecshopx.dispatch.redis.RedisDelayedDispatchMover;
import cn.shopex.ecshopx.dispatch.redis.RedisDispatchDriver;
import cn.shopex.ecshopx.dispatch.redis.RedisDispatchMessageCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class DispatchBusConfig {

    @Bean
    public DispatchRegistry dispatchRegistry() {
        return new InMemoryDispatchRegistry();
    }

    @Bean
    public SyncDispatchDriver syncDispatchDriver(DispatchRegistry dispatchRegistry) {
        return new SyncDispatchDriver(dispatchRegistry);
    }

    @Bean
    public RedisDispatchDriver redisDispatchDriver(
            @Qualifier("dispatchBusStringRedisTemplate") StringRedisTemplate redis,
            RedisDispatchMessageCodec codec,
            DispatchConsumerStateRecorder dispatchConsumerStateRecorder,
            @Value("${dispatch.redis.delayed-zset-key:dispatch:delayed}") String delayedZSetKey) {
        return new RedisDispatchDriver(redis, codec, "dispatch:", dispatchConsumerStateRecorder, delayedZSetKey);
    }

    @Bean
    public RedisDelayedDispatchMover redisDelayedDispatchMover(
            @Qualifier("dispatchBusStringRedisTemplate") StringRedisTemplate redis,
            RedisDispatchMessageCodec codec,
            RedisDispatchDriver redisDispatchDriver,
            @Value("${dispatch.redis.delayed-zset-key:dispatch:delayed}") String delayedZSetKey) {
        return new RedisDelayedDispatchMover(redis, codec, redisDispatchDriver, delayedZSetKey);
    }

    @Bean
    public DispatchCore dispatchCore(
            DispatchRegistry dispatchRegistry,
            SyncDispatchDriver syncDispatchDriver,
            RedisDispatchDriver redisDispatchDriver) {
        return DispatchCore.asyncReady(
                dispatchRegistry, syncDispatchDriver, Map.of(DispatchDriverType.REDIS, redisDispatchDriver));
    }

    @Bean
    public DispatchFacade dispatchFacade(DispatchCore dispatchCore, DispatchRegistry dispatchRegistry) {
        return new DispatchFacade(dispatchCore, new DispatchFanOutPlanner(dispatchRegistry));
    }

    @Bean
    public DispatchStructuredLogger dispatchStructuredLogger() {
        return new DispatchStructuredLogger();
    }

    @Bean
    public DispatchRetryDecider dispatchRetryDecider() {
        return new DispatchRetryDecider();
    }

    @Bean
    public FailedJobMapper failedJobMapper() {
        return entity -> 1;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public FailedJobRecorder failedJobRecorder(FailedJobMapper failedJobMapper, ObjectMapper objectMapper) {
        return new FailedJobRecorder(failedJobMapper, objectMapper);
    }

    @Bean
    public DispatchConsumerStateRecorder dispatchConsumerStateRecorder() {
        return new InMemoryDispatchConsumerStateRecorder();
    }

    @Bean
    public DispatchConsumerRuntime dispatchConsumerRuntime(DispatchRegistry dispatchRegistry,
            DispatchRetryDecider dispatchRetryDecider,
            FailedJobRecorder failedJobRecorder,
            DispatchStructuredLogger dispatchStructuredLogger,
            DispatchConsumerStateRecorder dispatchConsumerStateRecorder) {
        return new DispatchConsumerRuntime(dispatchRegistry, dispatchRetryDecider, failedJobRecorder,
                dispatchStructuredLogger, dispatchConsumerStateRecorder);
    }

    @Bean
    public RedisDispatchMessageCodec redisDispatchMessageCodec(ObjectMapper objectMapper) {
        return new RedisDispatchMessageCodec(objectMapper);
    }

    @Bean
    public RabbitDispatchMessageConverter rabbitDispatchMessageConverter(ObjectMapper objectMapper) {
        return new RabbitDispatchMessageConverter(objectMapper);
    }
}
