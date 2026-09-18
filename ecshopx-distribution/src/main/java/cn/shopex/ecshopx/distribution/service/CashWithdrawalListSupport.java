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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;

/** Shared parsing for cash withdrawal list gate and core (single code path). */
final class CashWithdrawalListSupport {

	private CashWithdrawalListSupport() {}

	static void validatePagination(Map<String, Object> merged) {
		Object pageObj = merged.get("page");
		if (pageObj == null || !(pageObj instanceof Number)) {
			throw new ResourceException("分页参数错误");
		}
		int page = ((Number) pageObj).intValue();
		if (page < 1) {
			throw new ResourceException("分页参数错误");
		}
		Object psObj = merged.get("pageSize");
		if (psObj == null || !(psObj instanceof Number)) {
			throw new ResourceException("每页最多查询50条数据");
		}
		int pageSize = ((Number) psObj).intValue();
		if (pageSize < 1 || pageSize > 50) {
			throw new ResourceException("每页最多查询50条数据");
		}
	}

	static long parseDistributorIdFilter(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
