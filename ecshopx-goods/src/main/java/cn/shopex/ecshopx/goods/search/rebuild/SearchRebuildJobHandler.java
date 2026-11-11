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

package cn.shopex.ecshopx.goods.search.rebuild;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 商品搜索索引全量重建 Job。
 *
 * <p>payload：
 *
 * <ul>
 *   <li>{@code all_companies=true}：蓝绿重建全部未禁用且未过期公司
 *   <li>{@code company_id}（必填，除非 all_companies）：按公司 upsert 到当前 alias；可选 {@code
 *       distributor_id}；可选 {@code blue_green=true} 走单公司蓝绿（会替换整站 alias，慎用）
 * </ul>
 */
@Component
public class SearchRebuildJobHandler implements DispatchHandler {

	private final SearchRebuildService rebuildService;
	private final CompanysMapper companysMapper;

	public SearchRebuildJobHandler(SearchRebuildService rebuildService, CompanysMapper companysMapper) {
		this.rebuildService = rebuildService;
		this.companysMapper = companysMapper;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Map<String, Object> body = payload != null ? payload : Map.of();
		try {
			if (isTruthy(body.get("all_companies"))) {
				rebuildService.rebuildCompanies(listActiveCompanyIds());
				return;
			}
			long companyId = requirePositiveLong(body.get("company_id"), "company_id");
			Long distributorId = parseOptionalPositiveLong(body.get("distributor_id"));
			if (isTruthy(body.get("blue_green"))) {
				rebuildService.rebuild(companyId, distributorId);
			} else {
				rebuildService.reindexCompanyIntoAlias(companyId, distributorId);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("goods search index rebuild failed", e);
		}
	}

	List<Long> listActiveCompanyIds() {
		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<Companys> w = new LambdaQueryWrapper<>();
		w.select(Companys::getCompanyId)
				.and(q -> q.isNull(Companys::getIsDisabled).or().eq(Companys::getIsDisabled, false))
				.and(q -> q.isNull(Companys::getExpiredAt).or().gt(Companys::getExpiredAt, nowSec))
				.orderByAsc(Companys::getCompanyId);
		List<Companys> rows = companysMapper.selectList(w);
		List<Long> ids = new ArrayList<>();
		if (rows != null) {
			for (Companys row : rows) {
				if (row.getCompanyId() != null && row.getCompanyId() > 0L) {
					ids.add(row.getCompanyId());
				}
			}
		}
		return ids;
	}

	private static boolean isTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = raw.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
	}

	private static long requirePositiveLong(Object raw, String field) {
		long value = toLong(raw);
		if (value <= 0) {
			throw new IllegalArgumentException(field + " is required and must be positive");
		}
		return value;
	}

	private static Long parseOptionalPositiveLong(Object raw) {
		if (raw == null) {
			return null;
		}
		long value = toLong(raw);
		return value > 0 ? value : null;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
