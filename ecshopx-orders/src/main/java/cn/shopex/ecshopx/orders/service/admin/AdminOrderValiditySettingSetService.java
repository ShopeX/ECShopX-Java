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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.dispatch.RefundFreightAutoZyEventDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingDefaults;
import cn.shopex.ecshopx.orders.service.setting.OrdersErpSettingRedisReadService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderValiditySettingSetService {

	private final OrderValiditySettingRedisWriteService orderValiditySettingRedisWriteService;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OrdersErpSettingRedisReadService ordersErpSettingRedisReadService;
	private final RefundFreightAutoZyEventDispatchPublisher refundFreightAutoZyEventDispatchPublisher;

	public AdminOrderValiditySettingSetService(
			OrderValiditySettingRedisWriteService orderValiditySettingRedisWriteService,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			NormalOrdersMapper normalOrdersMapper,
			OrdersErpSettingRedisReadService ordersErpSettingRedisReadService,
			RefundFreightAutoZyEventDispatchPublisher refundFreightAutoZyEventDispatchPublisher) {
		this.orderValiditySettingRedisWriteService = orderValiditySettingRedisWriteService;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.ordersErpSettingRedisReadService = ordersErpSettingRedisReadService;
		this.refundFreightAutoZyEventDispatchPublisher = refundFreightAutoZyEventDispatchPublisher;
	}

	public Map<String, Object> setOrderSetting(long companyId, Map<String, Object> mergedInput) {
		Object rawAutoAftersales = mergedInput.get("auto_aftersales");
		boolean normalizedAutoAftersales = autoAftersalesFromInput(mergedInput);

		int orderCancelTime = parseIntFromInput(mergedInput.get("order_cancel_time"), 15);
		Object rawOrderFinish = mergedInput.get("order_finish_time");
		Object orderFinishToStore;
		if (!mergedInput.containsKey("order_finish_time")
				|| rawOrderFinish == null
				|| isEmptyScalarForOrderFinishTime(rawOrderFinish)) {
			orderFinishToStore = OrderValiditySettingDefaults.DEFAULTS.get("order_finish_time");
		} else {
			orderFinishToStore = rawOrderFinish;
		}
		int latestAftersaleTime = parseIntFromInput(mergedInput.get("latest_aftersale_time"), 0);
		int autoRefuseTime = parseIntFromInput(mergedInput.get("auto_refuse_time"), 0);
		boolean offlineAftersales = offlineAftersalesFromInput(mergedInput);
		int isRefundFreight = parseIntFromInput(mergedInput.get("is_refund_freight"), 0);

		Map<String, Object> toStore = new LinkedHashMap<>();
		toStore.put("order_cancel_time", orderCancelTime);
		toStore.put("order_finish_time", orderFinishToStore);
		toStore.put("latest_aftersale_time", latestAftersaleTime);
		toStore.put("auto_refuse_time", autoRefuseTime);
		toStore.put("auto_aftersales", normalizedAutoAftersales);
		toStore.put("offline_aftersales", offlineAftersales);
		toStore.put("is_refund_freight", isRefundFreight);

		if (orderCancelTime < 5) {
			throw new ResourceException("订单自动取消时间需大于等于5分钟");
		}

		if (autoAftersalesEnabledForErpCheck(normalizedAutoAftersales, rawAutoAftersales)) {
			checkErpAndPendingCancelCount(companyId, ordersErpSettingRedisReadService.readJushuitanSetting(companyId));
			checkErpAndPendingCancelCount(companyId, ordersErpSettingRedisReadService.readWdtErpSetting(companyId));
		}

		if (looseEqualsOne(mergedInput.get("is_refund_freight"))) {
			refundFreightAutoZyEventDispatchPublisher.publish(companyId, isRefundFreight);
		}

		orderValiditySettingRedisWriteService.writeCompanySettingJson(companyId, toStore);
		return orderValiditySettingRedisReadService.readPlatformSetting(companyId);
	}

	private void checkErpAndPendingCancelCount(long companyId, Map<String, Object> erpSetting) {
		if (!erpIsOpenTruthy(erpSetting.get("is_open"))) {
			return;
		}
		long cnt =
				normalOrdersMapper.selectCount(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getCancelStatus, "WAIT_PROCESS")
								.eq(NormalOrders::getOrderStatus, "PAYED"));
		if (cnt > 0) {
			throw new ResourceException("有未审核的取消订单申请，不能开启自动审批同意");
		}
	}

	/** PHP Order@setOrderSetting: only boolean {@code true} or string {@code "true"} enables offline aftersales. */
	private static boolean offlineAftersalesFromInput(Map<String, Object> mergedInput) {
		if (!mergedInput.containsKey("offline_aftersales")) {
			return false;
		}
		Object value = mergedInput.get("offline_aftersales");
		if (value == null) {
			return false;
		}
		return Boolean.TRUE.equals(value) || "true".equals(String.valueOf(value));
	}

	/** PHP Order@setOrderSetting: present, truthy, and not string {@code "false"}. */
	private static boolean autoAftersalesFromInput(Map<String, Object> mergedInput) {
		if (!mergedInput.containsKey("auto_aftersales")) {
			return false;
		}
		Object value = mergedInput.get("auto_aftersales");
		if (value == null) {
			return false;
		}
		if (Boolean.FALSE.equals(value)) {
			return false;
		}
		if (value instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		if (value instanceof String s) {
			if (s.isEmpty() || "0".equals(s) || "false".equals(s)) {
				return false;
			}
			return true;
		}
		if (Boolean.TRUE.equals(value)) {
			return true;
		}
		return true;
	}

	private static boolean looseEqualsOne(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		return "1".equals(String.valueOf(v).trim());
	}

	private static boolean erpIsOpenTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static boolean autoAftersalesEnabledForErpCheck(
			Object normalizedAutoAftersales, Object rawFromInput) {
		if (Boolean.TRUE.equals(normalizedAutoAftersales)) {
			return true;
		}
		return "true".equalsIgnoreCase(String.valueOf(rawFromInput == null ? "" : rawFromInput).trim());
	}

	private static int parseIntFromInput(Object v, int dflt) {
		if (v == null) {
			return dflt;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return dflt;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private static boolean isEmptyScalarForOrderFinishTime(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return s.trim().isEmpty();
		}
		return false;
	}
}
