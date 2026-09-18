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

import cn.shopex.ecshopx.common.orders.port.AdminOrderListPromoterUserIdLookupPort;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderListPromoterUserIdLookupPortImpl implements AdminOrderListPromoterUserIdLookupPort {

	private final ShopSalespersonMapper shopSalespersonMapper;

	public AdminOrderListPromoterUserIdLookupPortImpl(ShopSalespersonMapper shopSalespersonMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
	}

	@Override
	public List<Long> listUserIdsBySalespersonNamePlaintext(long companyId, String salespersonnameTrimmed) {
		String enc = LegacyFixedMobileEncrypt.fixedEncryptMobile(salespersonnameTrimmed);
		Set<Long> ids = new LinkedHashSet<>();
		for (ShopSalesperson sp :
				shopSalespersonMapper.selectList(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.eq(ShopSalesperson::getName, enc))) {
			addUserId(ids, sp.getUserId());
		}
		for (ShopSalesperson sp :
				shopSalespersonMapper.selectList(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.eq(ShopSalesperson::getMobile, enc))) {
			addUserId(ids, sp.getUserId());
		}
		return new ArrayList<>(ids);
	}

	private static void addUserId(Set<Long> ids, Integer userId) {
		if (userId != null && userId != 0) {
			ids.add(userId.longValue());
		}
	}
}
