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
 * 活动结束条件满足时，删除整段「用户剩余次数」Redis 键；实现类位于 ecshopx-promotions（与
 * {@link TurntablePayGetTimesOnOrderPort} 操作同一 key 命名空间）。
 */
public interface TurntableClearSurplusTimesRedisPort {

	String SURPLUS_KEY_PREFIX = "turntableUserSurplusTimes:CompanyId:";

	/**
	 * 删除键 {@code turntableUserSurplusTimes:CompanyId:{companyId}}（整 key）。
	 */
	void deleteEntireSurplusTimesKey(long companyId);
}
