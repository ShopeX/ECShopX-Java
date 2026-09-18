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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.popularize.domain.TaskBrokerage;
import cn.shopex.ecshopx.popularize.mapper.TaskBrokerageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeTaskBrokerageLogsListReadService {

	private final TaskBrokerageMapper taskBrokerageMapper;
	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public PopularizeTaskBrokerageLogsListReadService(
			TaskBrokerageMapper taskBrokerageMapper,
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.taskBrokerageMapper = taskBrokerageMapper;
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> buildLogsListFilter(
			long companyId,
			Long resolvedUserId,
			String orderId,
			String itemName,
			String status,
			String timeStart,
			String timeEnd,
			String planDate) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		if (resolvedUserId != null) {
			filter.put("user_id", resolvedUserId);
		}
		if (PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterString(orderId)) {
			filter.put("order_id", orderId.trim());
		}
		if (PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterString(itemName)) {
			filter.put("item_name|contains", itemName.trim());
		}
		if (PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterString(status)) {
			filter.put("status", status.trim());
		}
		if (PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterObject(timeStart)
				&& PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterObject(timeEnd)) {
			try {
				int[] r =
						PopularizeTaskBrokerageCountListQueryService.parseUpdatedRange(
								timeStart.trim(), timeEnd.trim());
				filter.put("updated|gte", r[0]);
				filter.put("updated|lte", r[1]);
			} catch (Exception ex) {
				throw new BadRequestException("时间筛选格式错误");
			}
		}
		if (PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterString(planDate)) {
			try {
				filter.put(
						"plan_date",
						PopularizeTaskBrokerageCountListQueryService.endOfMonthYmd(planDate.trim()));
			} catch (Exception ex) {
				// invalid plan_date: do not add filter key
			}
		}
		return filter;
	}

	public Map<String, Object> getTaskBrokerageList(Map<String, Object> filter, int page, int pageSize) {
		LambdaQueryWrapper<TaskBrokerage> w = buildTaskBrokerageWrapper(filter);
		long totalCount = taskBrokerageMapper.selectCount(w);
		List<Map<String, Object>> list = new ArrayList<>();
		if (totalCount > 0) {
			Page<TaskBrokerage> pg = new Page<>(page, pageSize, false);
			Page<TaskBrokerage> result = taskBrokerageMapper.selectPage(pg, w);
			for (TaskBrokerage e : result.getRecords()) {
				list.add(entityToListRow(e));
			}
		}

		long companyId = longFrom(filter.get("company_id"));
		Set<Long> userIds = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			Object uid = row.get("user_id");
			if (uid instanceof Number n) {
				userIds.add(n.longValue());
			}
			Object buid = row.get("buy_user_id");
			if (buid instanceof Number n) {
				userIds.add(n.longValue());
			}
		}
		Map<Long, String> memberMobiles =
				userIds.isEmpty() ? Collections.emptyMap() : loadDecryptedMobilesByUserIds(companyId, userIds);
		for (Map<String, Object> row : list) {
			row.put("promoter_mobile", memberMobiles.getOrDefault(longFrom(row.get("user_id")), ""));
			row.put("buy_mobile", memberMobiles.getOrDefault(longFrom(row.get("buy_user_id")), ""));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", list);
		return data;
	}

	private LambdaQueryWrapper<TaskBrokerage> buildTaskBrokerageWrapper(Map<String, Object> filter) {
		LambdaQueryWrapper<TaskBrokerage> w = new LambdaQueryWrapper<>();
		w.eq(TaskBrokerage::getCompanyId, longFrom(filter.get("company_id")));
		Object userId = filter.get("user_id");
		if (userId != null) {
			w.eq(TaskBrokerage::getUserId, longFrom(userId));
		}
		Object orderId = filter.get("order_id");
		if (orderId != null && StringUtils.hasText(String.valueOf(orderId).trim())) {
			w.eq(TaskBrokerage::getOrderId, String.valueOf(orderId).trim());
		}
		Object itemNameContains = filter.get("item_name|contains");
		if (itemNameContains != null && StringUtils.hasText(String.valueOf(itemNameContains).trim())) {
			String trim = String.valueOf(itemNameContains).trim();
			w.like(TaskBrokerage::getItemName, "%" + trim + "%");
		}
		Object status = filter.get("status");
		if (status != null && StringUtils.hasText(String.valueOf(status).trim())) {
			w.eq(TaskBrokerage::getStatus, String.valueOf(status).trim());
		}
		Object gte = filter.get("updated|gte");
		if (gte != null) {
			w.ge(TaskBrokerage::getUpdated, (int) longFrom(gte));
		}
		Object lte = filter.get("updated|lte");
		if (lte != null) {
			w.le(TaskBrokerage::getUpdated, (int) longFrom(lte));
		}
		Object planDate = filter.get("plan_date");
		if (planDate != null && StringUtils.hasText(String.valueOf(planDate).trim())) {
			w.eq(TaskBrokerage::getPlanDate, String.valueOf(planDate).trim());
		}
		w.orderByDesc(TaskBrokerage::getCreated);
		return w;
	}

	private static Map<String, Object> entityToListRow(TaskBrokerage e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("item_id", e.getItemId());
		m.put("user_id", e.getUserId());
		m.put("order_id", e.getOrderId());
		m.put("buy_user_id", e.getBuyUserId());
		m.put("company_id", e.getCompanyId());
		m.put("item_name", e.getItemName());
		m.put("item_spec_desc", e.getItemSpecDesc());
		m.put("price", e.getPrice());
		m.put("num", e.getNum());
		m.put("status", e.getStatus());
		m.put("plan_date", e.getPlanDate());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}

	private Map<Long, String> loadDecryptedMobilesByUserIds(long companyId, Set<Long> userIds) {
		List<Members> mems =
				membersMapper.selectList(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.in(Members::getUserId, userIds));
		Map<Long, String> out = new HashMap<>();
		for (Members m : mems) {
			if (m.getUserId() == null) {
				continue;
			}
			String enc = m.getMobile();
			String plain = enc == null ? "" : sensitiveFieldEncryptor.decrypt(enc);
			out.put(m.getUserId(), plain == null ? "" : plain);
		}
		return out;
	}

	private static long longFrom(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
