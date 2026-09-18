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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.domain.DistributeLogs;
import cn.shopex.ecshopx.distribution.mapper.DistributeLogsMapper;
import cn.shopex.ecshopx.distribution.support.DistributeLogsColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributeLogsListCoreService {

	private final DistributeLogsMapper distributeLogsMapper;
	private final DistributeLogsColumnNamesDataMapper distributeLogsColumnNamesDataMapper;

	public DistributeLogsListCoreService(
			DistributeLogsMapper distributeLogsMapper,
			DistributeLogsColumnNamesDataMapper distributeLogsColumnNamesDataMapper) {
		this.distributeLogsMapper = distributeLogsMapper;
		this.distributeLogsColumnNamesDataMapper = distributeLogsColumnNamesDataMapper;
	}

	public Map<String, Object> buildList(Map<String, Object> user, Map<String, Object> merged, Locale locale) {
		long companyId = longOf(user.get("company_id"));
		LambdaQueryWrapper<DistributeLogs> w = new LambdaQueryWrapper<>();
		w.eq(DistributeLogs::getCompanyId, companyId);

		long queryDid = CashWithdrawalListSupport.parseDistributorIdFilter(merged.get("distributor_id"));
		if (queryDid > 0) {
			w.eq(DistributeLogs::getDistributorId, queryDid);
		}

		Object isCloseRawObj = merged.get("is_close_raw");
		String isCloseRaw = isCloseRawObj instanceof String s ? s : null;
		if (isCloseRaw != null && isStringTruthy(isCloseRaw)) {
			w.eq(DistributeLogs::getIsClose, looseEqualTrue(isCloseRaw));
		}

		int page = ((Number) merged.get("page")).intValue();
		int pageSize = ((Number) merged.get("pageSize")).intValue();
		Page<DistributeLogs> p = new Page<>(page, pageSize);
		distributeLogsMapper.selectPage(p, w.orderByDesc(DistributeLogs::getCreateTime));

		List<Map<String, Object>> list = new ArrayList<>();
		for (DistributeLogs row : p.getRecords()) {
			list.add(distributeLogsColumnNamesDataMapper.toRow(row, locale));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", (int) p.getTotal());
		data.put("list", list);
		return data;
	}

	static boolean isStringTruthy(String s) {
		if (s == null) {
			return false;
		}
		if (s.isEmpty()) {
			return false;
		}
		if ("0".equals(s)) {
			return false;
		}
		return true;
	}

	static boolean looseEqualTrue(String raw) {
		return "true".equals(raw);
	}

	private static long longOf(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
