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

package cn.shopex.ecshopx.salesperson.integration;

import cn.shopex.ecshopx.common.port.salesperson.ShoppingGuideSalespersonRowReadPort;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShoppingGuideSalespersonRowReadPortImpl implements ShoppingGuideSalespersonRowReadPort {

	private final ShopSalespersonMapper shopSalespersonMapper;

	public ShoppingGuideSalespersonRowReadPortImpl(ShopSalespersonMapper shopSalespersonMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
	}

	@Override
	public Optional<Map<String, Object>> loadShoppingGuideRow(long companyId, long salespersonId) {
		if (salespersonId <= 0L) {
			return Optional.empty();
		}
		ShopSalesperson e =
				shopSalespersonMapper.selectOne(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.eq(ShopSalesperson::getSalespersonId, salespersonId)
								.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
								.last("LIMIT 1"));
		if (e == null) {
			return Optional.empty();
		}
		Map<String, Object> m = new LinkedHashMap<>();
		String workUserid =
				StringUtils.hasText(e.getWorkClearUserid())
						? e.getWorkClearUserid()
						: (e.getWorkUserid() != null ? e.getWorkUserid() : "");
		m.put("work_userid", workUserid);
		m.put("name", e.getName() != null ? e.getName() : "");
		return Optional.of(m);
	}
}
