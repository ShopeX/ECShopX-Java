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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.promotions.port.WxaNewTmplListPort;
import cn.shopex.ecshopx.promotions.domain.WxaNoticeTemplate;
import cn.shopex.ecshopx.promotions.mapper.WxaNoticeTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxaTemplateMsgListService implements WxaNewTmplListPort {

	private static final Map<String, List<String>> SOURCE_TYPE_TO_SCENES;

	static {
		Map<String, List<String>> m = new LinkedHashMap<>();
		m.put("logistics_order", List.of("paymentSucc", "payOrdersRemind", "orderDeliverySucc"));
		m.put("ziti_order", List.of("paymentSucc", "payOrdersRemind", "pickSuccess"));
		m.put("after_refund", List.of("aftersalesRefuse"));
		m.put(
				"activity",
				List.of("reservationRemind", "registrationResultNotice", "registrationActivityNotice"));
		m.put("member", List.of("memberCreateSucc"));
		m.put("coupon", List.of("userGetCardSucc"));
		m.put("goods", List.of("goodsArrivalNotice"));
		SOURCE_TYPE_TO_SCENES = Collections.unmodifiableMap(m);
	}

	private final WxaNoticeTemplateMapper wxaNoticeTemplateMapper;

	public WxaTemplateMsgListService(WxaNoticeTemplateMapper wxaNoticeTemplateMapper) {
		this.wxaNoticeTemplateMapper = wxaNoticeTemplateMapper;
	}

	@Override
	public List<String> listTemplateIds(long companyId, String sourceType, String tempName) {
		if (sourceType == null || tempName == null) {
			return List.of();
		}
		List<String> scenes = SOURCE_TYPE_TO_SCENES.getOrDefault(sourceType, List.of());
		if (scenes.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<WxaNoticeTemplate> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(WxaNoticeTemplate::getCompanyId, companyId)
				.eq(WxaNoticeTemplate::getTemplateName, tempName)
				.eq(WxaNoticeTemplate::getIsOpen, Boolean.TRUE)
				.in(WxaNoticeTemplate::getScenesName, scenes)
				.select(WxaNoticeTemplate::getTemplateId);
		List<WxaNoticeTemplate> rows = wxaNoticeTemplateMapper.selectList(wrapper);
		List<String> result = new ArrayList<>(rows.size());
		for (WxaNoticeTemplate row : rows) {
			result.add(row.getTemplateId());
		}
		return result;
	}
}
