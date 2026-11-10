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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 商品搜索索引全量重建 Job。payload：{@code company_id}（必填）、{@code distributor_id}（可选，省略则整公司）。
 */
@Component
public class SearchRebuildJobHandler implements DispatchHandler {

	private final SearchRebuildService rebuildService;

	public SearchRebuildJobHandler(SearchRebuildService rebuildService) {
		this.rebuildService = rebuildService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = requirePositiveLong(payload.get("company_id"), "company_id");
		Long distributorId = parseOptionalPositiveLong(payload.get("distributor_id"));
		try {
			rebuildService.rebuild(companyId, distributorId);
		} catch (IOException e) {
			throw new UncheckedIOException("goods search index rebuild failed", e);
		}
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
