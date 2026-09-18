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

package cn.shopex.ecshopx.common.cron;

import java.util.Map;

/**
 * 计划活动消费者路径上的场景类短信外发，由 promotions 装配合适实现，test-cron 下替换为 Noop。
 */
public interface PromotionScheduleFireSceneSmsOutPort {

	void sendTemplatedSceneSms(long companyId, String mobilePlain, String sceneTitle, Map<String, String> variables);
}
