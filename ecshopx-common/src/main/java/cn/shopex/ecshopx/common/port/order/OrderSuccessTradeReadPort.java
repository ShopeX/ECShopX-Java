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

package cn.shopex.ecshopx.common.port.order;

import java.util.Map;
import java.util.Optional;

/** 读取订单下支付成功的交易单（用于售后/取消退款组装）。 */
public interface OrderSuccessTradeReadPort {

	/**
	 * 多条成功记录时排除纯积分支付再取一条。
	 */
	Optional<Map<String, Object>> primarySuccessTrade(long companyId, long orderId);
}
