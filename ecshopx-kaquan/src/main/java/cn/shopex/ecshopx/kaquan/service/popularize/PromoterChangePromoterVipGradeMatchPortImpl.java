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

package cn.shopex.ecshopx.kaquan.service.popularize;

import cn.shopex.ecshopx.common.popularize.PromoterChangePromoterVipGradeMatchPort;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromoterChangePromoterVipGradeMatchPortImpl implements PromoterChangePromoterVipGradeMatchPort {

	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;

	public PromoterChangePromoterVipGradeMatchPortImpl(
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService) {
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
	}

	@Override
	public boolean matchesConfiguredVipType(long companyId, long userId, String configuredVipGradeType) {
		Map<String, Object> vip = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		boolean isVip = Boolean.TRUE.equals(vip.get("is_vip"));
		String vipType = vip.get("vip_type") == null ? "" : String.valueOf(vip.get("vip_type")).trim();
		String configured = configuredVipGradeType == null ? "" : configuredVipGradeType.trim();
		return isVip && vipType.equals(configured);
	}
}
