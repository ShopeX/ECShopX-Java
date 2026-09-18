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

package cn.shopex.ecshopx.common.port.thirdparty;

/**
 * 对位 <code>TradeRefundFinish</code> 监听器外发营销中心 / DM 等；仅供消费端在退款成功等路径上调用。
 */
public interface TradeRefundFinishExternalNotifyPort {

	/**
	 * 结构化打点参数；无业务语义，实现侧可映射为 HTTP。
	 */
	void notifyAfterRefundSettled(long companyId, long orderId, long refundBn, String tradeId);
}
