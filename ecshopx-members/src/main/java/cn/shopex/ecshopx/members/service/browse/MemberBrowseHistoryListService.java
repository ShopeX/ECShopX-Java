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

package cn.shopex.ecshopx.members.service.browse;

import cn.shopex.ecshopx.common.members.port.MemberBrowseHistoryItemLookupPort;
import cn.shopex.ecshopx.members.domain.MemberBrowseHistory;
import cn.shopex.ecshopx.members.mapper.MemberBrowseHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberBrowseHistoryListService {

	private final MemberBrowseHistoryMapper memberBrowseHistoryMapper;

	private final MemberBrowseHistoryItemLookupPort itemLookupPort;

	public Map<String, Object> getBrowseHistory(
			long companyId, long userId, int page, int pageSize, String languageTagForItems) {
		return getBrowseHistory(companyId, (Object) userId, page, pageSize, languageTagForItems);
	}

	public Map<String, Object> getBrowseHistory(
			long companyId, Object userId, int page, int pageSize, String languageTagForItems) {
		LambdaQueryWrapper<MemberBrowseHistory> w = new LambdaQueryWrapper<>();
		w.eq(MemberBrowseHistory::getCompanyId, companyId)
				.eq(MemberBrowseHistory::getUserId, userId)
				.orderByDesc(MemberBrowseHistory::getUpdated);
		long totalCount = memberBrowseHistoryMapper.selectCount(w);
		Page<MemberBrowseHistory> p = new Page<>(page, pageSize, false);
		memberBrowseHistoryMapper.selectPage(p, w);
		List<MemberBrowseHistory> records = p.getRecords();

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		List<Map<String, Object>> list = new ArrayList<>();
		if (records.isEmpty()) {
			result.put("list", list);
			return result;
		}
		List<Long> idOrder = new ArrayList<>();
		for (MemberBrowseHistory history : records) {
			Long iid = history.getItemId();
			if (iid != null && iid > 0L) {
				idOrder.add(iid);
			}
		}
		Map<Long, Map<String, Object>> itemById =
				itemLookupPort.listItemRowsForBrowseHistory(idOrder, companyId, languageTagForItems);
		for (MemberBrowseHistory history : records) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			Long hid = history.getHistoryId();
			row.put("history_id", hid != null ? hid : 0L);
			row.put("company_id", history.getCompanyId());
			row.put("user_id", history.getUserId());
			row.put("item_id", history.getItemId());
			row.put("created", history.getCreated());
			row.put("updated", history.getUpdated());
			Map<String, Object> itemRow = itemById.get(history.getItemId());
			row.put("itemData", itemRow != null ? itemRow : Collections.emptyList());
			list.add(row);
		}
		result.put("list", list);
		return result;
	}

	public List<MemberBrowseHistory> listHistoryPage(
			long companyId, Object userId, int page, Integer pageSize) {
		LambdaQueryWrapper<MemberBrowseHistory> w = new LambdaQueryWrapper<>();
		w.eq(MemberBrowseHistory::getCompanyId, companyId)
				.eq(MemberBrowseHistory::getUserId, userId)
				.orderByDesc(MemberBrowseHistory::getUpdated);
		if (pageSize != null && pageSize > 0) {
			long offset = ((long) page - 1L) * (long) pageSize.intValue();
			w.last("LIMIT " + pageSize + " OFFSET " + offset);
		}
		return memberBrowseHistoryMapper.selectList(w);
	}
}
