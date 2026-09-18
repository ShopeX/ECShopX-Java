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

import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrderAssociationAssociationDataAssembler {

	private final ObjectMapper objectMapper;

	public OrderAssociationAssociationDataAssembler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> toAssociationDataMap(OrderAssociations row) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("order_id", row.getOrderId());
		result.put("authorizer_appid", row.getAuthorizerAppid());
		result.put("wxa_appid", row.getWxaAppid());
		result.put("title", row.getTitle());
		result.put("total_fee", row.getTotalFee());
		result.put("company_id", row.getCompanyId());
		result.put("shop_id", row.getShopId());
		result.put("store_name", row.getStoreName());
		result.put("user_id", row.getUserId());
		result.put("salesman_id", row.getSalesmanId());
		result.put("promoter_user_id", row.getPromoterUserId());
		result.put("promoter_shop_id", row.getPromoterShopId());
		result.put("source_id", row.getSourceId());
		result.put("monitor_id", row.getMonitorId());
		result.put("mobile", row.getMobile());
		result.put("order_class", row.getOrderClass());
		result.put("order_type", row.getOrderType());
		result.put("order_status", row.getOrderStatus());
		result.put("create_time", row.getCreateTime());
		result.put("update_time", row.getUpdateTime());
		result.put("is_distribution", row.getIsDistribution());
		result.put("total_rebate", row.getTotalRebate());
		result.put("delivery_corp", row.getDeliveryCorp());
		result.put("delivery_code", row.getDeliveryCode());
		result.put("member_discount", row.getMemberDiscount());
		result.put("coupon_discount", row.getCouponDiscount());
		result.put("coupon_discount_desc", new ArrayList<>());
		result.put("member_discount_desc", new ArrayList<>());
		result.put("delivery_status", row.getDeliveryStatus());
		result.put("delivery_time", row.getDeliveryTime());
		result.put("cancel_status", row.getCancelStatus());
		result.put("end_time", row.getEndTime());
		result.put("fee_type", row.getFeeType());
		result.put("fee_rate", row.getFeeRate());
		result.put("fee_symbol", row.getFeeSymbol());
		if (StringUtils.hasText(row.getCouponDiscountDesc())) {
			result.put("coupon_discount_desc", decodeJsonFlexible(row.getCouponDiscountDesc()));
		}
		if (StringUtils.hasText(row.getMemberDiscountDesc())) {
			result.put("member_discount_desc", decodeJsonFlexible(row.getMemberDiscountDesc()));
		}
		return result;
	}

	private Object decodeJsonFlexible(String raw) {
		try {
			return objectMapper.readValue(raw.trim(), Object.class);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}
}
