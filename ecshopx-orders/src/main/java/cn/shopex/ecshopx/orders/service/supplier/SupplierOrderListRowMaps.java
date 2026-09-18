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

package cn.shopex.ecshopx.orders.service.supplier;

import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SupplierOrderListRowMaps {

	private SupplierOrderListRowMaps() {}

	public static Map<String, Object> toRow(SupplierOrder e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("order_id", e.getOrderId());
		m.put("title", e.getTitle());
		m.put("shop_id", e.getShopId());
		m.put("cost_fee", e.getCostFee());
		m.put("commission_fee", e.getCommissionFee());
		m.put("user_id", e.getUserId());
		m.put("act_id", e.getActId());
		m.put("mobile", e.getMobile());
		m.put("order_class", e.getOrderClass());
		m.put("freight_fee", e.getFreightFee());
		m.put("freight_type", e.getFreightType());
		m.put("item_fee", e.getItemFee());
		m.put("total_fee", e.getTotalFee());
		m.put("market_fee", e.getMarketFee());
		m.put("step_paid_fee", e.getStepPaidFee());
		m.put("total_rebate", e.getTotalRebate());
		m.put("distributor_id", e.getDistributorId());
		m.put("receipt_type", e.getReceiptType());
		m.put("ziti_code", e.getZitiCode());
		m.put("ziti_status", e.getZitiStatus());
		m.put("order_status", e.getOrderStatus());
		m.put("pay_status", e.getPayStatus());
		m.put("order_source", e.getOrderSource());
		m.put("order_type", e.getOrderType());
		m.put("is_distribution", e.getIsDistribution());
		m.put("source_id", e.getSourceId());
		m.put("delivery_corp", e.getDeliveryCorp());
		m.put("delivery_corp_source", e.getDeliveryCorpSource());
		m.put("delivery_code", e.getDeliveryCode());
		m.put("delivery_img", e.getDeliveryImg());
		m.put("delivery_time", e.getDeliveryTime());
		m.put("end_time", e.getEndTime());
		m.put("delivery_status", e.getDeliveryStatus());
		m.put("cancel_status", e.getCancelStatus());
		m.put("receiver_name", e.getReceiverName());
		m.put("receiver_mobile", e.getReceiverMobile());
		m.put("receiver_zip", e.getReceiverZip());
		m.put("receiver_state", e.getReceiverState());
		m.put("receiver_city", e.getReceiverCity());
		m.put("receiver_district", e.getReceiverDistrict());
		m.put("receiver_address", e.getReceiverAddress());
		m.put("member_discount", e.getMemberDiscount());
		m.put("coupon_discount", e.getCouponDiscount());
		m.put("discount_fee", e.getDiscountFee());
		m.put("discount_info", e.getDiscountInfo());
		m.put("coupon_discount_desc", e.getCouponDiscountDesc());
		m.put("member_discount_desc", e.getMemberDiscountDesc());
		m.put("fee_type", e.getFeeType());
		m.put("fee_rate", e.getFeeRate());
		m.put("fee_symbol", e.getFeeSymbol());
		m.put("item_point", e.getItemPoint());
		m.put("point", e.getPoint());
		m.put("pay_type", e.getPayType());
		m.put("pay_channel", e.getPayChannel());
		m.put("remark", e.getRemark());
		m.put("invoice", e.getInvoice());
		m.put("invoice_number", e.getInvoiceNumber());
		m.put("is_invoiced", e.getIsInvoiced());
		m.put("send_point", e.getSendPoint());
		m.put("type", e.getType());
		m.put("point_fee", e.getPointFee());
		m.put("point_use", e.getPointUse());
		m.put("pack", e.getPack());
		m.put("operator_id", e.getOperatorId());
		m.put("source_from", e.getSourceFrom());
		m.put("supplier_id", e.getSupplierId());
		m.put("is_settled", e.getIsSettled());
		m.put("create_time", e.getCreateTime());
		m.put("update_time", e.getUpdateTime());
		return m;
	}
}
