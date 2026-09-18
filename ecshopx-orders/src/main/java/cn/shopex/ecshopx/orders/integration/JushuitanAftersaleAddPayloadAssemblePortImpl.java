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

import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.common.port.systemlink.JushuitanAftersaleAddPayloadAssemblePort;
import cn.shopex.ecshopx.orders.domain.OrdersRelJushuitan;
import cn.shopex.ecshopx.orders.mapper.OrdersRelJushuitanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JushuitanAftersaleAddPayloadAssemblePortImpl implements JushuitanAftersaleAddPayloadAssemblePort {

	private static final int DETAIL_LIMIT = 200;

	private final OrdersRelJushuitanMapper ordersRelJushuitanMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;

	public JushuitanAftersaleAddPayloadAssemblePortImpl(
			OrdersRelJushuitanMapper ordersRelJushuitanMapper, AftersalesDetailMapper aftersalesDetailMapper) {
		this.ordersRelJushuitanMapper = ordersRelJushuitanMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
	}

	@Override
	public Optional<Map<String, Object>> assemble(long companyId, Map<String, Object> aftersalesRow) {
		if (aftersalesRow == null || aftersalesRow.isEmpty()) {
			return Optional.empty();
		}
		long orderId = longOrZero(aftersalesRow.get("order_id"));
		long aftersalesBn = longOrZero(aftersalesRow.get("aftersales_bn"));
		if (orderId <= 0 || aftersalesBn <= 0) {
			return Optional.empty();
		}
		OrdersRelJushuitan rel =
				ordersRelJushuitanMapper.selectOne(
						new LambdaQueryWrapper<OrdersRelJushuitan>()
								.eq(OrdersRelJushuitan::getCompanyId, companyId)
								.eq(OrdersRelJushuitan::getOrderId, orderId)
								.last("LIMIT 1"));
		if (rel == null || rel.getOId() == null || rel.getOId() <= 0) {
			return Optional.empty();
		}
		List<AftersalesDetail> details =
				aftersalesDetailMapper.selectList(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getAftersalesBn, aftersalesBn)
								.last("LIMIT " + DETAIL_LIMIT));
		if (details == null || details.isEmpty()) {
			return Optional.empty();
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (AftersalesDetail d : details) {
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("sku_id", d.getItemBn() != null ? d.getItemBn() : "");
			line.put("name", d.getItemName() != null ? d.getItemName() : "");
			line.put("qty", d.getNum() != null ? d.getNum() : 0);
			line.put("amount", d.getRefundFee() != null ? d.getRefundFee() : 0);
			items.add(line);
		}
		Map<String, Object> biz = new LinkedHashMap<>();
		biz.put("so_id", String.valueOf(orderId));
		biz.put("o_id", rel.getOId());
		biz.put("outer_as_id", String.valueOf(aftersalesBn));
		biz.put("items", items);
		String reason = str(aftersalesRow.get("reason"));
		if (StringUtils.hasText(reason)) {
			biz.put("remark", reason);
		}
		String aftersalesType = str(aftersalesRow.get("aftersales_type"));
		if (StringUtils.hasText(aftersalesType)) {
			biz.put("aftersales_type", aftersalesType);
		}
		return Optional.of(biz);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static long longOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
