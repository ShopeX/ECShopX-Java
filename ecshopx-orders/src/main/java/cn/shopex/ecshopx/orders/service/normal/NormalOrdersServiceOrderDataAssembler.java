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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.orders.config.OrderAppPayTypeDescHolder;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrdersServiceOrderDataAssembler {

	private static final DateTimeFormatter END_DATE_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OrderAppPayTypeDescHolder appPayTypeDescHolder;
	private final ObjectMapper objectMapper;

	public NormalOrdersServiceOrderDataAssembler(
			OrderAppPayTypeDescHolder appPayTypeDescHolder, ObjectMapper objectMapper) {
		this.appPayTypeDescHolder = appPayTypeDescHolder;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> toServiceOrderData(NormalOrders order) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", order.getOrderId());
		m.put("title", order.getTitle());
		m.put("company_id", order.getCompanyId());
		m.put("user_id", order.getUserId());
		m.put("act_id", order.getActId());
		m.put("mobile", order.getMobile());
		m.put("commission_fee", order.getCommissionFee());
		m.put("freight_fee", Objects.requireNonNullElse(order.getFreightFee(), 0));
		m.put("freight_point", order.getFreightPoint());
		m.put("freight_point_fee", order.getFreightPointFee());
		m.put("freight_type", order.getFreightType());
		m.put("item_fee", order.getItemFee());
		m.put("item_point", order.getItemPoint());
		m.put("cost_fee", order.getCostFee());
		m.put("total_fee", order.getTotalFee());
		m.put("market_fee", order.getMarketFee());
		m.put("step_paid_fee", order.getStepPaidFee());
		m.put("total_rebate", order.getTotalRebate());
		m.put("distributor_id", order.getDistributorId());
		m.put("order_holder", order.getOrderHolder());
		m.put("receipt_type", order.getReceiptType());
		m.put("ziti_code", order.getZitiCode());
		m.put("shop_id", order.getShopId());
		m.put("ziti_status", order.getZitiStatus());
		m.put("order_status", order.getOrderStatus());
		m.put("order_source", order.getOrderSource());
		m.put("order_type", order.getOrderType());
		m.put("order_class", order.getOrderClass());
		m.put("auto_cancel_time", order.getAutoCancelTime());
		long cancelAt = parseSecondsFlexible(order.getAutoCancelTime());
		m.put("auto_cancel_seconds", cancelAt - System.currentTimeMillis() / 1000L);
		m.put("auto_finish_time", order.getAutoFinishTime());
		m.put("is_distribution", order.getIsDistribution());
		m.put("source_id", order.getSourceId());
		m.put("monitor_id", order.getMonitorId());
		m.put("salesman_id", order.getSalesmanId());
		m.put("delivery_corp", order.getDeliveryCorp());
		m.put("delivery_corp_source", order.getDeliveryCorpSource());
		m.put("delivery_code", order.getDeliveryCode());
		m.put("delivery_img", order.getDeliveryImg());
		m.put("delivery_status", order.getDeliveryStatus());
		m.put("cancel_status", order.getCancelStatus());
		m.put("delivery_time", order.getDeliveryTime());
		m.put("end_time", order.getEndTime());
		m.put("end_date", formatEndDate(order.getEndTime()));
		m.put("receiver_name", order.getReceiverName());
		m.put("receiver_mobile", order.getReceiverMobile());
		m.put("receiver_zip", order.getReceiverZip());
		m.put("receiver_state", order.getReceiverState());
		m.put("receiver_city", order.getReceiverCity());
		m.put("receiver_district", order.getReceiverDistrict());
		m.put("receiver_address", order.getReceiverAddress());
		m.put("member_discount", Objects.requireNonNullElse(order.getMemberDiscount(), 0));
		m.put("coupon_discount", Objects.requireNonNullElse(order.getCouponDiscount(), 0));
		Integer discountFee = order.getDiscountFee();
		int md = Objects.requireNonNullElse(order.getMemberDiscount(), 0);
		int cd = Objects.requireNonNullElse(order.getCouponDiscount(), 0);
		if (discountFee == null || discountFee == 0) {
			m.put("discount_fee", md + cd);
		} else {
			m.put("discount_fee", discountFee);
		}
		m.put("create_time", order.getCreateTime());
		m.put("update_time", order.getUpdateTime());
		m.put("fee_type", order.getFeeType());
		m.put("fee_rate", order.getFeeRate());
		m.put("fee_symbol", order.getFeeSymbol());
		m.put("cny_fee", computeCnyFee(order));
		m.put("point", order.getPoint());
		m.put("pay_type", order.getPayType());
		m.put("pay_channel", order.getPayChannel());
		m.put("remark", order.getRemark());
		m.put("distributor_remark", order.getDistributorRemark());
		m.put("third_params", order.getThirdParams());
		String inv = order.getInvoice();
		m.put("invoice", (inv != null && inv.isEmpty()) ? null : inv);
		m.put("send_point", order.getSendPoint());
		m.put("is_rate", order.getIsRate());
		m.put("is_invoiced", order.getIsInvoiced());
		m.put("invoice_number", order.getInvoiceNumber());
		m.put("audit_status", order.getAuditStatus());
		m.put("audit_msg", order.getAuditMsg());
		m.put("point_fee", order.getPointFee());
		m.put("point_use", order.getPointUse());
		m.put("uppoint_use", order.getUppointUse());
		m.put("point_up_use", order.getPointUpUse());
		m.put("pay_status", order.getPayStatus());
		m.put("get_points", order.getGetPoints());
		m.put("bonus_points", order.getBonusPoints());
		m.put("get_point_type", order.getGetPointType());
		m.put("pack", order.getPack());
		m.put("is_shopscreen", order.getIsShopscreen());
		m.put("is_logistics", order.getIsLogistics());
		m.put("is_profitsharing", order.getIsProfitsharing());
		m.put("profitsharing_status", order.getProfitsharingStatus());
		m.put("order_auto_close_aftersales_time", order.getOrderAutoCloseAftersalesTime());
		m.put("profitsharing_rate", order.getProfitsharingRate());
		m.put("bind_auth_code", order.getBindAuthCode());
		m.put("extra_points", order.getExtraPoints());
		m.put("sale_salesman_distributor_id", order.getSaleSalesmanDistributorId());
		m.put("bind_salesman_id", order.getBindSalesmanId());
		m.put("bind_salesman_distributor_id", order.getBindSalesmanDistributorId());
		m.put("chat_id", order.getChatId());
		m.put("is_consumption", order.getIsConsumption());
		m.put("app_pay_type", order.getAppPayType());
		if (appPayTypeDescHolder.hasValidMap()) {
			String desc = appPayTypeDescHolder.descForAppPayTypeOrNull(order.getAppPayType());
			if (desc != null) {
				m.put("app_pay_type_desc", desc);
			} else {
				m.put("app_pay_type_desc", "{}");
			}
		} else {
			m.put("app_pay_type_desc", "{}");
		}
		m.put("merchant_id", order.getMerchantId());
		m.put("subdistrict_parent_id", order.getSubdistrictParentId());
		m.put("subdistrict_id", order.getSubdistrictId());
		m.put("building_number", order.getBuildingNumber());
		m.put("house_number", order.getHouseNumber());
		m.put("operator_id", order.getOperatorId());
		m.put("left_aftersales_num", order.getLeftAftersalesNum());
		m.put("source_from", order.getSourceFrom());
		m.put("self_delivery_status", order.getSelfDeliveryStatus());
		m.put("self_delivery_operator_id", order.getSelfDeliveryOperatorId());
		m.put("self_delivery_fee", order.getSelfDeliveryFee());
		m.put("self_delivery_time", order.getSelfDeliveryTime());
		m.put("supplier_id", order.getSupplierId());
		m.put("self_delivery_end_time", order.getSelfDeliveryEndTime());
		m.put("offline_payment_status", order.getOfflinePaymentStatus());
		m.put("prescription_status", order.getPrescriptionStatus());
		m.put("invoice_status", order.getInvoiceStatus());
		m.put("dm_point_preid", order.getDmPointPreid());
		m.put("type", order.getType());
		m.put("taxable_fee", order.getTaxableFee());
		m.put("identity_id", order.getIdentityId());
		m.put("identity_name", order.getIdentityName());
		m.put("total_tax", order.getTotalTax());
		m.put("coupon_discount_desc", Objects.requireNonNullElse(order.getCouponDiscountDesc(), ""));
		m.put("member_discount_desc", Objects.requireNonNullElse(order.getMemberDiscountDesc(), ""));
		boolean onlineDefault = order.getIsOnlineOrder() == null || Boolean.TRUE.equals(order.getIsOnlineOrder());
		m.put("is_online_order", onlineDefault ? "1" : "0");
		m.put("original_order_id", order.getOriginalOrderId());
		m.put("discount_info", buildDiscountInfo(order));
		return m;
	}

	private static long parseSecondsFlexible(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		String t = raw.trim();
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String formatEndDate(Long endTime) {
		if (endTime == null || endTime <= 0) {
			return "";
		}
		long seconds = endTime;
		if (endTime > 9_999_999_999L) {
			seconds = endTime / 1000L;
		}
		try {
			return END_DATE_FMT.format(Instant.ofEpochSecond(seconds));
		} catch (Exception e) {
			return "";
		}
	}

	private static int computeCnyFee(NormalOrders order) {
		double feeRate = 0.0;
		if (order.getFeeRate() != null) {
			feeRate = order.getFeeRate().doubleValue();
		}
		long totalFee = 0L;
		if (StringUtils.hasText(order.getTotalFee())) {
			try {
				totalFee = Long.parseLong(order.getTotalFee().trim());
			} catch (NumberFormatException ignored) {
				totalFee = 0L;
			}
		}
		BigDecimal fr = BigDecimal.valueOf(feeRate).setScale(4, RoundingMode.HALF_UP);
		return fr.multiply(BigDecimal.valueOf(totalFee)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private List<Map<String, Object>> buildDiscountInfo(NormalOrders order) {
		List<Map<String, Object>> list = new ArrayList<>();
		String di = order.getDiscountInfo();
		if (StringUtils.hasText(di)) {
			list.addAll(OrderDiscountInfoSupport.parseDiscountInfoJson(di, objectMapper));
			return list;
		}
		if (StringUtils.hasText(order.getCouponDiscountDesc())) {
			try {
				Map<String, Object> cm = objectMapper.readValue(order.getCouponDiscountDesc(), new TypeReference<>() {});
				if (cm != null) {
					cm.put("type", "coupon_discount");
					list.add(cm);
				}
			} catch (Exception ignored) {
				// skip
			}
		}
		if (StringUtils.hasText(order.getMemberDiscountDesc())) {
			try {
				Map<String, Object> mm = objectMapper.readValue(order.getMemberDiscountDesc(), new TypeReference<>() {});
				if (mm != null) {
					mm.put("type", "member_discount");
					list.add(mm);
				}
			} catch (Exception ignored) {
				// skip
			}
		}
		return list;
	}
}
