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

import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.service.normal.OrderDiscountInfoSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdminOrderDetailPayloadMaps {

	/** JSON key for trade state; must match {@link #tradeToMap} output and SUCCESS filtering. */
	static final String KEY_TRADE_STATE = "trade_state";

	private static final ObjectMapper DISCOUNT_INFO_MAPPER = new ObjectMapper();

	private AdminOrderDetailPayloadMaps() {
	}

	public static Map<String, Object> itemToMap(NormalOrdersItems it) {
		Map<String, Object> m = new LinkedHashMap<>();
		put(m, "id", it.getId());
		put(m, "order_id", it.getOrderId());
		put(m, "company_id", it.getCompanyId());
		put(m, "user_id", it.getUserId());
		put(m, "goods_id", it.getGoodsId());
		put(m, "item_id", it.getItemId());
		put(m, "item_bn", it.getItemBn());
		put(m, "goods_bn", it.getGoodsBn());
		put(m, "cost_fee", it.getCostFee());
		put(m, "commission_fee", it.getCommissionFee());
		put(m, "shop_id", it.getShopId());
		put(m, "act_id", it.getActId());
		put(m, "is_total_store", it.getIsTotalStore());
		put(m, "distributor_id", it.getDistributorId());
		put(m, "item_name", it.getItemName());
		put(m, "item_unit", it.getItemUnit());
		put(m, "pic", it.getPic());
		put(m, "num", it.getNum());
		put(m, "price", it.getPrice());
		put(m, "cost_price", it.getCostPrice());
		put(m, "market_price", it.getMarketPrice());
		put(m, "total_fee", it.getTotalFee());
		put(m, "rebate", it.getRebate());
		put(m, "total_rebate", it.getTotalRebate());
		put(m, "templates_id", it.getTemplatesId());
		put(m, "item_fee", it.getItemFee());
		put(m, "member_discount", it.getMemberDiscount());
		put(m, "coupon_discount", it.getCouponDiscount());
		int discountFee = it.getDiscountFee() == null ? 0 : it.getDiscountFee();
		if (discountFee == 0) {
			int memberDisc = it.getMemberDiscount() == null ? 0 : it.getMemberDiscount();
			int couponDisc = it.getCouponDiscount() == null ? 0 : it.getCouponDiscount();
			discountFee = memberDisc + couponDisc;
		}
		put(m, "discount_fee", discountFee);
		List<Map<String, Object>> discountInfo =
				OrderDiscountInfoSupport.buildItemDiscountInfo(
						it.getDiscountInfo(),
						it.getCouponDiscountDesc(),
						it.getMemberDiscountDesc(),
						DISCOUNT_INFO_MAPPER);
		put(m, "discount_info", discountInfo);
		put(m, "add_service_info", it.getAddServiceInfo());
		put(m, "order_item_type", it.getOrderItemType());
		put(m, "coupon_discount_desc", it.getCouponDiscountDesc());
		put(m, "member_discount_desc", it.getMemberDiscountDesc());
		put(m, "is_rate", it.getIsRate());
		put(m, "auto_close_aftersales_time", it.getAutoCloseAftersalesTime());
		put(m, "create_time", it.getCreateTime());
		put(m, "update_time", it.getUpdateTime());
		put(m, "delivery_corp", it.getDeliveryCorp());
		put(m, "delivery_code", it.getDeliveryCode());
		put(m, "delivery_img", it.getDeliveryImg());
		put(m, "delivery_time", it.getDeliveryTime());
		put(m, "delivery_status", it.getDeliveryStatus());
		put(m, "aftersales_status", it.getAftersalesStatus());
		put(m, "refunded_fee", it.getRefundedFee());
		put(m, "fee_type", it.getFeeType());
		put(m, "fee_rate", it.getFeeRate());
		put(m, "fee_symbol", it.getFeeSymbol());
		put(m, "cny_fee", OrderDiscountInfoSupport.computeItemCnyFee(it.getFeeRate(), it.getTotalFee()));
		put(m, "item_point", it.getItemPoint());
		put(m, "point", it.getPoint());
		put(m, "item_spec_desc", it.getItemSpecDesc());
		put(m, "volume", it.getVolume());
		put(m, "weight", it.getWeight());
		put(m, "type", it.getType());
		if (it.getType() != null && it.getType() == 1) {
			put(m, "tax_rate", it.getTaxRate());
			put(m, "cross_border_tax", it.getCrossBorderTax());
			put(m, "origincountry_name", it.getOrigincountryName());
			put(m, "origincountry_img_url", it.getOrigincountryImgUrl());
			put(m, "taxable_fee", it.getTaxableFee());
		}
		put(m, "point_fee", it.getPointFee());
		put(m, "is_logistics", it.getIsLogistics());
		put(m, "share_points", it.getSharePoints());
		put(m, "up_share_points", it.getShareUppoints());
		put(m, "delivery_item_num", it.getDeliveryItemNum());
		put(m, "cancel_item_num", it.getCancelItemNum());
		put(m, "get_points", it.getGetPoints());
		put(m, "supplier_id", it.getSupplierId());
		put(m, "is_prescription", it.getIsPrescription());
		put(m, "is_invoice", it.getIsInvoice());
		return m;
	}

	public static Map<String, Object> tradeToMap(Trade t) {
		Map<String, Object> m = new LinkedHashMap<>();
		put(m, "trade_id", t.getTradeId());
		put(m, "order_id", t.getOrderId());
		put(m, "company_id", t.getCompanyId());
		put(m, "shop_id", t.getShopId());
		put(m, "distributor_id", t.getDistributorId());
		put(m, "dealer_id", t.getDealerId());
		put(m, "trade_source_type", t.getTradeSourceType());
		put(m, "user_id", t.getUserId());
		put(m, "mobile", t.getMobile());
		put(m, "open_id", t.getOpenId());
		put(m, "discount_info", t.getDiscountInfo());
		put(m, "mch_id", t.getMchId());
		put(m, "total_fee", t.getTotalFee());
		put(m, "discount_fee", t.getDiscountFee());
		put(m, "fee_type", t.getFeeType());
		put(m, "pay_fee", t.getPayFee());
		put(m, "trade_no", t.getTradeNo());
		put(m, KEY_TRADE_STATE, t.getTradeState());
		put(m, "pay_type", t.getPayType());
		put(m, "pay_channel", t.getPayChannel());
		put(m, "transaction_id", t.getTransactionId());
		put(m, "authorizer_appid", t.getAuthorizerAppid());
		put(m, "wxa_appid", t.getWxaAppid());
		put(m, "bank_type", t.getBankType());
		put(m, "body", t.getBody());
		put(m, "detail", t.getDetail());
		put(m, "time_start", t.getTimeStart());
		put(m, "time_expire", t.getTimeExpire());
		put(m, "div_members", t.getDivMembers());
		put(m, "refunded_fee", t.getRefundedFee());
		put(m, "adapay_fee_mode", t.getAdapayFeeMode());
		put(m, "adapay_fee", t.getAdapayFee());
		put(m, "adapay_div_status", t.getAdapayDivStatus());
		put(m, "cur_fee_type", t.getCurFeeType());
		put(m, "cur_fee_rate", t.getCurFeeRate());
		put(m, "cur_fee_symbol", t.getCurFeeSymbol());
		put(m, "cur_pay_fee", t.getCurPayFee());
		put(m, "coupon_fee", t.getCouponFee());
		put(m, "coupon_info", t.getCouponInfo());
		put(m, "inital_request", t.getInitalRequest());
		put(m, "inital_response", t.getInitalResponse());
		put(m, "merchant_id", t.getMerchantId());
		put(m, "is_settled", t.getIsSettled());
		put(m, "payment_params", t.getPaymentParams());
		put(m, "supplier_id", t.getSupplierId());
		put(m, "bspay_req_date", t.getBspayReqDate());
		put(m, "bspay_div_members", t.getBspayDivMembers());
		put(m, "bspay_div_status", t.getBspayDivStatus());
		put(m, "bspay_fee_mode", t.getBspayFeeMode());
		put(m, "bspay_fee", t.getBspayFee());
		return m;
	}

	static Map<String, Object> dadaToMap(NormalOrdersRelDada d) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (d == null) {
			return m;
		}
		put(m, "id", d.getId());
		put(m, "order_id", d.getOrderId());
		put(m, "company_id", d.getCompanyId());
		if (d.getDadaStatus() != null) {
			m.put("dada_status", String.valueOf(d.getDadaStatus()));
		}
		put(m, "dada_delivery_no", d.getDadaDeliveryNo());
		put(m, "dada_cancel_from", d.getDadaCancelFrom());
		put(m, "dm_id", d.getDmId());
		put(m, "dm_name", d.getDmName());
		put(m, "dm_mobile", d.getDmMobile());
		put(m, "pickup_time", d.getPickupTime());
		put(m, "accept_time", d.getAcceptTime());
		put(m, "delivered_time", d.getDeliveredTime());
		put(m, "create_time", d.getCreateTime());
		put(m, "update_time", d.getUpdateTime());
		m.put("delivery_length", 0);
		return m;
	}

	private static void put(Map<String, Object> m, String k, Object v) {
		m.put(k, v);
	}
}
