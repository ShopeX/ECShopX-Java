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

package cn.shopex.ecshopx.common.deposit.port;

import java.util.List;
import java.util.Map;

public interface OpenapiMemberDetailDepositRechargeSumPort {

	/**
	 * 条件：trade_type=recharge AND trade_status=SUCCESS，GROUP BY user_id，SUM(money)。
	 *
	 * @return Map key=userId, value=充值成功金额（分）；无记录 userId 不在 Map 中
	 */
	Map<Long, Long> sumRechargeSuccessFenByUserIds(List<Long> userIds);
}
