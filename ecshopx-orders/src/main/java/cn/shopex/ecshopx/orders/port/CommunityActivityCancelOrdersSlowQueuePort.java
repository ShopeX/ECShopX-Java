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

package cn.shopex.ecshopx.orders.port;

import cn.shopex.ecshopx.common.port.orders.CommunityActivityCancelOrderBatchPayload;

/**
 * 对位 Laravel 团活动单批量取消入 {@code slow} 队列；与发票等分键，负载为 JSON {@link CommunityActivityCancelOrderBatchPayload}。
 */
public interface CommunityActivityCancelOrdersSlowQueuePort {

	/**
	 * 投递一批待取消行，delaySeconds=0 表示立即可被轮询拉取；&gt;0 为相对延迟入延迟 ZSET（若实现支持）。
	 */
	void enqueue(CommunityActivityCancelOrderBatchPayload payload, int delaySeconds);
}
