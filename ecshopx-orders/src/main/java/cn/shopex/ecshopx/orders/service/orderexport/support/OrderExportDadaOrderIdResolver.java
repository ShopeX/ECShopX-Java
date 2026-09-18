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

package cn.shopex.ecshopx.orders.service.orderexport.support;

import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderExportDadaOrderIdResolver {

	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;

	public OrderExportDadaOrderIdResolver(NormalOrdersRelDadaMapper normalOrdersRelDadaMapper) {
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
	}

	public List<Long> resolve(LinkedHashMap<String, Object> filter) {
		long companyId = longVal(filter.get("company_id"));
		Integer dadaCode = parseDadaStatus(filter.get("order_status"));
		if (dadaCode == null) {
			return List.of();
		}
		List<NormalOrdersRelDada> rows =
				normalOrdersRelDadaMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersRelDada>()
								.eq(NormalOrdersRelDada::getCompanyId, companyId)
								.eq(NormalOrdersRelDada::getDadaStatus, dadaCode));
		List<Long> ids = new ArrayList<>();
		for (NormalOrdersRelDada row : rows) {
			if (row.getOrderId() != null && row.getOrderId() > 0L) {
				ids.add(row.getOrderId());
			}
		}
		return ids;
	}

	private static Integer parseDadaStatus(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		String u = s.toUpperCase();
		if (u.length() < 6 || !u.startsWith("DADA_")) {
			return null;
		}
		String suffix = s.substring(5);
		try {
			return Integer.parseInt(suffix);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
