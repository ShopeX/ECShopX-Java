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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.order.JushuitanTradeCancelAckPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.orders.domain.OrdersRelJushuitan;
import cn.shopex.ecshopx.orders.mapper.OrdersRelJushuitanMapper;
import cn.shopex.ecshopx.systemlink.jushuitan.JushuitanOpenApiClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JushuitanTradeCancelNotificationProcessor {

	private static final Logger log = LoggerFactory.getLogger(JushuitanTradeCancelNotificationProcessor.class);

	private final JushuitanSettingReadPort jushuitanSettingReadPort;
	private final DistributorListQueryService distributorListQueryService;
	private final JushuitanOpenApiClient jushuitanOpenApiClient;
	private final OrdersRelJushuitanMapper ordersRelJushuitanMapper;
	private final JushuitanTradeCancelAckPort jushuitanTradeCancelAckPort;

	public JushuitanTradeCancelNotificationProcessor(
			JushuitanSettingReadPort jushuitanSettingReadPort,
			DistributorListQueryService distributorListQueryService,
			JushuitanOpenApiClient jushuitanOpenApiClient,
			OrdersRelJushuitanMapper ordersRelJushuitanMapper,
			JushuitanTradeCancelAckPort jushuitanTradeCancelAckPort) {
		this.jushuitanSettingReadPort = jushuitanSettingReadPort;
		this.distributorListQueryService = distributorListQueryService;
		this.jushuitanOpenApiClient = jushuitanOpenApiClient;
		this.ordersRelJushuitanMapper = ordersRelJushuitanMapper;
		this.jushuitanTradeCancelAckPort = jushuitanTradeCancelAckPort;
	}

	public void handle(Map<String, Object> entities) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		Long companyId = longVal(entities.get("company_id"));
		if (companyId == null || companyId <= 0) {
			return;
		}
		long orderId = longOrZero(entities.get("order_id"));
		if (orderId <= 0) {
			return;
		}
		Map<String, Object> setting = jushuitanSettingReadPort.readJushuitanSetting(companyId);
		if (!isJushuitanOpen(setting)) {
			log.debug("jushuitan trade cancel skipped: ERP not enabled companyId={}", companyId);
			return;
		}
		long distributorId = longOrZero(entities.get("distributor_id"));
		String jstShopId = resolveJstShopId(companyId, distributorId, setting);
		if (distributorId > 0 && !StringUtils.hasText(jstShopId)) {
			log.debug(
					"jushuitan trade cancel skipped: distributor without jst_shop_id companyId={} distributorId={}",
					companyId,
					distributorId);
			return;
		}
		OrdersRelJushuitan rel =
				ordersRelJushuitanMapper.selectOne(
						new LambdaQueryWrapper<OrdersRelJushuitan>()
								.eq(OrdersRelJushuitan::getCompanyId, companyId)
								.eq(OrdersRelJushuitan::getOrderId, orderId)
								.last("LIMIT 1"));
		if (rel == null || rel.getOId() == null || rel.getOId() <= 0) {
			log.debug("jushuitan trade cancel skipped: no platform link row companyId={} orderId={}", companyId, orderId);
			return;
		}
		String accessToken = setting.get("access_token") != null ? setting.get("access_token").toString() : "";
		Map<String, Object> biz = new LinkedHashMap<>();
		biz.put("o_id", rel.getOId());
		biz.put("so_id", String.valueOf(orderId));
		if (StringUtils.hasText(jstShopId)) {
			biz.put("shop_id", jstShopId);
		}
		String cancelReason = entities.get("cancel_reason") == null ? "" : String.valueOf(entities.get("cancel_reason"));
		if (StringUtils.hasText(cancelReason)) {
			biz.put("remark", cancelReason);
		}
		Map<String, Object> result = jushuitanOpenApiClient.call(companyId, "order_cancel", biz, accessToken);
		if (isUnconfiguredClient(result)) {
			log.debug("jushuitan trade cancel skipped: client not configured companyId={}", companyId);
			return;
		}
		if (isHardFailure(result)) {
			throw new ResourceException("聚水潭订单取消失败: " + summarizeFailure(result));
		}
		if (!isApiSuccess(result)) {
			throw new ResourceException("聚水潭订单取消失败: " + summarizeFailure(result));
		}
		String action = entities.get("action") == null ? "" : String.valueOf(entities.get("action")).trim();
		if ("cancel_order".equals(action)) {
			jushuitanTradeCancelAckPort.acknowledgeAfterPlatformCancel(companyId, orderId, entities);
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

	private static boolean isUnconfiguredClient(Map<String, Object> result) {
		Object fail = result != null ? result.get("fail_msg") : null;
		if (fail == null) {
			return false;
		}
		String m = fail.toString();
		return m.contains("jushuitan client not configured") || m.contains("missing base-url");
	}

	private static boolean isHardFailure(Map<String, Object> result) {
		if (result == null || result.isEmpty()) {
			return false;
		}
		Object fail = result.get("fail_msg");
		if (fail != null && StringUtils.hasText(fail.toString())) {
			return true;
		}
		if ("false".equalsIgnoreCase(String.valueOf(result.get("status")))) {
			return true;
		}
		return false;
	}

	private static boolean isApiSuccess(Map<String, Object> result) {
		if (result == null) {
			return false;
		}
		Object code = result.get("code");
		if (code instanceof Number n) {
			return n.intValue() == 0;
		}
		return "0".equals(String.valueOf(code));
	}

	private static String summarizeFailure(Map<String, Object> result) {
		Object fail = result.get("fail_msg");
		if (fail != null && StringUtils.hasText(fail.toString())) {
			return fail.toString();
		}
		return result.toString();
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
