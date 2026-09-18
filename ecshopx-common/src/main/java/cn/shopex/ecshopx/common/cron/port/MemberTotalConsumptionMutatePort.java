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

package cn.shopex.ecshopx.common.cron.port;

import java.math.BigDecimal;

/**
 * 会员累计消费 Redis（分）写入；与 {@link cn.shopex.ecshopx.members.service.stats.MemberTotalConsumptionReadService} 同键。
 */
public interface MemberTotalConsumptionMutatePort {

	/**
	 * 与 PHP 先读再分支一致：若累加后总额 &gt; 0 则对键做增量；否则置 0。
	 *
	 * @param userId 用户 id
	 * @param deltaFen 本次订单侧汇总消费，单位分，可正可负（与 trade 汇总一致）
	 */
	void addFenToTotalOrSetZero(long userId, BigDecimal deltaFen);
}
