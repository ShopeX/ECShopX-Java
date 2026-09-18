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

import java.util.List;
import java.util.Map;

/**
 * 小程序「可预约权益」分页：orders 域 {@code orders_rights} 查询与 TimesCard 行形态。
 */
public interface ReservationWxappCanRightsListPort {

	WxappCanRightsListResult queryPage(long companyId, Long userId, int nowEpochSec, int page, int pageSize);

	final class WxappCanRightsListResult {

		private final List<Map<String, Object>> list;

		private final long totalCount;

		public WxappCanRightsListResult(List<Map<String, Object>> list, long totalCount) {
			this.list = list;
			this.totalCount = totalCount;
		}

		public List<Map<String, Object>> getList() {
			return list;
		}

		public long getTotalCount() {
			return totalCount;
		}
	}
}
