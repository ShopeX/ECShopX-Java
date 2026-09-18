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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.front.WxappOrderDetailMembercardBundlePort;
import cn.shopex.ecshopx.common.order.front.WxappOrderDetailSupplierBundlePort;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminEntityOrderDetailTypePolicy;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailPhpParityApplier;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOrderDetailService {

	private static final Set<String> PROMOTION_DISCOUNT_TYPES =
			Set.of("full_minus", "full_discount", "member_tag_targeted_promotion");

	private final WxappOrderDetailDepositSupport wxappOrderDetailDepositSupport;
	private final WxappOrderDetailTradePrefixSupport wxappOrderDetailTradePrefixSupport;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final WxappOrderDetailMembercardBundlePort wxappOrderDetailMembercardBundlePort;
	private final WxappOrderDetailSupplierBundlePort wxappOrderDetailSupplierBundlePort;
	private final WxappOrderDetailDeliveryInfoService wxappOrderDetailDeliveryInfoService;
	private final PickupcodeSettingRedisService pickupcodeSettingRedisService;
	private final WxappOrderDetailSalespromoterService wxappOrderDetailSalespromoterService;
	private final WxappOrderDetailPrescriptionRandomGuard wxappOrderDetailPrescriptionRandomGuard;
	private final WxappOrderDetailOrderRecombineSupport wxappOrderDetailOrderRecombineSupport;
	private final WxappOrderDetailPostProcessService wxappOrderDetailPostProcessService;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy;
	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;
	private final AdminOrderDetailPhpParityApplier adminOrderDetailPhpParityApplier;

	public WxappOrderDetailService(
			WxappOrderDetailDepositSupport wxappOrderDetailDepositSupport,
			WxappOrderDetailTradePrefixSupport wxappOrderDetailTradePrefixSupport,
			OrderAssociationsMapper orderAssociationsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			WxappOrderDetailMembercardBundlePort wxappOrderDetailMembercardBundlePort,
			WxappOrderDetailSupplierBundlePort wxappOrderDetailSupplierBundlePort,
			WxappOrderDetailDeliveryInfoService wxappOrderDetailDeliveryInfoService,
			PickupcodeSettingRedisService pickupcodeSettingRedisService,
			WxappOrderDetailSalespromoterService wxappOrderDetailSalespromoterService,
			WxappOrderDetailPrescriptionRandomGuard wxappOrderDetailPrescriptionRandomGuard,
			WxappOrderDetailOrderRecombineSupport wxappOrderDetailOrderRecombineSupport,
			WxappOrderDetailPostProcessService wxappOrderDetailPostProcessService,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort,
			AdminOrderDetailPhpParityApplier adminOrderDetailPhpParityApplier) {
		this.wxappOrderDetailDepositSupport = wxappOrderDetailDepositSupport;
		this.wxappOrderDetailTradePrefixSupport = wxappOrderDetailTradePrefixSupport;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.wxappOrderDetailMembercardBundlePort = wxappOrderDetailMembercardBundlePort;
		this.wxappOrderDetailSupplierBundlePort = wxappOrderDetailSupplierBundlePort;
		this.wxappOrderDetailDeliveryInfoService = wxappOrderDetailDeliveryInfoService;
		this.pickupcodeSettingRedisService = pickupcodeSettingRedisService;
		this.wxappOrderDetailSalespromoterService = wxappOrderDetailSalespromoterService;
		this.wxappOrderDetailPrescriptionRandomGuard = wxappOrderDetailPrescriptionRandomGuard;
		this.wxappOrderDetailOrderRecombineSupport = wxappOrderDetailOrderRecombineSupport;
		this.wxappOrderDetailPostProcessService = wxappOrderDetailPostProcessService;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.adminEntityOrderDetailTypePolicy = adminEntityOrderDetailTypePolicy;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
		this.adminOrderDetailPhpParityApplier = adminOrderDetailPhpParityApplier;
	}

	@SuppressWarnings("unchecked")
	public Object getOrderDetail(
			HttpServletRequest request,
			Map<String, Object> auth,
			String orderIdRaw,
			String invoiceListRaw,
			String from,
			String promoterUserId,
			String isSalesmanPage,
			List<String> selfDeliveryOperatorIdParam,
			String prescriptionOrderRandom) {
		final String invoiceListT = invoiceListRaw == null ? "" : invoiceListRaw.trim();
		final boolean invoiceListTruthy = !invoiceListT.isEmpty() && !"0".equalsIgnoreCase(invoiceListT);

		if (orderIdRaw == null || orderIdRaw.isBlank() || "0".equals(orderIdRaw.trim())) {
			throw new BadRequestException("订单号必填");
		}
		String orderId = orderIdRaw.trim();
		String prefix = firstTwoUnicodeUpper(orderId);
		if ("CZ".equals(prefix)) {
			return wxappOrderDetailDepositSupport.buildResult(orderId);
		}
		if ("TD".equals(prefix)) {
			String canonicalOrderId = wxappOrderDetailTradePrefixSupport.resolveCanonicalOrderId(orderId);
			if (canonicalOrderId == null) {
				return Collections.emptyList();
			}
			orderId = canonicalOrderId;
		}

		long companyId = requireCompanyId(auth);
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderId);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (assoc == null) {
			return Collections.emptyList();
		}

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		String detailFrom = from == null || from.isBlank() ? "front_list" : from.trim();
		Map<String, Object> result;
		if (adminEntityOrderDetailTypePolicy.supportsNormalPipeline(effective)) {
			result = adminNormalOrderDetailService.buildOrderBundle(companyId, orderId, true, detailFrom);
		} else if ("membercard".equals(effective)) {
			result = wxappOrderDetailMembercardBundlePort.buildMembercardOrderDetailBundle(companyId, orderId, true);
		} else if ("supplier_order".equals(effective)) {
			result = wxappOrderDetailSupplierBundlePort.buildSupplierOrderDetailBundle(companyId, orderId, true, detailFrom);
		} else {
			throw new ResourceException("无此类型订单！");
		}

		Map<String, Object> orderInfo = (Map<String, Object>) result.get("orderInfo");
		if (orderInfo == null || orderInfo.isEmpty()) {
			return Collections.emptyList();
		}

		wxappOrderDetailDeliveryInfoService.appendDeliveryInfo(companyId, orderIdNum, result);

		long currentUserId = longVal(auth.get("user_id"));
		List<String> selfDeliveryParams = selfDeliveryOperatorIdParam == null ? List.of() : selfDeliveryOperatorIdParam;
		List<String> orderSelfDeliveryIds = selfDeliveryOperatorIdsFromOrderInfo(orderInfo);
		boolean selfDeliveryMatch =
				!selfDeliveryParams.isEmpty()
						&& selfDeliveryParams.stream()
								.map(WxappOrderDetailService::normalizeId)
								.filter(s -> !s.isEmpty())
								.anyMatch(orderSelfDeliveryIds::contains);
		boolean allowed =
				(currentUserId > 0L && currentUserId == longVal(orderInfo.get("user_id"))) || selfDeliveryMatch;
		if (allowed) {
			applyPickupcodeStatus(companyId, orderInfo);
		} else if (queryTruthy(promoterUserId)
				&& queryTruthy(isSalesmanPage)
				&& branchBSalesMatch(assocToRow(assoc), promoterUserId)) {
			orderInfo.put("order_source", "salesperson");
		} else if (currentUserId != longVal(orderInfo.get("user_id")) && queryTruthy(prescriptionOrderRandom)) {
			wxappOrderDetailPrescriptionRandomGuard.logIfMiss(orderId, prescriptionOrderRandom);
		} else {
			return Collections.emptyList();
		}

		applyPickupcodeStatus(companyId, orderInfo);
		wxappOrderDetailSalespromoterService.fillPromoterAndSalesperson(companyId, assocToRow(assoc), result);
		wxappOrderDetailOrderRecombineSupport.applyIfPresent(assoc.getOrderType(), result);

		String v = request.getHeader("x-datapass-block");
		boolean block = v != null && !v.isBlank() && !"0".equals(v.trim());
		if (block && orderInfo.get("receiver_mobile") != null) {
			orderInfo.put("receiver_name", DataMasking.maskTruename(str(orderInfo.get("receiver_name"))));
			orderInfo.put("receiver_mobile", DataMasking.maskMobile(str(orderInfo.get("receiver_mobile"))));
			orderInfo.put("receiver_address", DataMasking.maskAddress(str(orderInfo.get("receiver_address"))));
		}

		applyPromotionDiscount(orderInfo);

		if (!orderInfo.containsKey("items") || orderInfo.get("items") == null) {
			return result;
		}

		recalcItemAndOrderFees(orderInfo);

		wxappOrderDetailPostProcessService.apply(invoiceListTruthy, result, effective, companyId, orderIdNum);

		Map<String, Object> settingsAfter = orderValiditySettingRedisReadService.getOrderSetting(companyId);
		result.put("offline_aftersales_is_open", settingsAfter == null ? null : settingsAfter.get("offline_aftersales"));
		orderInfo.remove("offline_aftersales_is_open");

		if (adminEntityOrderDetailTypePolicy.supportsNormalPipeline(effective)) {
			attachDistributorWxappDetail(companyId, orderInfo, result);
			normalizeEmptyCancelDataArray(result);
			adminOrderDetailPhpParityApplier.applyWxappDetail(companyId, result);
		}
		return result;
	}

	private void applyPickupcodeStatus(long companyId, Map<String, Object> orderInfo) {
		Map<String, Object> setting = pickupcodeSettingRedisService.handle(companyId, Map.of());
		Object status = setting.get("pickupcode_status");
		orderInfo.put("pickupcode_status", status instanceof Boolean b ? b : Boolean.FALSE);
	}

	private void attachDistributorWxappDetail(long companyId, Map<String, Object> orderInfo, Map<String, Object> result) {
		long distId = longVal(orderInfo.get("distributor_id"));
		Map<String, Object> distributorInfo = new LinkedHashMap<>();
		if (distId > 0L) {
			distributorInfo.putAll(
					adminOrderDetailDistributionSupportPort.getDistributorInfoSimple(companyId, String.valueOf(distId)));
			Map<String, Object> formatted =
					adminOrderDetailDistributionSupportPort.getDistributorInfoFormatted(companyId, distId);
			for (String key : List.of("store_address", "store_name", "phone", "selfDeliveryRule", "rate")) {
				if (formatted.containsKey(key)) {
					distributorInfo.put(key, formatted.get(key));
				}
			}
		} else {
			distributorInfo.putAll(adminOrderDetailDistributionSupportPort.getDistributorSelfSimpleInfo(companyId));
		}
		Map<String, Object> platform = adminOrderDetailDistributionSupportPort.readOrderValidityPlatformSetting(companyId);
		if (platform != null && intVal(platform.get("is_refund_freight")) == 0) {
			distributorInfo.put("is_refund_freight", 0);
		}
		orderInfo.put("distributor_name", str(distributorInfo.get("name")));
		orderInfo.put("distributor_info", distributorInfo);
		result.put("distributor", distributorInfo);
	}

	private static void normalizeEmptyCancelDataArray(Map<String, Object> result) {
		Object cd = result.get("cancelData");
		if (cd instanceof Map<?, ?> m && m.isEmpty()) {
			result.put("cancelData", new ArrayList<Object>());
		}
	}

	private static void applyPromotionDiscount(Map<String, Object> orderInfo) {
		orderInfo.put("promotion_discount", 0);
		Object di = orderInfo.get("discount_info");
		List<Map<String, Object>> list = asDiscountList(di);
		int sum = 0;
		for (Map<String, Object> discountInfo : list) {
			Object type = discountInfo.get("type");
			if (!PROMOTION_DISCOUNT_TYPES.contains(String.valueOf(type))) {
				continue;
			}
			sum += intVal(discountInfo.get("discount_fee"));
		}
		orderInfo.put("promotion_discount", sum);
	}

	private static List<Map<String, Object>> asDiscountList(Object di) {
		if (di instanceof List<?> l) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object el : l) {
				if (el instanceof Map<?, ?> m) {
					out.add((Map<String, Object>) m);
				}
			}
			return out;
		}
		return List.of();
	}

	private static void recalcItemAndOrderFees(Map<String, Object> orderInfo) {
		Object itemsObj = orderInfo.get("items");
		if (!(itemsObj instanceof List<?> rawList)) {
			return;
		}
		for (Object el : rawList) {
			if (!(el instanceof Map<?, ?>)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> item = (Map<String, Object>) el;
			int totalFee = intVal(item.get("total_fee"));
			int pointFee = intVal(item.get("point_fee"));
			int couponDisc = intVal(item.get("coupon_discount"));
			int promDisc = intVal(item.get("promotion_discount"));
			item.put("item_fee_new", totalFee + pointFee + couponDisc + promDisc);
			int unit = pickMarketOrPrice(item);
			int num = intVal(item.get("num"));
			item.put("market_fee", unit * num);
		}
		int orderTotal = intVal(orderInfo.get("total_fee"));
		int freight = intVal(orderInfo.get("freight_fee"));
		int orderPoint = intVal(orderInfo.get("point_fee"));
		int orderCoupon = intVal(orderInfo.get("coupon_discount"));
		int orderPromo = intVal(orderInfo.get("promotion_discount"));
		orderInfo.put("item_fee_new", orderTotal - freight + orderPoint + orderCoupon + orderPromo);
	}

	private static int pickMarketOrPrice(Map<String, Object> item) {
		Object mp = item.get("market_price");
		if (mp != null) {
			if (mp instanceof Boolean) {
				// fall through to price
			} else if (mp instanceof Number n && n.doubleValue() != 0.0d) {
				return n.intValue();
			} else if (mp instanceof String s && StringUtils.hasText(s.trim()) && !"0".equals(s.trim())) {
				try {
					return Integer.parseInt(s.trim());
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return intVal(item.get("price"));
	}

	private static boolean branchBSalesMatch(Map<String, Object> orderAssoc, String promoterUserId) {
		String q = promoterUserId == null ? "" : promoterUserId.trim();
		String p = normalizeId(orderAssoc.get("promoter_user_id"));
		String s = normalizeId(orderAssoc.get("salesman_id"));
		return p.equals(q) || s.equals(q);
	}

	private static Map<String, Object> assocToRow(OrderAssociations a) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("promoter_user_id", a.getPromoterUserId());
		m.put("salesman_id", a.getSalesmanId());
		return m;
	}

	private static boolean queryTruthy(String s) {
		return s != null && !s.isBlank() && !"0".equals(s.trim());
	}

	private static String normalizeId(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long requireCompanyId(Map<String, Object> auth) {
		Object c = auth.get("company_id");
		if (c == null) {
			throw new BadRequestException("缺少 company_id");
		}
		long companyId = longVal(c);
		if (companyId <= 0L) {
			throw new BadRequestException("缺少 company_id");
		}
		return companyId;
	}

	private static List<String> selfDeliveryOperatorIdsFromOrderInfo(Map<String, Object> orderInfo) {
		List<String> fromIds = new ArrayList<>();
		Object raw = orderInfo.get("self_delivery_operator_ids");
		if (raw instanceof List<?> list) {
			for (Object e : list) {
				String s = normalizeId(e);
				if (!s.isEmpty()) {
					fromIds.add(s);
				}
			}
		} else if (raw instanceof int[] ia) {
			for (int v : ia) {
				String s = normalizeId(v);
				if (!s.isEmpty()) {
					fromIds.add(s);
				}
			}
		} else if (raw instanceof long[] la) {
			for (long v : la) {
				String s = normalizeId(v);
				if (!s.isEmpty()) {
					fromIds.add(s);
				}
			}
		} else if (raw instanceof Object[] arr) {
			for (Object e : arr) {
				String s = normalizeId(e);
				if (!s.isEmpty()) {
					fromIds.add(s);
				}
			}
		}
		if (fromIds.isEmpty()) {
			String one = normalizeId(orderInfo.get("self_delivery_operator_id"));
			if (!one.isEmpty()) {
				fromIds = new ArrayList<>(List.of(one));
			}
		}
		return fromIds.isEmpty() ? List.of() : List.copyOf(fromIds);
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

	private static String firstTwoUnicodeUpper(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		int cps = s.codePointCount(0, s.length());
		int n = Math.min(2, cps);
		int end = s.offsetByCodePoints(0, n);
		return s.substring(0, end).toUpperCase(Locale.ROOT);
	}
}
