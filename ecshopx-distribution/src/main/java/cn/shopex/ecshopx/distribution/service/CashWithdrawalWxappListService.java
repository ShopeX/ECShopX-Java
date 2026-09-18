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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.domain.CashWithdrawal;
import cn.shopex.ecshopx.distribution.mapper.CashWithdrawalMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CashWithdrawalWxappListService {

	private final CashWithdrawalMapper cashWithdrawalMapper;
	private final MessageSource messageSource;

	public CashWithdrawalWxappListService(
			CashWithdrawalMapper cashWithdrawalMapper, MessageSource messageSource) {
		this.cashWithdrawalMapper = cashWithdrawalMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getCashWithdrawalList(
			long companyId, String userId, Object pageRaw, Object pageSizeRaw, Locale locale) {
		int p = validatePage(pageRaw, locale);
		int ps = validatePageSize(pageSizeRaw, locale);

		LambdaQueryWrapper<CashWithdrawal> w = new LambdaQueryWrapper<>();
		w.eq(CashWithdrawal::getCompanyId, companyId);
		w.eq(CashWithdrawal::getUserId, userId);
		w.orderByDesc(CashWithdrawal::getCreated);

		Page<CashWithdrawal> page = new Page<>(p, ps);
		cashWithdrawalMapper.selectPage(page, w);

		int totalCount = (int) page.getTotal();
		List<Map<String, Object>> list = new ArrayList<>();
		for (CashWithdrawal cw : page.getRecords()) {
			list.add(toListRow(cw));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", list);
		return data;
	}

	public Map<String, Object> salesmanGetCashWithdrawalList(
			long companyId,
			String userId,
			Object pageRaw,
			Object pageSizeRaw,
			Object distributorIdRaw,
			Locale locale) {
		int p = validatePage(pageRaw, locale);
		int ps = validatePageSize(pageSizeRaw, locale);

		LambdaQueryWrapper<CashWithdrawal> w = new LambdaQueryWrapper<>();
		w.eq(CashWithdrawal::getCompanyId, companyId);
		w.eq(CashWithdrawal::getUserId, userId);
		long distributorId = parsePositiveLongOrZero(distributorIdRaw);
		if (distributorId > 0L) {
			w.eq(CashWithdrawal::getDistributorId, Long.toString(distributorId));
		}
		w.orderByDesc(CashWithdrawal::getCreated);

		Page<CashWithdrawal> page = new Page<>(p, ps);
		cashWithdrawalMapper.selectPage(page, w);

		int totalCount = (int) page.getTotal();
		List<Map<String, Object>> list = new ArrayList<>();
		for (CashWithdrawal cw : page.getRecords()) {
			list.add(toListRow(cw));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", list);
		return data;
	}

	private int validatePage(Object pageRaw, Locale locale) {
		if (pageRaw == null) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.page_error", null, "分页参数错误", locale));
		}
		Integer parsed = parsePositiveDigitsInt(pageRaw);
		if (parsed == null || parsed < 1) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.page_error", null, "分页参数错误", locale));
		}
		return parsed;
	}

	private int validatePageSize(Object pageSizeRaw, Locale locale) {
		if (pageSizeRaw == null) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.pagesize_max",
							null,
							"每页最多查询50条数据",
							locale));
		}
		Integer parsed = parsePositiveDigitsInt(pageSizeRaw);
		if (parsed == null || parsed < 1 || parsed > 50) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.pagesize_max",
							null,
							"每页最多查询50条数据",
							locale));
		}
		return parsed;
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Integer parsePositiveDigitsInt(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || !t.matches("^\\d+$")) {
				return null;
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static Map<String, Object> toListRow(CashWithdrawal cw) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", cw.getId());
		row.put("company_id", cw.getCompanyId());
		row.put("distributor_id", cw.getDistributorId());
		row.put("distributor_name", cw.getDistributorName());
		row.put("open_id", cw.getOpenId());
		row.put("user_id", cw.getUserId());
		row.put("distributor_mobile", cw.getDistributorMobile());
		row.put("money", cw.getMoney());
		row.put("status", cw.getStatus());
		row.put("remarks", cw.getRemarks());
		row.put("wxa_appid", cw.getWxaAppid());
		row.put("shop_id", cw.getShopId());
		row.put("created", cw.getCreated());
		row.put("updated", cw.getUpdated());
		return row;
	}
}
