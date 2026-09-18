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

package cn.shopex.ecshopx.salesperson.integration.order;

import cn.shopex.ecshopx.common.order.front.WxappSalespersonIdByWorkUseridPort;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappSalespersonIdByWorkUseridPortImpl implements WxappSalespersonIdByWorkUseridPort {

	private final ShopSalespersonMapper shopSalespersonMapper;

	public WxappSalespersonIdByWorkUseridPortImpl(ShopSalespersonMapper shopSalespersonMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
	}

	@Override
	public long resolveSalespersonId(long companyId, String workUserid) {
		if (!StringUtils.hasText(workUserid)) {
			return 0L;
		}
		ShopSalesperson row =
				shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
						.eq(ShopSalesperson::getCompanyId, companyId)
						.eq(ShopSalesperson::getWorkUserid, workUserid.trim())
						.last("LIMIT 1"));
		return row != null && row.getSalespersonId() != null ? row.getSalespersonId() : 0L;
	}
}
