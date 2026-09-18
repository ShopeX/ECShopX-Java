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

package cn.shopex.ecshopx.kaquan.cron.testcfg;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用 bean 覆盖配置；仅在 spring.profiles.active 包含 test-cron 时生效。
 * UserDiscountService.scheduleCancelExCard 任务无 plan §4.1 所列 Noop 项，此处暂无 {@code @Bean} 覆盖项。
 */
@Profile("test-cron")
@Configuration("kaquanCronTestMockConfig")
public class CronTestMockConfig {}
