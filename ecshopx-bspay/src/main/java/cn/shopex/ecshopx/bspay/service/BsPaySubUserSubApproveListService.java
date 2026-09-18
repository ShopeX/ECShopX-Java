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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.EntryApply;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BsPaySubUserSubApproveListService {

	private final EntryApplyMapper entryApplyMapper;
	private final BsPaySubUserSubApproveInfoService bsPaySubUserSubApproveInfoService;

	public BsPaySubUserSubApproveListService(
			EntryApplyMapper entryApplyMapper,
			BsPaySubUserSubApproveInfoService bsPaySubUserSubApproveInfoService) {
		this.entryApplyMapper = entryApplyMapper;
		this.bsPaySubUserSubApproveInfoService = bsPaySubUserSubApproveInfoService;
	}

	public Map<String, Object> subApproveLists(
			long companyId,
			List<String> status,
			String userName,
			List<String> address,
			String timeStart,
			String timeEnd,
			int page,
			int pageSize) {
		int effectivePage = page < 1 ? 1 : page;
		int effectivePageSize = pageSize < 1 ? 1 : pageSize;

		LambdaQueryWrapper<EntryApply> countWrapper = new LambdaQueryWrapper<>();
		applyListFilters(countWrapper, companyId, status, userName, address, timeStart, timeEnd);
		Long total = entryApplyMapper.selectCount(countWrapper);
		long totalCount = total == null ? 0L : total;

		List<Map<String, Object>> list;
		if (totalCount == 0L) {
			list = Collections.emptyList();
		} else {
			LambdaQueryWrapper<EntryApply> listWrapper = new LambdaQueryWrapper<>();
			applyListFilters(listWrapper, companyId, status, userName, address, timeStart, timeEnd);
			listWrapper.orderByDesc(EntryApply::getCreated);
			Page<EntryApply> mpPage = new Page<>(effectivePage, effectivePageSize, false);
			entryApplyMapper.selectPage(mpPage, listWrapper);
			List<EntryApply> records = mpPage.getRecords();
			if (records == null || records.isEmpty()) {
				list = Collections.emptyList();
			} else {
				list = new ArrayList<>(records.size());
				for (EntryApply e : records) {
					list.add(bsPaySubUserSubApproveInfoService.toSnakeMapForSubApproveEntryApply(e));
				}
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("list", list);
		return result;
	}

	private static void applyListFilters(
			LambdaQueryWrapper<EntryApply> w,
			long companyId,
			List<String> status,
			String userName,
			List<String> address,
			String timeStart,
			String timeEnd) {
		w.eq(EntryApply::getCompanyId, companyId);

		if (status != null && !status.isEmpty()) {
			List<String> cleaned =
					status.stream()
							.filter(s -> s != null)
							.map(String::trim)
							.filter(BsPaySubUserSubApproveListService::scalarTruthyForFilter)
							.toList();
			if (!cleaned.isEmpty()) {
				if (cleaned.size() == 1) {
					w.eq(EntryApply::getStatus, cleaned.get(0));
				} else {
					w.in(EntryApply::getStatus, cleaned);
				}
			}
		}

		if (scalarTruthyForFilter(userName)) {
			w.like(EntryApply::getUserName, userName.trim());
		}

		if (address != null && !address.isEmpty()) {
			List<String> cleanedAddr =
					address.stream()
							.filter(s -> s != null)
							.map(String::trim)
							.filter(BsPaySubUserSubApproveListService::scalarTruthyForFilter)
							.toList();
			if (!cleanedAddr.isEmpty()) {
				if (cleanedAddr.size() == 1) {
					w.eq(EntryApply::getAddress, cleanedAddr.get(0));
				} else {
					w.in(EntryApply::getAddress, cleanedAddr);
				}
			}
		}

		if (scalarTruthyForFilter(timeStart)) {
			int lower = leadingSignedIntFromRawString(timeStart);
			int upper = leadingSignedIntFromRawString(timeEnd) + 86399;
			w.ge(EntryApply::getCreated, lower);
			w.le(EntryApply::getCreated, upper);
		}
	}

	/** Scalar query parameter treated as “present” for optional filters (empty / {@code "0"} absent). */
	private static boolean scalarTruthyForFilter(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			return true;
		}
		if (v instanceof Number n) {
			if (n instanceof Double d) {
				return d.doubleValue() != 0.0d;
			}
			if (n instanceof Float f) {
				return f.floatValue() != 0.0f;
			}
			return n.longValue() != 0L;
		}
		return true;
	}

	/** Parse leading signed decimal digits from a string; empty or no digits yields 0. */
	private static int leadingSignedIntFromRawString(String raw) {
		if (raw == null) {
			return 0;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0;
		}
		int i = 0;
		int len = s.length();
		boolean negative = false;
		char c0 = s.charAt(0);
		if (c0 == '-') {
			negative = true;
			i = 1;
		} else if (c0 == '+') {
			i = 1;
		}
		if (i >= len) {
			return 0;
		}
		int startDigits = i;
		while (i < len && Character.isDigit(s.charAt(i))) {
			i++;
		}
		if (startDigits == i) {
			return 0;
		}
		String digitPart = s.substring(startDigits, i);
		try {
			long parsed = Long.parseLong(digitPart);
			if (negative) {
				parsed = -parsed;
			}
			if (parsed > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (parsed < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) parsed;
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
