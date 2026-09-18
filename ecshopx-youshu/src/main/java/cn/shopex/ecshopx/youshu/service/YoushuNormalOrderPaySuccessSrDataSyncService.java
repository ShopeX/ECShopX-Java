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

package cn.shopex.ecshopx.youshu.service;

import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.youshu.domain.YoushuSetting;
import cn.shopex.ecshopx.youshu.integration.YoushuOrderPushPort;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class YoushuNormalOrderPaySuccessSrDataSyncService {

	private static final int DATA_SOURCE_TYPE_ORDER = 0;

	private final YoushuSettingMapper youshuSettingMapper;
	private final YoushuDataSourceApiPort youshuDataSourceApiPort;
	private final YoushuOrderPushPort youshuOrderPushPort;
	private final NormalOrdersMapper normalOrdersMapper;

	public YoushuNormalOrderPaySuccessSrDataSyncService(
			YoushuSettingMapper youshuSettingMapper,
			YoushuDataSourceApiPort youshuDataSourceApiPort,
			YoushuOrderPushPort youshuOrderPushPort,
			NormalOrdersMapper normalOrdersMapper) {
		this.youshuSettingMapper = youshuSettingMapper;
		this.youshuDataSourceApiPort = youshuDataSourceApiPort;
		this.youshuOrderPushPort = youshuOrderPushPort;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	public void syncOrderAfterNormalPaySuccess(long companyId, Object orderIdRaw) {
		Long orderId = parseOrderId(orderIdRaw);
		if (orderId == null) {
			return;
		}

		YoushuSetting setting =
				youshuSettingMapper.selectOne(
						new LambdaQueryWrapper<YoushuSetting>()
								.eq(YoushuSetting::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (setting == null) {
			return;
		}
		String merchantId = setting.getMerchantId();
		if (!StringUtils.hasText(merchantId)) {
			return;
		}

		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (order == null) {
			return;
		}

		YoushuOpenApiCredentials credentials = toCredentials(setting);
		String dataSourceId =
				youshuDataSourceApiPort.getOrCreateDataSourceId(
						merchantId.trim(), DATA_SOURCE_TYPE_ORDER, credentials);
		youshuOrderPushPort.pushOrder(dataSourceId, buildOrderPayload(order), credentials);
	}

	private static Map<String, Object> buildOrderPayload(NormalOrders order) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("order_id", order.getOrderId());
		body.put("company_id", order.getCompanyId());
		if (order.getTotalFee() != null) {
			body.put("total_fee", order.getTotalFee());
		}
		if (order.getTitle() != null) {
			body.put("title", order.getTitle());
		}
		if (order.getPayType() != null) {
			body.put("pay_type", order.getPayType());
		}
		return body;
	}

	private static Long parseOrderId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static YoushuOpenApiCredentials toCredentials(YoushuSetting v) {
		String base = firstNonBlank(v.getApiUrl(), v.getSandboxApiUrl());
		String appId = firstNonBlank(v.getAppId(), v.getSandboxAppId());
		String appSecret = firstNonBlank(v.getAppSecret(), v.getSandboxAppSecret());
		return new YoushuOpenApiCredentials(base, appId, appSecret);
	}

	private static String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		if (fallback != null && !fallback.isBlank()) {
			return fallback;
		}
		return "";
	}
}
