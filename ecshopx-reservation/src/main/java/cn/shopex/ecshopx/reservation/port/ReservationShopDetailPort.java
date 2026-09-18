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

package cn.shopex.ecshopx.reservation.port;

import java.util.Map;
import java.util.Optional;

/**
 * 门店详情（微信门店 + 资源包），供预约域读取。
 */
public interface ReservationShopDetailPort {

	Map<String, Object> getShopsDetail(long shopId, long companyId);

	/**
	 * 与 {@link #getShopsDetail} 相同字段；门店过期时抛出文案为排班场景专用（非预约文案）。
	 */
	Map<String, Object> getShopsDetailForWorkShiftCreate(long shopId, long companyId);

	/**
	 * GET /api/v1/reservation/period 专用：仅读库，无门店 / 归属不符 / 营业时间为空 / 门店已过期时返回 empty，不抛预约类 {@code ResourceException}。
	 */
	Optional<Map<String, Object>> findShopForEveryDayTimePeriod(long shopId, long companyId);

	/**
	 * Wxapp timelist: missing shop, company mismatch, expired, or empty hour → empty; when the shop references a
	 * resource pack that does not exist, throws the same business exception as {@link #getShopsDetail}.
	 */
	Optional<Map<String, Object>> findShopForWxappTimelist(long shopId, long companyId);
}
