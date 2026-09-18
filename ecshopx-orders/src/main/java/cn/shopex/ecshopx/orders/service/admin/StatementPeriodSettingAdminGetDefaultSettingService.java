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

import cn.shopex.ecshopx.orders.domain.StatementPeriodSetting;
import cn.shopex.ecshopx.orders.mapper.StatementPeriodSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StatementPeriodSettingAdminGetDefaultSettingService {

	private final StatementPeriodSettingMapper statementPeriodSettingMapper;
	private final ObjectMapper objectMapper;

	public StatementPeriodSettingAdminGetDefaultSettingService(
			StatementPeriodSettingMapper statementPeriodSettingMapper, ObjectMapper objectMapper) {
		this.statementPeriodSettingMapper = statementPeriodSettingMapper;
		this.objectMapper = objectMapper;
	}

	public Object getDefaultSetting(long companyId, String merchantType) {
		LambdaQueryWrapper<StatementPeriodSetting> w = new LambdaQueryWrapper<StatementPeriodSetting>()
				.eq(StatementPeriodSetting::getCompanyId, companyId)
				.eq(StatementPeriodSetting::getDistributorId, 0L)
				.eq(StatementPeriodSetting::getMerchantType, merchantType)
				.orderByDesc(StatementPeriodSetting::getUpdated);
		Page<StatementPeriodSetting> page = new Page<>(1, 1);
		statementPeriodSettingMapper.selectPage(page, w);
		StatementPeriodSetting row =
				page.getRecords().isEmpty() ? null : page.getRecords().get(0);
		if (row == null) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("merchant_id", row.getMerchantId());
		m.put("distributor_id", row.getDistributorId());
		m.put("period", decodePeriodLenient(row.getPeriod()));
		m.put("supplier_id", row.getSupplierId());
		m.put("merchant_type", row.getMerchantType());
		return m;
	}

	private Object decodePeriodLenient(String periodStr) {
		if (periodStr == null || periodStr.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(periodStr, new TypeReference<List<Object>>() {});
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
