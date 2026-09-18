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

package cn.shopex.ecshopx.popularize.integration.members;

import cn.shopex.ecshopx.common.members.port.WxappMemberPopularizeFlagsPort;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.service.PopularizeSettingSaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("wxappMemberPopularizeFlagsPortImpl")
public class WxappMemberPopularizeFlagsPortImpl implements WxappMemberPopularizeFlagsPort {

	private final PopularizeSettingSaveService popularizeSettingSaveService;
	private final PromoterMapper promoterMapper;

	public WxappMemberPopularizeFlagsPortImpl(
			PopularizeSettingSaveService popularizeSettingSaveService,
			PromoterMapper promoterMapper) {
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.promoterMapper = promoterMapper;
	}

	@Override
	public void apply(long companyId, long userId, Map<String, Object> claims, Map<String, Object> result) {
		String openLit = popularizeSettingSaveService.getOpenPopularizeLiteral(companyId);
		boolean isOpen = !"false".equals(openLit);
		result.put("is_open_popularize", isOpen);
		if (!isOpen || userId <= 0L) {
			return;
		}
		Promoter promoter =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, userId)
								.eq(Promoter::getDisabled, 0)
								.last("LIMIT 1"));
		boolean isPromoter = promoter != null && promoter.getIsPromoter() != null && promoter.getIsPromoter() == 1;
		result.put("is_promoter", isPromoter);
		if (!isPromoter) {
			Map<String, Object> cfg = popularizeSettingSaveService.getConfig(companyId, null, null);
			Object change = cfg.get("change_promoter");
			if (change instanceof Map<?, ?> cm) {
				Object type = cm.get("type");
				if ("internal".equals(String.valueOf(type))) {
					result.put("is_open_popularize", Boolean.FALSE);
				}
			}
		}
	}
}
