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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.members.domain.MemberOperateLog;
import cn.shopex.ecshopx.members.mapper.MemberOperateLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberOperateLogListService {

	private final MemberOperateLogMapper memberOperateLogMapper;

	public AdminMemberOperateLogListService(MemberOperateLogMapper memberOperateLogMapper) {
		this.memberOperateLogMapper = memberOperateLogMapper;
	}

	public Map<String, Object> gerMemberOperateLogList(long companyId, String userIdRaw) {
		LambdaQueryWrapper<MemberOperateLog> w = new LambdaQueryWrapper<>();
		w.eq(MemberOperateLog::getCompanyId, companyId);

		if (StringUtils.hasText(userIdRaw)) {
			String trimmed = userIdRaw.trim();
			if (!trimmed.isEmpty()) {
				try {
					long parsedUserId = Long.parseLong(trimmed);
					if (parsedUserId != 0L) {
						w.eq(MemberOperateLog::getUserId, parsedUserId);
					}
				} catch (NumberFormatException ignored) {
					Map<String, Object> empty = new LinkedHashMap<>();
					empty.put("total_count", 0);
					empty.put("list", new ArrayList<>());
					return empty;
				}
			}
		}

		w.orderByDesc(MemberOperateLog::getCreated);

		Page<MemberOperateLog> page = new Page<>(1, 100);
		memberOperateLogMapper.selectPage(page, w);

		int totalCount = (int) Math.min(page.getTotal(), Integer.MAX_VALUE);

		List<Map<String, Object>> listRows = new ArrayList<>();
		for (MemberOperateLog entity : page.getRecords()) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", entity.getId());
			row.put("company_id", entity.getCompanyId());
			row.put("user_id", entity.getUserId());
			row.put("operate_type", entity.getOperateType());
			row.put("remarks", entity.getRemarks());
			row.put("old_data", entity.getOldData());
			row.put("new_data", entity.getNewData());
			row.put("operater", entity.getOperater());
			row.put("created", entity.getCreated());
			listRows.add(row);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("list", listRows);
		return result;
	}
}
