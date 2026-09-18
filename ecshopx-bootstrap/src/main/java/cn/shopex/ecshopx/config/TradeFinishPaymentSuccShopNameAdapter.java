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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.port.orders.TradeFinishPaymentSuccShopNamePort;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TradeFinishPaymentSuccShopNameAdapter implements TradeFinishPaymentSuccShopNamePort {

	private final WxShopsMapper wxShopsMapper;
	private final WechatAuthQueryService wechatAuthQueryService;

	public TradeFinishPaymentSuccShopNameAdapter(
			WxShopsMapper wxShopsMapper, WechatAuthQueryService wechatAuthQueryService) {
		this.wxShopsMapper = wxShopsMapper;
		this.wechatAuthQueryService = wechatAuthQueryService;
	}

	@Override
	public String resolveForSubscribeTemplate(long companyId, String orderClass, String tradeShopId, String wxaAppid) {
		String cls = orderClass == null ? "" : orderClass.trim();
		if (!StringUtils.hasText(cls) || "community".equalsIgnoreCase(cls)) {
			return fallbackFromAuthorizer(companyId, wxaAppid);
		}
		Long wxShopId = parseLongFlexible(tradeShopId);
		if (wxShopId != null && wxShopId > 0) {
			WxShops row =
					wxShopsMapper.selectOne(
							new LambdaQueryWrapper<WxShops>()
									.eq(WxShops::getWxShopId, wxShopId)
									.eq(WxShops::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (row != null && StringUtils.hasText(row.getStoreName())) {
				return row.getStoreName().trim();
			}
		}
		return fallbackFromAuthorizer(companyId, wxaAppid);
	}

	private String fallbackFromAuthorizer(long companyId, String wxaAppid) {
		if (!StringUtils.hasText(wxaAppid)) {
			return "";
		}
		try {
			Map<String, Object> info = wechatAuthQueryService.getAuthorizerInfo(companyId, wxaAppid.trim());
			if (info == null || info.isEmpty()) {
				return "";
			}
			Object nick = info.get("nick_name");
			if (nick != null && StringUtils.hasText(String.valueOf(nick).trim())) {
				return String.valueOf(nick).trim();
			}
			Object principal = info.get("principal_name");
			return principal == null ? "" : String.valueOf(principal).trim();
		} catch (RuntimeException e) {
			return "";
		}
	}

	private static Long parseLongFlexible(String s) {
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
