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

/**
 * 分佣结算时对商户 Redis Hash（promoterPopularizeCount / companyPopularizeCount）做与 PHP addSettleRebate 一致的 hincr。
 * 生产实现见 ecshopx-popularize；test-cron 下由 Noop 替换。
 */
public interface PopularizeBrokerageSettleRedisPort {

	/**
	 * 等价 PHP setDataToRedis 可提现增量 + 未结算扣减：cashWithdrawalRebate += rebate、noCloseRebate -= rebate（各 2 个 key，共 4 次 hincr）。
	 */
	void applySettleRebateHashIncr(long companyId, long userId, long rebateMinor);
}
