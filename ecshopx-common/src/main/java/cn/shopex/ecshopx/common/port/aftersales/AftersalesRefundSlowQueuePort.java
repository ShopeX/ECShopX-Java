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

package cn.shopex.ecshopx.common.port.aftersales;

/**
 * 对位 Laravel <code>onQueue('slow')</code> 的投递端口；由 Redis 等实现，不落 MySQL jobs 表。
 */
public interface AftersalesRefundSlowQueuePort {

	/**
	 * 投递待执行的退款单消息。{@code delaySeconds} 为 0 表示立即可消费；&gt;0 为秒级相对延迟，实现侧可用 ZSET/延迟队列等落地。
	 */
	void enqueue(AftersalesRefundQueueMessage message, int delaySeconds);
}
