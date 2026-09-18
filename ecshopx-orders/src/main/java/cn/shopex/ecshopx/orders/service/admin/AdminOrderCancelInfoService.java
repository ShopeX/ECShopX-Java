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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminOrderCancelInfoService {

	private static final Set<String> ALLOWED_ORDER_TYPE_DISPATCH_KEYS =
			Set.of(
					"service",
					"bargain",
					"normal_bargain",
					"normal",
					"supplier_order",
					"service_groups",
					"groups",
					"normal_groups",
					"membercard",
					"normal_seckill",
					"service_seckill",
					"normal_drug",
					"normal_shopguide",
					"normal_pointsmall",
					"normal_excard",
					"normal_community",
					"normal_shopadmin",
					"normal_employee_purchase");

	private final CancelOrdersMapper cancelOrdersMapper;
	private final NormalOrdersMapper normalOrdersMapper;

	public AdminOrderCancelInfoService(
			CancelOrdersMapper cancelOrdersMapper, NormalOrdersMapper normalOrdersMapper) {
		this.cancelOrdersMapper = cancelOrdersMapper;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	public Object getOrderCancelInfo(long companyId, String orderIdRaw, String orderTypeParam) {
		String oid = requireNonBlankPathOrderId(orderIdRaw);
		String effectiveOrderType =
				(orderTypeParam == null || orderTypeParam.isEmpty() || "0".equals(orderTypeParam))
						? "normal"
						: orderTypeParam;
		String dispatchKey = effectiveOrderType.toLowerCase(Locale.ROOT);
		if (!ALLOWED_ORDER_TYPE_DISPATCH_KEYS.contains(dispatchKey)) {
			throw new ResourceException("无此类型订单！");
		}

		CancelOrders row =
				cancelOrdersMapper.selectOne(
						new LambdaQueryWrapper<CancelOrders>()
								.eq(CancelOrders::getCompanyId, companyId)
								.apply("order_id = {0}", oid)
								.eq(CancelOrders::getOrderType, effectiveOrderType)
								.last("LIMIT 1"));
		if (row == null) {
			return Collections.emptyList();
		}

		Map<String, Object> out = cancelRowToFullMap(row);
		Long uid = row.getUserId();
		NormalOrders no =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.apply("order_id = {0}", oid)
								.eq(NormalOrders::getUserId, uid)
								.last("LIMIT 1"));
		int freight = (no == null || no.getFreightFee() == null) ? 0 : no.getFreightFee();
		out.put("freight_fee", freight);
		return out;
	}

	private static String requireNonBlankPathOrderId(String orderIdRaw) {
		String oid = orderIdRaw == null ? "" : orderIdRaw.trim();
		if (!StringUtils.hasText(oid)) {
			throw new ResourceException("此订单不存在！");
		}
		return oid;
	}

	private static Map<String, Object> cancelRowToFullMap(CancelOrders row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("cancel_id", row.getCancelId());
		m.put("order_id", row.getOrderId());
		m.put("company_id", row.getCompanyId());
		m.put("supplier_id", row.getSupplierId());
		m.put("shop_id", row.getShopId());
		m.put("user_id", row.getUserId());
		m.put("distributor_id", row.getDistributorId());
		m.put("order_type", row.getOrderType());
		m.put("total_fee", row.getTotalFee());
		m.put("progress", row.getProgress());
		m.put("cancel_from", row.getCancelFrom());
		m.put("cancel_reason", row.getCancelReason());
		m.put("shop_reject_reason", row.getShopRejectReason());
		m.put("refund_status", row.getRefundStatus());
		m.put("create_time", row.getCreateTime());
		m.put("update_time", row.getUpdateTime());
		m.put("fee_type", row.getFeeType());
		m.put("fee_rate", row.getFeeRate());
		m.put("fee_symbol", row.getFeeSymbol());
		m.put("point", row.getPoint());
		m.put("pay_type", row.getPayType());
		return m;
	}
}
