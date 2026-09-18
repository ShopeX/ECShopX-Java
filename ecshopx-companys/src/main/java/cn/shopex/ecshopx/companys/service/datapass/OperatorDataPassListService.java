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

package cn.shopex.ecshopx.companys.service.datapass;

import cn.shopex.ecshopx.companys.domain.OperatorDataPass;
import cn.shopex.ecshopx.companys.dto.OperatorDataPassListRow;
import cn.shopex.ecshopx.companys.mapper.OperatorDataPassMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorDataPassListService {

	private final OperatorDataPassMapper operatorDataPassMapper;
	private final ObjectMapper objectMapper;
	private final OperatorDataPassExPassFormatter exPassFormatter;

	public OperatorDataPassListService(
			OperatorDataPassMapper operatorDataPassMapper,
			ObjectMapper objectMapper,
			OperatorDataPassExPassFormatter exPassFormatter) {
		this.operatorDataPassMapper = operatorDataPassMapper;
		this.objectMapper = objectMapper;
		this.exPassFormatter = exPassFormatter;
	}

	public Map<String, Object> listDataPass(
			long companyId,
			long merchantId,
			long operatorId,
			String operatorType,
			Integer pageParam,
			Integer pageSizeParam,
			String paramsJson,
			String loginName,
			String status,
			Integer startTime,
			Integer endTime) {
		int page = pageParam != null && pageParam > 0 ? pageParam : 1;
		int pageSize = pageSizeParam != null && pageSizeParam > 0 ? pageSizeParam : 10;
		if (StringUtils.hasText(paramsJson)) {
			try {
				Map<String, Object> pm = objectMapper.readValue(paramsJson.trim(), new TypeReference<>() {});
				page = coalescePageFromParamsMap(pm);
				pageSize = coalescePageSizeFromParamsMap(pm);
			} catch (Exception ignored) {
				// keep page / pageSize from query
			}
		}

		String op = operatorType == null ? "" : operatorType.trim();

		OperatorDataPassListFilter filter = new OperatorDataPassListFilter();
		filter.setCompanyId(companyId);
		filter.setMerchantId(merchantId);
		if (!"admin".equals(op) && !"merchant".equals(op)) {
			filter.setRestrictOperatorId(Math.toIntExact(operatorId));
		}
		if (StringUtils.hasText(loginName)) {
			filter.setLoginNameEq(loginName.trim());
		}
		if (status != null && !status.isEmpty()) {
			try {
				filter.setStatusEq(Integer.parseInt(status.trim()));
			} catch (NumberFormatException ignored) {
				// skip invalid status
			}
		}
		if (startTime != null && endTime != null && startTime != 0 && endTime != 0) {
			filter.setCreateTimeGte(startTime);
			filter.setCreateTimeLte(endTime);
		}

		long total = operatorDataPassMapper.countDataPassListJoin(filter);
		int offset = (page - 1) * pageSize;
		List<OperatorDataPassListRow> rows = operatorDataPassMapper.pageDataPassListJoin(filter, offset, pageSize);

		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (OperatorDataPassListRow r : rows) {
			list.add(toResponseRow(r));
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", total);
		body.put("list", list);
		return body;
	}

	private static int coalescePageFromParamsMap(Map<String, Object> pm) {
		Object raw = pm.get("page");
		if (raw == null) {
			return 1;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException ignored) {
			return 1;
		}
	}

	private static int coalescePageSizeFromParamsMap(Map<String, Object> pm) {
		Object raw = pm.get("page_size");
		if (raw == null) {
			return 10;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException ignored) {
			return 10;
		}
	}

	private Map<String, Object> toResponseRow(OperatorDataPassListRow r) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		putLong(m, "pass_id", r.getPassId());
		putLong(m, "company_id", r.getCompanyId());
		m.put("operator_id", r.getOperatorId());
		m.put("status", r.getStatus());
		m.put("is_closed", r.getIsClosed());
		m.put("reason", r.getReason() == null ? "" : r.getReason());
		m.put("remarks", r.getRemarks() == null ? "" : r.getRemarks());
		m.put("create_time", r.getCreateTime());
		m.put("approve_time", r.getApproveTime());
		putLong(m, "merchant_id", r.getMerchantId());
		m.put("rule", r.getRule() == null ? "" : r.getRule());

		OperatorDataPass temp = new OperatorDataPass();
		temp.setRule(r.getRule());
		temp.setStartTime(r.getStartTime());
		temp.setEndTime(r.getEndTime());
		exPassFormatter.applyExPassItemToMap(temp, m);

		m.put("login_name", r.getLoginName() == null ? "" : r.getLoginName());
		m.put("head_portrait", r.getHeadPortrait() == null ? "" : r.getHeadPortrait());
		m.put("operator_type", r.getOperatorType() == null ? "" : r.getOperatorType());
		return m;
	}

	private static void putLong(Map<String, Object> m, String key, Long v) {
		m.put(key, v == null ? 0L : v);
	}
}
