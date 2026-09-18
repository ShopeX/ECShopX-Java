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

package cn.shopex.ecshopx.common.refund;

import java.util.Map;

/**
 * 售后退款支付渠道执行器。由各领域模块注册 Spring Bean，统一由订单侧调度。
 */
public interface AftersalesRefundPayChannelExecutor {

	/**
	 * @param payTypeLower 已规范为小写的 {@link AftersalesRefundPaymentContext#payTypeRaw}
	 */
	boolean supports(String payTypeLower);

	/**
	 * @return 与各支付通道 {@code doRefund} 对齐的摘要：至少包含 {@code status}（SUCCESS / FAIL /
	 *     PROCESSING），成功或处理中时常含 {@code refund_id}
	 */
	Map<String, Object> execute(AftersalesRefundPaymentContext ctx);
}
