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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailSalespersonLookupPort;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.CommunityOrderRelActivity;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminOrderDetailService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final AdminSupplierOrderDetailItemsOverlayService adminSupplierOrderDetailItemsOverlayService;
	private final OrderProfitMapper orderProfitMapper;
	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;
	private final MemberAccountService memberAccountService;
	private final OrderDeliveryTimelineService orderDeliveryTimelineService;
	private final AdminOrderDetailDataPassUiApplier adminOrderDetailDataPassUiApplier;
	private final AdminOrderDetailDerivedFeesService adminOrderDetailDerivedFeesService;
	private final AdminOrderDetailSalespersonLookupPort adminOrderDetailSalespersonLookupPort;
	private final CommunityOrderRelActivityMapper communityOrderRelActivityMapper;
	private final AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy;
	private final AdminOrderDetailPhpParityApplier adminOrderDetailPhpParityApplier;

	public AdminOrderDetailService(
			OrderAssociationsMapper orderAssociationsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			AdminSupplierOrderDetailItemsOverlayService adminSupplierOrderDetailItemsOverlayService,
			OrderProfitMapper orderProfitMapper,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort,
			MemberAccountService memberAccountService,
			OrderDeliveryTimelineService orderDeliveryTimelineService,
			AdminOrderDetailDataPassUiApplier adminOrderDetailDataPassUiApplier,
			AdminOrderDetailDerivedFeesService adminOrderDetailDerivedFeesService,
			AdminOrderDetailSalespersonLookupPort adminOrderDetailSalespersonLookupPort,
			CommunityOrderRelActivityMapper communityOrderRelActivityMapper,
			AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy,
			AdminOrderDetailPhpParityApplier adminOrderDetailPhpParityApplier) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.adminSupplierOrderDetailItemsOverlayService = adminSupplierOrderDetailItemsOverlayService;
		this.orderProfitMapper = orderProfitMapper;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
		this.memberAccountService = memberAccountService;
		this.orderDeliveryTimelineService = orderDeliveryTimelineService;
		this.adminOrderDetailDataPassUiApplier = adminOrderDetailDataPassUiApplier;
		this.adminOrderDetailDerivedFeesService = adminOrderDetailDerivedFeesService;
		this.adminOrderDetailSalespersonLookupPort = adminOrderDetailSalespersonLookupPort;
		this.communityOrderRelActivityMapper = communityOrderRelActivityMapper;
		this.adminEntityOrderDetailTypePolicy = adminEntityOrderDetailTypePolicy;
		this.adminOrderDetailPhpParityApplier = adminOrderDetailPhpParityApplier;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getOrderDetail(
			long companyId,
			String orderIdRaw,
			Map<String, Object> operatorJwt,
			HttpServletRequest request) {
		String raw = orderIdRaw == null ? "" : orderIdRaw.trim();
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("此订单不存在！");
		}
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new ResourceException("此订单不存在！");
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if (!adminEntityOrderDetailTypePolicy.supportsNormalPipeline(effective)) {
			throw new ResourceException("无此类型订单！");
		}

		Map<String, Object> result;
		try {
			result = adminNormalOrderDetailService.buildOrderBundle(companyId, raw, true);
		} catch (BadRequestException e) {
			throw e;
		}

		adminSupplierOrderDetailItemsOverlayService.apply(operatorJwt, result);

		result.put("profit", buildProfitPayload(companyId, raw));

		Map<String, Object> orderInfo = (Map<String, Object>) result.get("orderInfo");
		Map<String, Object> tradeInfo = (Map<String, Object>) result.get("tradeInfo");
		if (orderInfo == null) {
			throw new ResourceException("OrdersBundle/Order.order_not_found");
		}
		if (tradeInfo == null) {
			tradeInfo = new LinkedHashMap<>();
		}

		long userId = longVal(orderInfo.get("user_id"));
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		orderInfo.put("user_delete", memberInfo == null || memberInfo.isEmpty());

		Object appInfoObj = orderInfo.get("app_info");
		if (appInfoObj instanceof Map<?, ?> appInfoRaw && !appInfoRaw.isEmpty()) {
			Map<String, Object> appInfo = (Map<String, Object>) appInfoRaw;
			appInfo.put(
					"delivery_log",
					orderDeliveryTimelineService.buildDeliveryLog(
							orderInfo, tradeInfo, dadaSubMap(orderInfo)));
		}

		if (parseDatapassBlock(request) != 0) {
			adminOrderDetailDataPassUiApplier.applyBlockedAppInfoAndOrderMask(orderInfo);
		}

		if (orderInfo.containsKey("salesman_id")) {
			long spId = longVal(orderInfo.get("salesman_id"));
			Map<String, Object> spInfo =
					spId > 0L
							? adminOrderDetailSalespersonLookupPort.loadSalespersonForOrderDetail(companyId, spId)
							: Map.of();
			orderInfo.put("salespersonInfo", spInfo.isEmpty() ? new ArrayList<>() : new LinkedHashMap<>(spInfo));
		}

		if (orderInfo.containsKey("sale_salesman_distributor_id")) {
			long distId = longVal(orderInfo.get("sale_salesman_distributor_id"));
			Map<String, Object> distInfo =
					distId > 0L
							? adminOrderDetailDistributionSupportPort.getDistributorInfoFormatted(
									companyId, distId)
							: Map.of();
			orderInfo.put(
					"sale_salesman_distributor_info",
					distInfo.isEmpty() ? new ArrayList<>() : new LinkedHashMap<>(distInfo));
		}

		Map<String, Object> distributorMap = resolveDistributorMap(companyId, orderInfo);
		applyPlatformRefundFreightOnDistributor(companyId, distributorMap);
		orderInfo.put("distributor_name", str(distributorMap.get("name")));
		orderInfo.put("distributor_info", distributorMap);
		result.put("distributor", distributorMap);

		if ("community".equals(assoc.getOrderClass())) {
			CommunityOrderRelActivity rel = communityOrderRelActivityMapper.selectById(orderIdNum);
			result.put("community_activity", rel == null ? null : communityRelToMap(rel));
		}

		adminOrderDetailDerivedFeesService.apply(orderInfo);

		adminOrderDetailPhpParityApplier.apply(companyId, result);

		return result;
	}

	private static Map<String, Object> dadaSubMap(Map<String, Object> orderInfo) {
		Object d = orderInfo.get("dada");
		if (d instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> dm = (Map<String, Object>) m;
			return dm;
		}
		return new LinkedHashMap<>();
	}

	private Map<String, Object> resolveDistributorMap(long companyId, Map<String, Object> orderInfo) {
		long distributorId = longVal(orderInfo.get("distributor_id"));
		if (distributorId > 0L) {
			Map<String, Object> distributorMap =
					new LinkedHashMap<>(
							adminOrderDetailDistributionSupportPort.getDistributorInfoSimple(
									companyId, String.valueOf(distributorId)));
			if (distributorMap.isEmpty()) {
				return new LinkedHashMap<>(
						adminOrderDetailDistributionSupportPort.getDistributorSelfSimpleInfo(companyId));
			}
			Map<String, Object> formatted =
					adminOrderDetailDistributionSupportPort.getDistributorInfoFormatted(
							companyId, distributorId);
			for (String key : List.of("store_address", "store_name", "phone", "selfDeliveryRule", "rate")) {
				if (formatted.containsKey(key)) {
					distributorMap.put(key, formatted.get(key));
				}
			}
			return distributorMap;
		}
		return new LinkedHashMap<>(
				adminOrderDetailDistributionSupportPort.getDistributorSelfSimpleInfo(companyId));
	}

	private void applyPlatformRefundFreightOnDistributor(long companyId, Map<String, Object> distributorMap) {
		Map<String, Object> platform =
				adminOrderDetailDistributionSupportPort.readOrderValidityPlatformSetting(companyId);
		if (intVal(platform.get("is_refund_freight")) == 0) {
			distributorMap.put("is_refund_freight", 0);
		}
	}

	private Object buildProfitPayload(long companyId, String orderId) {
		long oid = parseOrderIdLong(orderId);
		OrderProfit p =
				orderProfitMapper.selectOne(
						new LambdaQueryWrapper<OrderProfit>().eq(OrderProfit::getOrderId, oid).last("LIMIT 1"));
		if (p == null) {
			return new ArrayList<>();
		}
		Map<String, Object> m = profitToMap(p);
		Long distId = p.getDistributorId();
		if (distId != null && distId > 0L) {
			m.put(
					"distributor_info",
					new LinkedHashMap<>(
							adminOrderDetailDistributionSupportPort.getDistributorInfoSimple(
									companyId, String.valueOf(distId))));
		} else {
			m.put("distributor_info", new LinkedHashMap<String, Object>());
		}
		m.put("seller_info", loadSalesperson(companyId, p.getSellerId()));
		m.put("popularize_seller_info", loadSalesperson(companyId, p.getPopularizeSellerId()));
		return m;
	}

	private Map<String, Object> loadSalesperson(long companyId, Long salespersonId) {
		if (salespersonId == null || salespersonId <= 0L) {
			return new LinkedHashMap<>();
		}
		return new LinkedHashMap<>(
				adminOrderDetailSalespersonLookupPort.loadSalespersonForOrderDetail(companyId, salespersonId));
	}

	private static Map<String, Object> profitToMap(OrderProfit p) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", p.getId());
		m.put("order_id", p.getOrderId());
		m.put("order_profit_status", p.getOrderProfitStatus());
		m.put("company_id", p.getCompanyId());
		m.put("total_fee", p.getTotalFee());
		m.put("pay_fee", p.getPayFee());
		m.put("profit_type", p.getProfitType());
		m.put("user_id", p.getUserId());
		m.put("dealer_id", p.getDealerId());
		m.put("distributor_id", p.getDistributorId());
		m.put("order_distributor_id", p.getOrderDistributorId());
		m.put("distributor_nid", p.getDistributorNid());
		m.put("seller_id", p.getSellerId());
		m.put("popularize_distributor_id", p.getPopularizeDistributorId());
		m.put("popularize_seller_id", p.getPopularizeSellerId());
		m.put("proprietary", p.getProprietary());
		m.put("popularize_proprietary", p.getPopularizeProprietary());
		m.put("dealers", p.getDealers());
		m.put("distributor", p.getDistributor());
		m.put("seller", p.getSeller());
		m.put("popularize_distributor", p.getPopularizeDistributor());
		m.put("popularize_seller", p.getPopularizeSeller());
		m.put("commission", p.getCommission());
		m.put("rule", p.getRule());
		m.put("plan_close_time", p.getPlanCloseTime());
		m.put("created", p.getCreated());
		m.put("updated", p.getUpdated());
		return m;
	}

	private static Map<String, Object> communityRelToMap(CommunityOrderRelActivity r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", r.getOrderId());
		m.put("company_id", r.getCompanyId());
		m.put("chief_id", r.getChiefId());
		m.put("chief_name", r.getChiefName());
		m.put("chief_avatar", r.getChiefAvatar());
		m.put("activity_id", r.getActivityId());
		m.put("activity_name", r.getActivityName());
		m.put("ziti_name", r.getZitiName());
		m.put("ziti_address", r.getZitiAddress());
		m.put("lng", r.getZitiLng());
		m.put("lat", r.getZitiLat());
		m.put("ziti_contact_user", r.getZitiContactUser());
		m.put("ziti_contact_mobile", r.getZitiContactMobile());
		m.put("activity_trade_no", r.getActivityTradeNo());
		m.put("extra_data", r.getExtraData());
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		m.put("rebate_ratio", r.getRebateRatio());
		return m;
	}

	private static int parseDatapassBlock(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return 1;
		}
		if (Boolean.TRUE.equals(attr)) {
			return 1;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return 1;
			}
		}
		String p = request.getParameter("x-datapass-block");
		if (p == null || p.trim().isEmpty() || "0".equals(p.trim()) || "false".equalsIgnoreCase(p.trim())) {
			return 0;
		}
		return 1;
	}

	private static long parseOrderIdLong(String orderId) {
		try {
			return Long.parseLong(orderId.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
