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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailSalespersonLookupPort;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderDetailSalespersonLookupPortImpl implements AdminOrderDetailSalespersonLookupPort {

	private final ShopSalespersonMapper shopSalespersonMapper;

	public AdminOrderDetailSalespersonLookupPortImpl(ShopSalespersonMapper shopSalespersonMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
	}

	@Override
	public Map<String, Object> loadSalespersonForOrderDetail(long companyId, long salespersonId) {
		if (salespersonId <= 0L) {
			return Collections.emptyMap();
		}
		ShopSalesperson sp =
				shopSalespersonMapper.selectOne(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.eq(ShopSalesperson::getSalespersonId, salespersonId)
								.last("LIMIT 1"));
		if (sp == null) {
			return Collections.emptyMap();
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("salesperson_id", sp.getSalespersonId());
		m.put("name", sp.getName());
		m.put("mobile", sp.getMobile());
		m.put("number", sp.getNumber());
		m.put("shop_name", sp.getShopName());
		return m;
	}
}
