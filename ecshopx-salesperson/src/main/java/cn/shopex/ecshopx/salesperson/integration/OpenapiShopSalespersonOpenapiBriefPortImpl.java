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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.salesperson.port.OpenapiShopSalespersonOpenapiBriefPort;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiShopSalespersonOpenapiBriefPortImpl implements OpenapiShopSalespersonOpenapiBriefPort {

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OpenapiShopSalespersonOpenapiBriefPortImpl(
			ShopSalespersonMapper shopSalespersonMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Override
	public Map<String, Object> findByMobile(long companyId, String mobilePlain) {
		if (!StringUtils.hasText(mobilePlain)) {
			return null;
		}
		ShopSalesperson row =
				shopSalespersonMapper.selectOne(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.eq(ShopSalesperson::getMobile, sensitiveFieldEncryptor.encrypt(mobilePlain.trim()))
								.last("LIMIT 1"));
		return row == null ? null : toBriefRow(row);
	}

	@Override
	public Map<Long, Map<String, Object>> listBySalespersonIds(long companyId, List<Long> salespersonIds) {
		if (salespersonIds == null || salespersonIds.isEmpty()) {
			return Map.of();
		}
		List<ShopSalesperson> rows =
				shopSalespersonMapper.selectList(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.in(ShopSalesperson::getSalespersonId, salespersonIds));
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (ShopSalesperson row : rows) {
			if (row.getSalespersonId() != null) {
				out.put(row.getSalespersonId(), toBriefRow(row));
			}
		}
		return out;
	}

	private Map<String, Object> toBriefRow(ShopSalesperson row) {
		Map<String, Object> brief = new LinkedHashMap<>();
		brief.put("salesperson_id", row.getSalespersonId());
		brief.put("name", row.getName() != null ? row.getName() : "");
		String mobilePlain =
				row.getMobile() == null ? "" : sensitiveFieldEncryptor.decrypt(row.getMobile());
		brief.put("mobile", mobilePlain);
		return brief;
	}
}
