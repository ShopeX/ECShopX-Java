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

package cn.shopex.ecshopx.orders.service.companyrellogistics;

import cn.shopex.ecshopx.orders.mapper.CompanyRelLogisticsMapper;
import cn.shopex.ecshopx.orders.service.setting.KuaidiSettingRedisService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CompanyRelLogisticsTradeListService {

	private final KuaidiSettingRedisService kuaidiSettingRedisService;
	private final CompanyRelLogisticsMapper companyRelLogisticsMapper;

	public CompanyRelLogisticsTradeListService(
			KuaidiSettingRedisService kuaidiSettingRedisService,
			CompanyRelLogisticsMapper companyRelLogisticsMapper) {
		this.kuaidiSettingRedisService = kuaidiSettingRedisService;
		this.companyRelLogisticsMapper = companyRelLogisticsMapper;
	}

	public Map<String, Object> getLogisticsList(
			long companyId, String operatorType, Long operatorIdOrNull, int distributorId) {
		long supplierId =
				(Objects.equals(operatorType, "supplier") && operatorIdOrNull != null)
						? operatorIdOrNull.longValue()
						: 0L;
		long distributorIdLong = distributorId;
		String kuaidiType = kuaidiSettingRedisService.getKuaidiTypeOpenConfigRaw(companyId);
		boolean asKuaidi100 = "kuaidi100".equals(kuaidiType);
		long totalCount =
				companyRelLogisticsMapper.countTradeLogisticsList(companyId, distributorIdLong, supplierId);
		List<Map<String, Object>> dbRows;
		if (totalCount > 0) {
			dbRows =
					asKuaidi100
							? companyRelLogisticsMapper.selectTradeLogisticsListAsKuaidi100(
									companyId, distributorIdLong, supplierId)
							: companyRelLogisticsMapper.selectTradeLogisticsListAsCorpCode(
									companyId, distributorIdLong, supplierId);
		} else {
			dbRows = new ArrayList<>();
		}
		Map<String, Object> other = new LinkedHashMap<>();
		other.put("value", "OTHER");
		other.put("name", "其他");
		List<Map<String, Object>> list = new ArrayList<>(dbRows.size() + 1);
		list.add(other);
		list.addAll(dbRows);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}
}
