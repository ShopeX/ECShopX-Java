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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.companys.service.setting.TradeRateSettingRedisService;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListExecutorKind;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListQueryExecutor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOrderCountOrdersService {

	private final AdminOrderListQueryExecutor adminOrderListQueryExecutor;

	private final OrderAssociationsMapper orderAssociationsMapper;

	private final AftersalesMapper aftersalesMapper;

	private final TradeRateSettingRedisService tradeRateSettingRedisService;

	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;

	public WxappOrderCountOrdersService(
			AdminOrderListQueryExecutor adminOrderListQueryExecutor,
			OrderAssociationsMapper orderAssociationsMapper,
			AftersalesMapper aftersalesMapper,
			TradeRateSettingRedisService tradeRateSettingRedisService,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort) {
		this.adminOrderListQueryExecutor = adminOrderListQueryExecutor;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.aftersalesMapper = aftersalesMapper;
		this.tradeRateSettingRedisService = tradeRateSettingRedisService;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
	}

	public Map<String, Object> countOrders(HttpServletRequest request, Map<String, Object> auth) {
		if (isUserIdFalsy(auth.get("user_id"))) {
			return new LinkedHashMap<>();
		}

		long userId = longVal(auth.get("user_id"));
		long companyId = longVal(auth.get("company_id"));
		Map<String, Object> paramMap = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();

		long distributorIdForFilter = 0L;
		if (wxappQueryTruthy(paramMap.get("is_distribution"))) {
			String mobile = auth.get("mobile") == null ? "" : String.valueOf(auth.get("mobile"));
			Map<String, Object> infoRow =
					adminOrderDetailDistributionSupportPort.resolveWxappListDistributorFilter(companyId, mobile);
			if (infoRow != null && !infoRow.isEmpty()) {
				Object raw = infoRow.get("distributor_id");
				if (raw instanceof Number && ((Number) raw).longValue() > 0L) {
					distributorIdForFilter = ((Number) raw).longValue();
				} else if (raw != null) {
					String s = String.valueOf(raw).trim();
					if (!s.isEmpty()) {
						try {
							long v = Long.parseLong(s);
							if (v > 0L) {
								distributorIdForFilter = v;
							}
						} catch (NumberFormatException ignored) {
						}
					}
				}
			}
		}

		boolean distReq = wxappQueryTruthy(paramMap.get("is_distribution"));

		Object orderTypeRaw = paramMap.get("order_type");
		String orderTypeNorm =
				!paramMap.containsKey("order_type")
						? "service"
						: (orderTypeRaw == null ? "" : String.valueOf(orderTypeRaw).trim());

		switch (orderTypeNorm) {
			case "service" -> {
				result.put(
						"service_notpay",
						countAssoc(companyId, userId, distributorIdForFilter, distReq, "service", "NOTPAY"));
				result.put(
						"service_payed",
						countAssoc(companyId, userId, distributorIdForFilter, distReq, "service", "DONE"));
			}
			case "bargain" -> {
				result.put(
						"service_notpay",
						countAssoc(companyId, userId, distributorIdForFilter, distReq, "bargain", "NOTPAY"));
				result.put(
						"service_payed",
						countAssoc(companyId, userId, distributorIdForFilter, distReq, "bargain", "DONE"));
			}
			case "normal" -> {
				List<String> classes = resolveOrderClasses(paramMap);
				LinkedHashMap<String, Object> base = new LinkedHashMap<>();
				base.put("company_id", companyId);
				base.put("supplier_id", 0);
				base.put("order_class|in", classes);
				if (paramMap.containsKey("activity_id")) {
					Object v = paramMap.get("activity_id");
					if (v != null && StringUtils.hasText(String.valueOf(v).trim())) {
						base.put("act_id", String.valueOf(v).trim());
					}
				}
				if (distReq) {
					base.put("distributor_id", distributorIdForFilter);
				} else {
					base.put("user_id", userId);
				}

				LinkedHashMap<String, Object> f1 = new LinkedHashMap<>(base);
				f1.put("order_status", "NOTPAY");
				result.put("normal_notpay_notdelivery", adminOrderListQueryExecutor.countNormalOrders(
						AdminOrderListExecutorKind.NORMAL, f1));

				LinkedHashMap<String, Object> f2 = new LinkedHashMap<>(base);
				f2.put("order_status|in", List.of("PAYED", "WAIT_BUYER_CONFIRM"));
				f2.put("ziti_status", "NOTZITI");
				result.put("normal_payed_notdelivery", adminOrderListQueryExecutor.countNormalOrders(
						AdminOrderListExecutorKind.NORMAL, f2));

				LinkedHashMap<String, Object> f3 = new LinkedHashMap<>(base);
				f3.put("order_status", "PAYED");
				f3.put("ziti_status", "NOTZITI");
				result.put("normal_payed_daifahuo", adminOrderListQueryExecutor.countNormalOrders(
						AdminOrderListExecutorKind.NORMAL, f3));

				// 与订单列表 status=1（待收货）筛选条件保持一致
				LinkedHashMap<String, Object> f4 = new LinkedHashMap<>(base);
				f4.put("order_status", "WAIT_BUYER_CONFIRM");
				f4.put("delivery_status", "DONE");
				f4.put("ziti_status", "NOTZITI");
				result.put("normal_payed_daishouhuo", adminOrderListQueryExecutor.countNormalOrders(
						AdminOrderListExecutorKind.NORMAL, f4));

				LinkedHashMap<String, Object> f5 = new LinkedHashMap<>(base);
				f5.put("order_status", "PAYED");
				f5.put("ziti_status", "PENDING");
				if (distReq) {
					f5.put("wxapp_order_is_distribution_eq", Boolean.TRUE);
				}
				result.put("normal_payed_daiziti", adminOrderListQueryExecutor.countNormalOrders(
						AdminOrderListExecutorKind.NORMAL, f5));

				LinkedHashMap<String, Object> f6 = new LinkedHashMap<>(base);
				f6.put("order_status", "DONE");
				f6.put("is_rate", "0");
				result.put("normal_not_rate", adminOrderListQueryExecutor.countNormalOrders(
						AdminOrderListExecutorKind.NORMAL, f6));

				LambdaQueryWrapper<Aftersales> w0 =
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getUserId, userId)
								.eq(Aftersales::getAftersalesStatus, 0);
				LambdaQueryWrapper<Aftersales> w1 =
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getUserId, userId)
								.eq(Aftersales::getAftersalesStatus, 1);
				LambdaQueryWrapper<Aftersales> w01 =
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getUserId, userId)
								.in(Aftersales::getAftersalesStatus, 0, 1);
				Long c0 = aftersalesMapper.selectCount(w0);
				Long c1 = aftersalesMapper.selectCount(w1);
				Long c01 = aftersalesMapper.selectCount(w01);
				result.put("aftersales_pending", c0 == null ? 0L : c0.longValue());
				result.put("aftersales_processing", c1 == null ? 0L : c1.longValue());
				result.put("aftersales", c01 == null ? 0L : c01.longValue());
			}
			default -> {
			}
		}

		boolean rateStatus = resolveRateStatus(companyId);
		result.put("rate_status", rateStatus);
		return result;
	}

	private static List<String> defaultOrderClasses() {
		return List.of(
				"normal",
				"groups",
				"seckill",
				"shopguide",
				"bargain",
				"pointsmall",
				"excard",
				"employee_purchase");
	}

	private static List<String> resolveOrderClasses(Map<String, Object> paramMap) {
		if (!paramMap.containsKey("order_class")) {
			return defaultOrderClasses();
		}
		Object raw = paramMap.get("order_class");
		if (raw == null) {
			return defaultOrderClasses();
		}
		if (raw instanceof Collection<?> c) {
			if (c.isEmpty()) {
				return defaultOrderClasses();
			}
			List<String> out = new ArrayList<>();
			for (Object o : c) {
				String s = o == null ? "" : String.valueOf(o).trim();
				if (!s.isEmpty()) {
					out.add(s);
				}
			}
			return out.isEmpty() ? defaultOrderClasses() : out;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return defaultOrderClasses();
		}
		List<String> parts = new ArrayList<>();
		for (String p : s.split(",")) {
			String t = p.trim();
			if (!t.isEmpty()) {
				parts.add(t);
			}
		}
		return parts.isEmpty() ? defaultOrderClasses() : parts;
	}

	private long countAssoc(
			long companyId,
			long userId,
			long distributorIdWhenDist,
			boolean distributionMode,
			String orderType,
			String orderStatus) {
		LambdaQueryWrapper<OrderAssociations> w = new LambdaQueryWrapper<>();
		w.eq(OrderAssociations::getCompanyId, companyId);
		w.eq(OrderAssociations::getOrderType, orderType);
		w.eq(OrderAssociations::getOrderStatus, orderStatus);
		if (distributionMode) {
			w.eq(OrderAssociations::getShopId, distributorIdWhenDist);
		} else {
			w.eq(OrderAssociations::getUserId, userId);
		}
		Long c = orderAssociationsMapper.selectCount(w);
		return c == null ? 0L : c.longValue();
	}

	private boolean resolveRateStatus(long companyId) {
		Object raw = tradeRateSettingRedisService.getRateSettingStatus(companyId);
		if (!(raw instanceof Map<?, ?> m)) {
			return false;
		}
		Object rs = m.get("rate_status");
		if (rs instanceof Boolean b) {
			return b;
		}
		if (rs != null && "true".equalsIgnoreCase(String.valueOf(rs).trim())) {
			return true;
		}
		return false;
	}

	private static boolean wxappQueryTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = String.valueOf(v).trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private static boolean isUserIdFalsy(Object uidObj) {
		if (uidObj == null) {
			return true;
		}
		if (uidObj instanceof Boolean b) {
			return !b;
		}
		String s = uidObj.toString().trim();
		if (s.isEmpty() || "0".equals(s)) {
			return true;
		}
		return longVal(uidObj) <= 0L;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
