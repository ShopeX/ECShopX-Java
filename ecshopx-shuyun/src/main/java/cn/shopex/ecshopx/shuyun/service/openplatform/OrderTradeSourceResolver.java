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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderTradeSourceResolver {

	private static final Logger log = LoggerFactory.getLogger(OrderTradeSourceResolver.class);
	public static final String UNKNOWN_LOG_KEYWORD = "shuyun_open_platform_trade_source_unknown";

	private final ShuyunOpenPlatformProperties properties;

	public OrderTradeSourceResolver(ShuyunOpenPlatformProperties properties) {
		this.properties = properties;
	}

	public String resolve(long companyId, String orderId, String orderClass) {
		String normalized = orderClass == null ? "" : orderClass.trim().toLowerCase(Locale.ROOT);
		if (!StringUtils.hasText(normalized)) {
			log.error(
					"{}: trade_source not configured for order_class companyId={} orderId={} reason=empty_order_class",
					UNKNOWN_LOG_KEYWORD,
					companyId,
					orderId);
			return null;
		}
		Map<String, String> map = properties.getOrderClassTradeSourceMap();
		if (map == null || !map.containsKey(normalized)) {
			log.error(
					"{}: trade_source not configured for order_class companyId={} orderId={} orderClass={} reason=missing_map_key",
					UNKNOWN_LOG_KEYWORD,
					companyId,
					orderId,
					orderClass);
			return null;
		}
		String value = map.get(normalized);
		if (!StringUtils.hasText(value)) {
			log.error(
					"{}: trade_source not configured for order_class companyId={} orderId={} orderClass={} reason=empty_map_value",
					UNKNOWN_LOG_KEYWORD,
					companyId,
					orderId,
					orderClass);
			return null;
		}
		return value.trim();
	}
}
