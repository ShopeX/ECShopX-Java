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

package cn.shopex.ecshopx.orders.service.saaserp;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpNormalOrderLookupPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OrdersSaasErpNormalOrderLookupAdapter implements SaasErpNormalOrderLookupPort {

	private final NormalOrdersMapper normalOrdersMapper;

	public OrdersSaasErpNormalOrderLookupAdapter(NormalOrdersMapper normalOrdersMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
	}

	@Override
	public Optional<Map<String, Object>> find(long companyId, String orderId) {
		Long parsedOrderId = parseOrderId(orderId);
		if (parsedOrderId == null) {
			return Optional.empty();
		}
		LambdaQueryWrapper<NormalOrders> query = new LambdaQueryWrapper<>();
		query.eq(NormalOrders::getOrderId, parsedOrderId);
		if (companyId > 0) {
			query.eq(NormalOrders::getCompanyId, companyId);
		}
		query.last("LIMIT 1");
		NormalOrders row = normalOrdersMapper.selectOne(query);
		if (row == null) {
			return Optional.empty();
		}
		Map<String, Object> found = new LinkedHashMap<>();
		found.put("order_id", row.getOrderId());
		found.put("company_id", row.getCompanyId());
		found.put("order_class", row.getOrderClass());
		found.put("user_id", row.getUserId());
		return Optional.of(found);
	}

	private static Long parseOrderId(String orderId) {
		if (orderId == null || orderId.isEmpty()) {
			return null;
		}
		try {
			return Long.valueOf(orderId);
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
