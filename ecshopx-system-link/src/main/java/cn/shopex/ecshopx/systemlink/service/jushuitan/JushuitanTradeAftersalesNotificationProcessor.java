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

package cn.shopex.ecshopx.systemlink.service.jushuitan;

import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.systemlink.JushuitanAftersaleAddPayloadAssemblePort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.orders.domain.OrdersRelJushuitan;
import cn.shopex.ecshopx.orders.mapper.OrdersRelJushuitanMapper;
import cn.shopex.ecshopx.systemlink.jushuitan.JushuitanOpenApiClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JushuitanTradeAftersalesNotificationProcessor {

	private static final Logger log =
			LoggerFactory.getLogger(JushuitanTradeAftersalesNotificationProcessor.class);

	private final JushuitanSettingReadPort jushuitanSettingReadPort;
	private final DistributorListQueryService distributorListQueryService;
	private final JushuitanOpenApiClient jushuitanOpenApiClient;
	private final JushuitanAftersaleAddPayloadAssemblePort jushuitanAftersaleAddPayloadAssemblePort;
	private final OrdersRelJushuitanMapper ordersRelJushuitanMapper;

	public JushuitanTradeAftersalesNotificationProcessor(
			JushuitanSettingReadPort jushuitanSettingReadPort,
			DistributorListQueryService distributorListQueryService,
			JushuitanOpenApiClient jushuitanOpenApiClient,
			JushuitanAftersaleAddPayloadAssemblePort jushuitanAftersaleAddPayloadAssemblePort,
			OrdersRelJushuitanMapper ordersRelJushuitanMapper) {
		this.jushuitanSettingReadPort = jushuitanSettingReadPort;
		this.distributorListQueryService = distributorListQueryService;
		this.jushuitanOpenApiClient = jushuitanOpenApiClient;
		this.jushuitanAftersaleAddPayloadAssemblePort = jushuitanAftersaleAddPayloadAssemblePort;
		this.ordersRelJushuitanMapper = ordersRelJushuitanMapper;
	}

	public void handle(Map<String, Object> aftersalesPayload) {
		if (aftersalesPayload == null || aftersalesPayload.isEmpty()) {
			return;
		}
		Long companyId = longVal(aftersalesPayload.get("company_id"));
		if (companyId == null || companyId <= 0) {
			return;
		}
		final long companyIdVal = companyId;
		try {
			Map<String, Object> setting = jushuitanSettingReadPort.readJushuitanSetting(companyIdVal);
			if (!isJushuitanOpen(setting)) {
				log.debug("jushuitan trade aftersales skipped: ERP not enabled companyId={}", companyIdVal);
				return;
			}
			long distributorId = longOrZero(aftersalesPayload.get("distributor_id"));
			String jstShopId = resolveJstShopId(companyIdVal, distributorId, setting);
			if (distributorId > 0 && !StringUtils.hasText(jstShopId)) {
				log.debug(
						"jushuitan trade aftersales skipped: distributor without jst_shop_id companyId={} distributorId={}",
						companyIdVal,
						distributorId);
				return;
			}
			long orderId = longOrZero(aftersalesPayload.get("order_id"));
			if (orderId <= 0) {
				return;
			}
			OrdersRelJushuitan rel =
					ordersRelJushuitanMapper.selectOne(
							new LambdaQueryWrapper<OrdersRelJushuitan>()
									.eq(OrdersRelJushuitan::getCompanyId, companyIdVal)
									.eq(OrdersRelJushuitan::getOrderId, orderId)
									.last("LIMIT 1"));
			if (rel == null || rel.getOId() == null || rel.getOId() <= 0) {
				log.debug(
						"jushuitan trade aftersales skipped: no platform link row companyId={} orderId={}",
						companyIdVal,
						orderId);
				return;
			}
			Optional<Map<String, Object>> bizOpt =
					jushuitanAftersaleAddPayloadAssemblePort.assemble(companyIdVal, aftersalesPayload);
			if (bizOpt.isEmpty()) {
				log.debug("jushuitan trade aftersales skipped: empty assemble companyId={}", companyIdVal);
				return;
			}
			Map<String, Object> biz = new LinkedHashMap<>(bizOpt.get());
			if (StringUtils.hasText(jstShopId)) {
				biz.put("shop_id", jstShopId);
			}
			String accessToken = setting.get("access_token") != null ? setting.get("access_token").toString() : "";
			jushuitanOpenApiClient.call(companyIdVal, "aftersale_add", biz, accessToken);
		} catch (Exception e) {
			log.debug(
					"jushuitan trade aftersales swallowed error companyId={} msg={}",
					companyIdVal,
					e.getMessage());
		}
	}

	private static boolean isJushuitanOpen(Map<String, Object> setting) {
		Object open = setting.get("is_open");
		if (open instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(String.valueOf(open));
	}

	private String resolveJstShopId(long companyId, long distributorId, Map<String, Object> setting) {
		Object shopIdObj = setting.get("shop_id");
		String shopId = shopIdObj != null ? shopIdObj.toString().trim() : "";
		if (distributorId > 0) {
			List<Distributor> dist =
					distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
			if (dist == null || dist.isEmpty()) {
				return "";
			}
			Long jst = dist.get(0).getJstShopId();
			if (jst == null || jst <= 0) {
				return "";
			}
			return jst.toString();
		}
		return shopId;
	}

	private static Long longVal(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longOrZero(Object o) {
		Long v = longVal(o);
		return v == null ? 0L : v;
	}
}
