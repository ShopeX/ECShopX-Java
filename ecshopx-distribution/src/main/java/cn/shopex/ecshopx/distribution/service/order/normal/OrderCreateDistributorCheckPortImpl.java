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

package cn.shopex.ecshopx.distribution.service.order.normal;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderCreateDistributorCheckPortImpl implements OrderCreateDistributorCheckPort {

	private static final String SQL_IS_VALID_ACTIVE =
			"(is_valid = 1 OR LOWER(TRIM(CAST(is_valid AS CHAR))) IN ('true','1'))";

	private final DistributorMapper distributorMapper;

	public OrderCreateDistributorCheckPortImpl(DistributorMapper distributorMapper) {
		this.distributorMapper = distributorMapper;
	}

	@Override
	public void check(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		long distributorId = longVal(pr.get("distributor_id"), 0L);
		if (distributorId <= 0L) {
			return;
		}
		long companyId = longVal(pr.get("company_id"), 0L);
		Distributor d = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, distributorId)
				.apply(SQL_IS_VALID_ACTIVE)
				.last("LIMIT 1"));
		if (d == null) {
			throw new ResourceException("店铺不存在或已失效");
		}
		String receipt = String.valueOf(pr.getOrDefault("receipt_type", ""));
		if ("ziti".equals(receipt) && !Boolean.TRUE.equals(d.getIsZiti())) {
			throw new ResourceException("该门店不支持自提");
		}
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
