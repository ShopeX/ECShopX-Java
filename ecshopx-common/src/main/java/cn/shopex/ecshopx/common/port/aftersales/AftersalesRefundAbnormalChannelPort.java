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

import java.util.Map;

/**
 * TradeFinish 异常退款渠道打款（仅渠道，无订单收尾 / 无 TradeRefundFinish）。
 */
public interface AftersalesRefundAbnormalChannelPort {

	/**
	 * @return 渠道结果，至少含 {@code status}（SUCCESS / PROCESSING / FAIL）
	 */
	Map<String, Object> dispatchAbnormalRefundChannel(long companyId, long refundBn);
}