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

package cn.shopex.ecshopx.common.port.shuyun;

import java.util.Map;
import org.springframework.stereotype.Component;

/** 默认空实现；有开放平台 {@code LoyaltyMemberPointService}（@Primary）时优先生效。 */
@Component
public class ShuyunOpenPlatformPointPortNoOp implements ShuyunOpenPlatformPointPort {

	@Override
	public boolean isOpenPlatformPointEnabled(long companyId) {
		return false;
	}

	@Override
	public boolean changePoint(
			long companyId,
			long userId,
			int point,
			boolean plus,
			int journalType,
			String record,
			String orderId,
			Map<String, Object> otherParams) {
		return false;
	}

	@Override
	public Map<String, Object> searchChangelog(
			long companyId, long userId, long regDistributorId, int pageNo, int pageSize) {
		return null;
	}

	@Override
	public Long queryValidPoint(long companyId, long userId) {
		return null;
	}
}
