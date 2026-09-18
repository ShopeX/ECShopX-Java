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

/**
 * 订单完成后按购物满额增加大转盘剩余抽奖次数（与 PHP {@code TurntableService::payGetTurntableTimes} 对齐的入口）。
 */
public interface TurntablePayGetTimesOnOrderPort {

	/**
	 * @param userId 用户 id
	 * @param companyId 公司 id
	 * @param totalFeeFen 订单金额，单位分
	 */
	void payGetTimes(long userId, long companyId, int totalFeeFen);
}
