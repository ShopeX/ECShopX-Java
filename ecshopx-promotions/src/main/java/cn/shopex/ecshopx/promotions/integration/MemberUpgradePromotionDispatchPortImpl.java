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

package cn.shopex.ecshopx.promotions.integration;

import cn.shopex.ecshopx.common.promotions.port.MemberUpgradePromotionDispatchPort;
import cn.shopex.ecshopx.promotions.service.PromotionActivityFireService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Dispatches {@code member_upgrade} promotion firing by delegating to {@link PromotionActivityFireService}
 * (valid activities in window, per-row scheduled actions).
 */
@Component
@RequiredArgsConstructor
public class MemberUpgradePromotionDispatchPortImpl implements MemberUpgradePromotionDispatchPort {

	private final PromotionActivityFireService promotionActivityFireService;

	@Override
	public void dispatchMemberUpgradeFire(long companyId, Map<String, Object> memberInfo) {
		promotionActivityFireService.fire(companyId, memberInfo, "member_upgrade");
	}
}
