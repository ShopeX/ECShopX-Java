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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.common.wechat.WorkWechatRelMemberBatchPort;
import cn.shopex.ecshopx.common.wechat.WorkWechatRelSalespersonBatchPort;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WorkWechatRelListService {

	private static final Logger log = LoggerFactory.getLogger(WorkWechatRelListService.class);

	private final WorkWechatRelMapper workWechatRelMapper;
	private final WorkWechatRelSalespersonBatchPort salespersonBatchPort;
	private final WorkWechatRelMemberBatchPort memberBatchPort;

	public WorkWechatRelListService(WorkWechatRelMapper workWechatRelMapper,
			WorkWechatRelSalespersonBatchPort salespersonBatchPort,
			WorkWechatRelMemberBatchPort memberBatchPort) {
		this.workWechatRelMapper = workWechatRelMapper;
		this.salespersonBatchPort = salespersonBatchPort;
		this.memberBatchPort = memberBatchPort;
	}

	public Map<String, Object> getWorkWechatRelList(String salespersonIdRaw, String pageRaw, String pageSizeRaw,
			String isFriendRaw, String isBindRaw) {
		long salespersonIdLong = parseLongOrZero(salespersonIdRaw);
		int page = parseIntStrictOrZero(pageRaw);
		int pageSize = parseIntStrictOrZero(pageSizeRaw);

		LambdaQueryWrapper<WorkWechatRel> wrapper = buildFilterWrapper(salespersonIdLong, isFriendRaw, isBindRaw);
		Long totalCount = workWechatRelMapper.selectCount(wrapper);
		long total = totalCount == null ? 0L : totalCount;
		if (total == 0L) {
			return Map.of("total_count", 0L, "list", List.of());
		}

		List<WorkWechatRel> relRows;
		if (pageSize > 0) {
			LambdaQueryWrapper<WorkWechatRel> pageWrapper = buildFilterWrapper(salespersonIdLong, isFriendRaw,
					isBindRaw);
			pageWrapper.orderByDesc(WorkWechatRel::getId);
			Page<WorkWechatRel> p = new Page<>(page, pageSize, false);
			relRows = workWechatRelMapper.selectPage(p, pageWrapper).getRecords();
		} else {
			LambdaQueryWrapper<WorkWechatRel> listWrapper = buildFilterWrapper(salespersonIdLong, isFriendRaw,
					isBindRaw);
			listWrapper.orderByDesc(WorkWechatRel::getId);
			relRows = workWechatRelMapper.selectList(listWrapper);
		}

		List<Long> spIds = new ArrayList<>();
		List<Long> userIds = new ArrayList<>();
		for (WorkWechatRel r : relRows) {
			if (r.getSalespersonId() != null) {
				spIds.add(r.getSalespersonId());
			}
			if (r.getUserId() != null) {
				userIds.add(r.getUserId());
			}
		}

		Map<Long, Map<String, Object>> salespersonMap = salespersonBatchPort.loadBySalespersonIds(spIds);
		Map<Long, Map<String, Object>> userMap = memberBatchPort.loadByUserIdsForWorkWechatRel(userIds);

		log.info("work_wechat_rel list size={}, salesperson_keys={}, user_keys={}", relRows.size(),
				salespersonMap.size(), userMap.size());

		List<Map<String, Object>> listOut = new ArrayList<>();
		for (WorkWechatRel r : relRows) {
			listOut.add(toRow(r, salespersonMap, userMap));
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", total);
		body.put("list", listOut);
		return body;
	}

	private static LambdaQueryWrapper<WorkWechatRel> buildFilterWrapper(long salespersonIdLong, String isFriendRaw,
			String isBindRaw) {
		LambdaQueryWrapper<WorkWechatRel> w = new LambdaQueryWrapper<>();
		w.eq(WorkWechatRel::getSalespersonId, salespersonIdLong);
		if (queryParamTruthy(isFriendRaw)) {
			int v = parseIntStrictOrZero(isFriendRaw);
			w.apply("is_friend = {0}", v);
		}
		if (queryParamTruthy(isBindRaw)) {
			int v = parseIntStrictOrZero(isBindRaw);
			w.apply("is_bind = {0}", v);
		}
		return w;
	}

	private static Map<String, Object> toRow(WorkWechatRel r, Map<Long, Map<String, Object>> salespersonMap,
			Map<Long, Map<String, Object>> userMap) {
		long sid = r.getSalespersonId() == null ? 0L : r.getSalespersonId();
		long uid = r.getUserId() == null ? 0L : r.getUserId();
		Map<String, Object> spInfo = salespersonMap.get(sid);

		Object salespersonInfo = (spInfo == null || spInfo.isEmpty()) ? Collections.emptyList() : spInfo;
		Map<String, Object> uInfo = userMap.get(uid);
		Object userInfo = (uInfo == null || uInfo.isEmpty()) ? Collections.emptyList() : uInfo;

		String workUserid = "";
		if (spInfo != null && spInfo.get("work_userid") != null) {
			workUserid = String.valueOf(spInfo.get("work_userid"));
		}

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", r.getId());
		row.put("company_id", r.getCompanyId());
		row.put("work_userid", workUserid);
		row.put("salesperson_id", r.getSalespersonId());
		row.put("external_userid", r.getExternalUserid() == null ? "" : r.getExternalUserid());
		row.put("unionid", r.getUnionid() == null ? "" : r.getUnionid());
		row.put("user_id", r.getUserId());
		row.put("is_friend", Boolean.TRUE.equals(r.getIsFriend()) ? 1 : 0);
		row.put("is_bind", Boolean.TRUE.equals(r.getIsBind()) ? 1 : 0);
		row.put("bound_time", r.getBoundTime());
		row.put("add_friend_time", r.getAddFriendTime());
		row.put("user_info", userInfo);
		row.put("salesperson_info", salespersonInfo);
		return row;
	}

	private static boolean queryParamTruthy(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return false;
		}
		return true;
	}

	private static int parseIntStrictOrZero(String raw) {
		if (raw == null) {
			return 0;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long parseLongOrZero(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
