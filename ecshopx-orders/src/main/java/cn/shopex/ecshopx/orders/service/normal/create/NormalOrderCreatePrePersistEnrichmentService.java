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

package cn.shopex.ecshopx.orders.service.normal.create;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderMemberGetPointsService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderCreatePrePersistEnrichmentService {

	private final NormalOrderMemberGetPointsService normalOrderMemberGetPointsService;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	public NormalOrderCreatePrePersistEnrichmentService(
			NormalOrderMemberGetPointsService normalOrderMemberGetPointsService,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService) {
		this.normalOrderMemberGetPointsService = normalOrderMemberGetPointsService;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
	}

	@SuppressWarnings("unchecked")
	public void applyBeforePersist(NormalOrderCreateParams p) {
		Map<String, Object> od = p.getOrderData();
		if (od == null || od.isEmpty()) {
			return;
		}
		Map<String, Object> pr = p.getParams();
		if (pr != null && pr.get("source_from") != null) {
			od.put("source_from", pr.get("source_from"));
		}
		od.put("get_point_type", 1);
		applyCostFees(od);
		applyAutoCancelTimeIfMissing(od, pr);

		long companyId = longVal(od.get("company_id"), 0L);
		long userId = longVal(od.get("user_id"), 0L);
		String orderType = stringVal(od.get("order_type"));
		if (!oemShuyun && userId > 0L && "normal".equals(orderType)) {
			normalOrderMemberGetPointsService.applyMemberGetPoints(companyId, od);
		}
	}

	private void applyAutoCancelTimeIfMissing(Map<String, Object> od, Map<String, Object> pr) {
		Object existing = od.get("auto_cancel_time");
		if (existing != null && StringUtils.hasText(String.valueOf(existing).trim())) {
			return;
		}
		long companyId = longVal(od.get("company_id"), 0L);
		if (companyId <= 0L) {
			return;
		}
		String payType = stringVal(pr != null ? pr.get("pay_type") : null);
		if (!StringUtils.hasText(payType)) {
			payType = stringVal(od.get("pay_type"));
		}
		int cancelMinutes = resolveCancelMinutes(companyId, payType);
		od.put("auto_cancel_time", Instant.now().getEpochSecond() + cancelMinutes * 60L);
	}

	private int resolveCancelMinutes(long companyId, String payType) {
		if ("offline".equals(payType) || "offline_pay".equals(payType)) {
			return 24 * 60 * 365;
		}
		Map<String, Object> setting = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
		Object raw = setting.get("order_cancel_time");
		if (raw instanceof Number n) {
			return Math.max(1, n.intValue());
		}
		try {
			return Math.max(1, Integer.parseInt(String.valueOf(raw).trim()));
		} catch (Exception e) {
			return 15;
		}
	}

	@SuppressWarnings("unchecked")
	private static void applyCostFees(Map<String, Object> od) {
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList)) {
			od.put("cost_fee", 0);
			return;
		}
		long orderCostFee = 0L;
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> rowRaw)) {
				continue;
			}
			Map<String, Object> line = (Map<String, Object>) rowRaw;
			int num = intVal(line.get("num"), 1);
			int unitCost = intVal(line.get("cost_price"), 0);
			int lineCostFee = unitCost * num;
			line.put("cost_fee", lineCostFee);
			orderCostFee += lineCostFee;
		}
		od.put("cost_fee", orderCostFee > 0L ? orderCostFee : 0L);
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
