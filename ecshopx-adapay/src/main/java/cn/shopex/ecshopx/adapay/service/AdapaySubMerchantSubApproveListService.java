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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayEntryApply;
import cn.shopex.ecshopx.adapay.mapper.AdapayEntryApplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdapaySubMerchantSubApproveListService {

	private final AdapayEntryApplyMapper adapayEntryApplyMapper;
	private final AdapaySubMerchantSubApproveInfoService adapaySubMerchantSubApproveInfoService;

	public AdapaySubMerchantSubApproveListService(
			AdapayEntryApplyMapper adapayEntryApplyMapper,
			AdapaySubMerchantSubApproveInfoService adapaySubMerchantSubApproveInfoService) {
		this.adapayEntryApplyMapper = adapayEntryApplyMapper;
		this.adapaySubMerchantSubApproveInfoService = adapaySubMerchantSubApproveInfoService;
	}

	public Map<String, Object> subApproveLists(
			long companyId,
			String status,
			String userName,
			String address,
			String timeStart,
			String timeEnd,
			int page,
			int pageSize) {
		int effectivePage = page < 1 ? 1 : page;
		int effectivePageSize = pageSize < 1 ? 1 : pageSize;

		LambdaQueryWrapper<AdapayEntryApply> countWrapper = new LambdaQueryWrapper<>();
		applyListFilters(countWrapper, companyId, status, userName, address, timeStart, timeEnd);
		Long totalCountObj = adapayEntryApplyMapper.selectCount(countWrapper);
		long totalCount = totalCountObj == null ? 0L : totalCountObj.longValue();

		List<Map<String, Object>> listOfMaps;
		if (totalCount == 0L) {
			listOfMaps = Collections.emptyList();
		} else {
			LambdaQueryWrapper<AdapayEntryApply> listWrapper = new LambdaQueryWrapper<>();
			applyListFilters(listWrapper, companyId, status, userName, address, timeStart, timeEnd);
			listWrapper.orderByDesc(AdapayEntryApply::getCreateTime);
			Page<AdapayEntryApply> mpPage = new Page<>(effectivePage, effectivePageSize, false);
			adapayEntryApplyMapper.selectPage(mpPage, listWrapper);
			List<AdapayEntryApply> records = mpPage.getRecords();
			if (records == null || records.isEmpty()) {
				listOfMaps = Collections.emptyList();
			} else {
				listOfMaps = new ArrayList<>(records.size());
				for (AdapayEntryApply e : records) {
					listOfMaps.add(
							adapaySubMerchantSubApproveInfoService.toSnakeMapForSubApproveEntryApply(e));
				}
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("list", listOfMaps);
		return result;
	}

	private static void applyListFilters(
			LambdaQueryWrapper<AdapayEntryApply> w,
			long companyId,
			String status,
			String userName,
			String address,
			String timeStart,
			String timeEnd) {
		w.eq(AdapayEntryApply::getCompanyId, String.valueOf(companyId));
		if (scalarTruthyForFilter(status)) {
			w.eq(AdapayEntryApply::getStatus, status.trim());
		}
		if (scalarTruthyForFilter(userName)) {
			w.like(AdapayEntryApply::getUserName, userName.trim());
		}
		if (scalarTruthyForFilter(address)) {
			w.eq(AdapayEntryApply::getAddress, address.trim());
		}
		if (scalarTruthyForFilter(timeStart)) {
			int lowerBound = leadingSignedIntFromRawString(timeStart);
			int upperBound = leadingSignedIntFromRawString(timeEnd) + 86399;
			w.ge(AdapayEntryApply::getCreateTime, lowerBound);
			w.le(AdapayEntryApply::getCreateTime, upperBound);
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
