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

package cn.shopex.ecshopx.chinaumspay.service;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionErrorLog;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionErrorLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DivisionErrorLogListService {

	private final ChinaumspayDivisionErrorLogMapper chinaumspayDivisionErrorLogMapper;

	public DivisionErrorLogListService(ChinaumspayDivisionErrorLogMapper chinaumspayDivisionErrorLogMapper) {
		this.chinaumspayDivisionErrorLogMapper = chinaumspayDivisionErrorLogMapper;
	}

	public Map<String, Object> query(Map<String, Object> jwtUser, String status, String orderId,
			String timeStartBegin, String timeStartEnd, Integer page, Integer pageSize) {
		int pageNum = normalizePage(page);
		int size = normalizePageSize(pageSize);

		LambdaQueryWrapper<ChinaumspayDivisionErrorLog> w = new LambdaQueryWrapper<>();
		w.eq(ChinaumspayDivisionErrorLog::getCompanyId, toLong(jwtUser.get("company_id")));

		if ("waiting".equals(status)) {
			w.eq(ChinaumspayDivisionErrorLog::getIsResubmit, DivisionErrorLogResubmitService.IS_RESUBMIT_WAITING);
		} else if ("is_resubmit".equals(status)) {
			w.eq(ChinaumspayDivisionErrorLog::getIsResubmit, DivisionErrorLogResubmitService.IS_RESUBMIT_SUCC);
		} else if ("not".equals(status)) {
			w.eq(ChinaumspayDivisionErrorLog::getIsResubmit, DivisionErrorLogResubmitService.IS_RESUBMIT_NOT);
		}

		String trimmedOrderId = orderId == null ? "" : orderId.trim();
		if (!trimmedOrderId.isEmpty() && !"0".equals(trimmedOrderId)) {
			w.apply("order_id = {0}", trimmedOrderId);
		}

		boolean timeBeginPresent = timeStartBegin != null && !timeStartBegin.trim().isEmpty();
		boolean timeEndPresent = timeStartEnd != null && !timeStartEnd.trim().isEmpty();
		if (timeBeginPresent && timeEndPresent) {
			Integer beginTs = parseIntOrNull(timeStartBegin.trim());
			Integer endTs = parseIntOrNull(timeStartEnd.trim());
			if (beginTs != null) {
				w.ge(ChinaumspayDivisionErrorLog::getCreateTime, beginTs);
			}
			if (endTs != null) {
				w.le(ChinaumspayDivisionErrorLog::getCreateTime, endTs);
			}
		}

		if ("distributor".equals(jwtUser.get("operator_type"))) {
			w.eq(ChinaumspayDivisionErrorLog::getDistributorId, toLong(jwtUser.get("distributor_id")));
		}

		long total = chinaumspayDivisionErrorLogMapper.selectCount(w);

		List<Map<String, Object>> list;
		if (total == 0) {
			list = Collections.emptyList();
		} else {
			w.orderByDesc(ChinaumspayDivisionErrorLog::getId);
			Page<ChinaumspayDivisionErrorLog> p = new Page<>(pageNum, size, false);
			chinaumspayDivisionErrorLogMapper.selectPage(p, w);
			List<ChinaumspayDivisionErrorLog> records = p.getRecords();
			list = new ArrayList<>(records.size());
			for (ChinaumspayDivisionErrorLog e : records) {
				list.add(toOutputRow(e));
			}
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", total);
		body.put("list", list);
		return body;
	}

	private static Map<String, Object> toOutputRow(ChinaumspayDivisionErrorLog e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("division_id", e.getDivisionId());
		m.put("upload_detail_id", e.getUploadDetailId());
		m.put("type", e.getType());
		m.put("distributor_id", e.getDistributorId());
		m.put("status", e.getStatus());
		m.put("is_resubmit", e.getIsResubmit());
		m.put("create_time", e.getCreateTime());
		m.put("update_time", e.getUpdateTime());
		String raw = e.getErrorDesc();
		m.put("error_code", raw);
		m.put("error_desc", getErrorDesc(raw));
		return m;
	}

	private static String getErrorDesc(String errorDesc) {
		if (errorDesc == null) {
			return null;
		}
		return switch (errorDesc) {
			case "COMPANYNO_INVALID" -> "企业用户号非法，企业用户号没有维护在当前集团下";
			case "ORDER_NOT_EXIST" -> "订单编号不存在或找到多条";
			case "STL_TYPE_INVALID" -> "划付类型非法";
			case "AMOUNT_INVALID" -> "金额非法";
			case "FEE_ERROR" -> "订单金额不足以提现";
			case "NOT_ENOUGH" -> "余额不足";
			case "SPLIT_ACCOUNT_INVALID" -> "分账方非法";
			case "MER_DETAIL_NO_REPEAT" -> "指令ID重复";
			case "MULTIAPP_ERROR" -> "商户多应用信息异常";
			case "EXCEED_LIMIT" -> "累计分账超限";
			case "UNKNOWN_ERROR" -> "未知错误";
			default -> errorDesc;
		};
	}

	private static int normalizePage(Integer page) {
		if (page == null || page < 1) {
			return 1;
		}
		return page;
	}

	private static int normalizePageSize(Integer pageSize) {
		if (pageSize == null || pageSize < 1) {
			return 20;
		}
		return pageSize;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static Integer parseIntOrNull(String s) {
		if (s == null || s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
