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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderNormalOrderHeaderReadPortImpl implements OrderNormalOrderHeaderReadPort {

	private final NormalOrdersMapper normalOrdersMapper;

	public OrderNormalOrderHeaderReadPortImpl(NormalOrdersMapper normalOrdersMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
	}

	@Override
	public Optional<Map<String, Object>> getHeader(long companyId, long orderId) {
		NormalOrders o =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (o == null) {
			return Optional.empty();
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", o.getCompanyId());
		m.put("order_id", o.getOrderId());
		m.put("dm_point_preid", o.getDmPointPreid() != null ? o.getDmPointPreid() : "");
		m.put("point_fee", o.getPointFee() == null ? 0 : o.getPointFee());
		m.put("point_use", o.getPointUse() == null ? 0 : o.getPointUse());
		m.put("total_fee", o.getTotalFee() != null ? o.getTotalFee() : "0");
		m.put("item_fee", o.getItemFee() != null ? o.getItemFee() : "0");
		m.put("salesman_id", o.getSalesmanId() == null ? 0L : o.getSalesmanId());
		m.put(
				"sale_salesman_distributor_id",
				o.getSaleSalesmanDistributorId() == null ? 0L : o.getSaleSalesmanDistributorId());
		m.put("receiver_name", o.getReceiverName() != null ? o.getReceiverName() : "");
		m.put("receiver_mobile", o.getReceiverMobile() != null ? o.getReceiverMobile() : "");
		m.put("receiver_zip", o.getReceiverZip() != null ? o.getReceiverZip() : "");
		m.put("receiver_address", o.getReceiverAddress() != null ? o.getReceiverAddress() : "");
		m.put("create_time", o.getCreateTime() == null ? 0 : o.getCreateTime());
		m.put("remark", o.getRemark() != null ? o.getRemark() : "");
		m.put("discount_info", o.getDiscountInfo() != null ? o.getDiscountInfo() : "");
		m.put("shop_id", o.getShopId() == null ? 0L : o.getShopId());
		m.put("distributor_id", o.getDistributorId() == null ? 0L : o.getDistributorId());
		m.put("merchant_id", o.getMerchantId() == null ? 0L : o.getMerchantId());
		m.put("pay_type", o.getPayType());
		m.put("freight_type", o.getFreightType());
		m.put("freight_fee", o.getFreightFee() == null ? 0 : o.getFreightFee());
		m.put("freight_point", o.getFreightPoint() == null ? 0 : o.getFreightPoint());
		m.put("user_id", o.getUserId());
		m.put("mobile", o.getMobile());
		m.put("order_class", o.getOrderClass());
		m.put("receipt_type", o.getReceiptType());
		m.put("order_status", o.getOrderStatus());
		m.put("delivery_status", o.getDeliveryStatus());
		m.put("act_id", o.getActId() == null ? 0L : o.getActId());
		m.put(
				"order_auto_close_aftersales_time",
				o.getOrderAutoCloseAftersalesTime() == null ? 0 : o.getOrderAutoCloseAftersalesTime());
		m.put("left_aftersales_num", o.getLeftAftersalesNum() == null ? 0 : o.getLeftAftersalesNum());
		return Optional.of(m);
	}
}
