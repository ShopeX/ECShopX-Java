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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.domain.RightsOperateLogs;
import cn.shopex.ecshopx.orders.mapper.RightsOperateLogsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RightsAdminGetRightsInfoService {

	private static final Pattern INTEGER_STRING = Pattern.compile("-?\\d+");

	private final RightsOperateLogsMapper rightsOperateLogsMapper;

	public RightsAdminGetRightsInfoService(RightsOperateLogsMapper rightsOperateLogsMapper) {
		this.rightsOperateLogsMapper = rightsOperateLogsMapper;
	}

	public Map<String, Object> getRightsInfo(Object rightsIdRaw) {
		LambdaQueryWrapper<RightsOperateLogs> w = Wrappers.lambdaQuery();
		applyRightsIdCondition(w, rightsIdRaw);
		w.orderByDesc(RightsOperateLogs::getCreated);

		long total = rightsOperateLogsMapper.selectCount(w);
		List<Map<String, Object>> list = new ArrayList<>();
		if (total > 0) {
			Page<RightsOperateLogs> page = new Page<>(1, 100, false);
			rightsOperateLogsMapper.selectPage(page, w);
			for (RightsOperateLogs row : page.getRecords()) {
				list.add(toRowMap(row));
			}
		}

		LinkedHashMap<String, Object> logs = new LinkedHashMap<>();
		int totalCount = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
		logs.put("total_count", totalCount);
		logs.put("list", list);
		return logs;
	}

	private static void applyRightsIdCondition(
			LambdaQueryWrapper<RightsOperateLogs> w, Object rightsIdRaw) {
		if (rightsIdRaw == null) {
			w.isNull(RightsOperateLogs::getRightsId);
			return;
		}
		if (rightsIdRaw instanceof Number n) {
			w.eq(RightsOperateLogs::getRightsId, n.longValue());
			return;
		}
		if (rightsIdRaw instanceof String s) {
			applyStringRightsId(w, s);
			return;
		}
		applyStringRightsId(w, String.valueOf(rightsIdRaw));
	}

	private static void applyStringRightsId(LambdaQueryWrapper<RightsOperateLogs> w, String s) {
		if (INTEGER_STRING.matcher(s).matches()) {
			w.eq(RightsOperateLogs::getRightsId, Long.parseLong(s));
		} else {
			w.apply("rights_id = {0}", s);
		}
	}

	private static Map<String, Object> toRowMap(RightsOperateLogs row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("rights_id", row.getRightsId());
		m.put("user_id", row.getUserId());
		m.put("company_id", row.getCompanyId());
		m.put("remark", row.getRemark());
		m.put("operator_id", row.getOperatorId());
		m.put("operator", row.getOperator());
		m.put("original_date", row.getOriginalDate());
		m.put("delay_date", row.getDelayDate());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		return m;
	}
}
