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

package cn.shopex.ecshopx.aftersales.service.export;

import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.dispatch.AftersaleFinancialExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class AftersalesFinancialExportService {

	private final AftersalesMapper aftersalesMapper;
	private final AftersaleFinancialExportFileJobDispatchPublisher aftersaleFinancialExportFileJobDispatchPublisher;

	public AftersalesFinancialExportService(
			AftersalesMapper aftersalesMapper,
			AftersaleFinancialExportFileJobDispatchPublisher aftersaleFinancialExportFileJobDispatchPublisher) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersaleFinancialExportFileJobDispatchPublisher = aftersaleFinancialExportFileJobDispatchPublisher;
	}

	public void financialExport(long companyId, long operatorId, String timeStartBegin, String timeStartEnd, String orderId) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("aftersales_status", 2);

		if (!isLooseEmpty(timeStartBegin) && !isLooseEmpty(timeStartEnd)) {
			long beginSec = LeadingNumberParser.parseAsLong(timeStartBegin.trim());
			long endSec = LeadingNumberParser.parseAsLong(timeStartEnd.trim());
			filter.put("create_time_gte", (int) beginSec);
			filter.put("create_time_lte", (int) endSec);
		}

		if (!isLooseEmpty(orderId)) {
			try {
				filter.put("order_id_eq", Long.parseLong(orderId.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("order_id 格式错误");
			}
		}

		long count = aftersalesMapper.countAdminList(filter);
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (count > 15000) {
			throw new ResourceException("导出有误，最高导出15000条数据");
		}

		aftersaleFinancialExportFileJobDispatchPublisher.publish(companyId, operatorId, new LinkedHashMap<>(filter));
	}

	private static boolean isLooseEmpty(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof Boolean b) {
			return !b;
		}
		if (o instanceof Number n) {
			if (o instanceof Double d) {
				return d == 0.0;
			}
			if (o instanceof Float f) {
				return f == 0.0f;
			}
			return n.longValue() == 0L;
		}
		if (o instanceof CharSequence s) {
			String t = s.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (o instanceof Character c) {
			return c == '0' || c == 0;
		}
		if (o instanceof java.util.Collection<?> c) {
			return c.isEmpty();
		}
		if (o instanceof java.util.Map<?, ?> m) {
			return m.isEmpty();
		}
		if (o instanceof Object[] arr) {
			return arr.length == 0;
		}
		return false;
	}
}
