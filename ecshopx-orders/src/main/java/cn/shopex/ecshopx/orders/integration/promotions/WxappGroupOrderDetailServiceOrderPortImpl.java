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

package cn.shopex.ecshopx.orders.integration.promotions;

import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.port.WxappGroupOrderDetailServiceOrderPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGroupOrderDetailServiceOrderPortImpl implements WxappGroupOrderDetailServiceOrderPort {

	private final ServiceOrdersMapper serviceOrdersMapper;

	public WxappGroupOrderDetailServiceOrderPortImpl(ServiceOrdersMapper serviceOrdersMapper) {
		this.serviceOrdersMapper = serviceOrdersMapper;
	}

	@Override
	public Map<String, Object> getServiceOrder(long companyId, String orderId) {
		if (!StringUtils.hasText(orderId)) {
			return null;
		}
		String trimmed = orderId.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		long parsed;
		try {
			parsed = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return null;
		}
		if (parsed <= 0L) {
			return null;
		}
		ServiceOrders row =
				serviceOrdersMapper.selectOne(
						new LambdaQueryWrapper<ServiceOrders>()
								.eq(ServiceOrders::getCompanyId, companyId)
								.eq(ServiceOrders::getOrderId, parsed)
								.last("LIMIT 1"));
		if (row == null) {
			return null;
		}
		return serviceOrderRow(row);
	}

	private static LinkedHashMap<String, Object> serviceOrderRow(ServiceOrders o) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", o.getOrderId());
		m.put("title", o.getTitle());
		m.put("company_id", o.getCompanyId());
		m.put("shop_id", o.getShopId());
		m.put("store_name", o.getStoreName());
		m.put("user_id", o.getUserId());
		m.put("consume_type", o.getConsumeType());
		m.put("item_id", o.getItemId());
		m.put("item_brief", o.getItemBrief());
		m.put("item_pics", o.getItemPics());
		m.put("source_id", o.getSourceId());
		m.put("bargain_id", o.getBargainId());
		m.put("monitor_id", o.getMonitorId());
		m.put("salesman_id", o.getSalesmanId());
		m.put("item_num", o.getItemNum());
		m.put("mobile", o.getMobile());
		m.put("total_fee", o.getTotalFee());
		m.put("step_paid_fee", o.getStepPaidFee());
		m.put("order_class", o.getOrderClass());
		m.put("order_status", o.getOrderStatus());
		m.put("order_source", o.getOrderSource());
		m.put("operator_desc", o.getOperatorDesc());
		m.put("order_type", o.getOrderType());
		m.put("create_time", o.getCreateTime());
		m.put("update_time", o.getUpdateTime());
		m.put("auto_cancel_time", o.getAutoCancelTime());
		m.put("date_type", o.getDateType());
		m.put("begin_date", o.getBeginDate());
		m.put("end_date", o.getEndDate());
		m.put("fixed_term", o.getFixedTerm());
		m.put("cost_fee", o.getCostFee());
		m.put("item_fee", o.getItemFee());
		m.put("member_discount", o.getMemberDiscount());
		m.put("coupon_discount", o.getCouponDiscount());
		m.put("coupon_discount_desc", o.getCouponDiscountDesc());
		m.put("member_discount_desc", o.getMemberDiscountDesc());
		m.put("fee_type", o.getFeeType());
		m.put("fee_rate", o.getFeeRate());
		m.put("fee_symbol", o.getFeeSymbol());
		return m;
	}
}
