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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EmployeeInviteListService {

	private final RelativesMapper relativesMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final EmployeePurchaseActivityDataService employeePurchaseActivityDataService;

	public EmployeeInviteListService(
			RelativesMapper relativesMapper,
			MembersInfoMapper membersInfoMapper,
			EmployeePurchaseActivityDataService employeePurchaseActivityDataService) {
		this.relativesMapper = relativesMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.employeePurchaseActivityDataService = employeePurchaseActivityDataService;
	}

	private static int parseInviteListQueryInt(String raw, int defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		if (raw.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public Map<String, Object> buildInviteList(
			long companyId,
			long inviterUserId,
			long activityId,
			long enterpriseId,
			String pageRaw,
			String pageSizeRaw) {
		int pageCandidate = parseInviteListQueryInt(pageRaw, 1);
		int pageSizeCandidate = parseInviteListQueryInt(pageSizeRaw, 20);
		int pageEff = pageCandidate < 1 ? 1 : pageCandidate;

		employeePurchaseActivityDataService.assertActivityExistsAndEnterpriseParticipates(
				companyId, activityId, enterpriseId);

		var w =
				Wrappers.<Relatives>lambdaQuery()
						.eq(Relatives::getCompanyId, companyId)
						.eq(Relatives::getEnterpriseId, enterpriseId)
						.eq(Relatives::getEmployeeUserId, inviterUserId)
						.eq(Relatives::getActivityId, activityId)
						.orderByDesc(Relatives::getCreated);

		long totalCount;
		List<Relatives> records;
		if (pageSizeCandidate > 0) {
			Long c = relativesMapper.selectCount(w);
			totalCount = c == null ? 0L : c.longValue();
			Page<Relatives> p = new Page<>(pageEff, pageSizeCandidate, false);
			relativesMapper.selectPage(p, w);
			records = p.getRecords();
		} else {
			Long c = relativesMapper.selectCount(w);
			totalCount = c == null ? 0L : c.longValue();
			records = relativesMapper.selectList(w);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		if (records == null || records.isEmpty()) {
			out.put("list", List.of());
			return out;
		}

		Set<Long> idSet = new LinkedHashSet<>();
		for (Relatives r : records) {
			if (r.getUserId() != null) {
				idSet.add(r.getUserId());
			}
		}
		List<Long> userIds = new ArrayList<>(idSet);

		Map<Long, MembersInfo> memberMap = new LinkedHashMap<>();
		if (!userIds.isEmpty()) {
			List<MembersInfo> infos =
					membersInfoMapper.selectList(
							Wrappers.<MembersInfo>lambdaQuery()
									.eq(MembersInfo::getCompanyId, companyId)
									.in(MembersInfo::getUserId, userIds));
			for (MembersInfo mi : infos) {
				if (mi.getUserId() != null) {
					memberMap.put(mi.getUserId(), mi);
				}
			}
		}

		List<Map<String, Object>> list = new ArrayList<>(records.size());
		for (Relatives row : records) {
			list.add(enrichRow(companyId, enterpriseId, activityId, row, memberMap));
		}
		out.put("list", list);
		return out;
	}

	private Map<String, Object> enrichRow(
			long companyId,
			long enterpriseId,
			long activityId,
			Relatives row,
			Map<Long, MembersInfo> memberMap) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("distributor_id", row.getDistributorId() == null ? 0 : row.getDistributorId());
		m.put("enterprise_id", row.getEnterpriseId());
		m.put("user_id", row.getUserId());
		m.put("member_mobile", row.getMemberMobile());
		m.put("activity_id", row.getActivityId());
		m.put("employee_id", row.getEmployeeId() == null ? 0L : row.getEmployeeId());
		m.put("employee_user_id", row.getEmployeeUserId());
		m.put("created", row.getCreated());
		m.put("disabled", booleanDisabledToInt(row.getDisabled()));

		Long uid = row.getUserId();
		if (uid == null || !memberMap.containsKey(uid)) {
			return m;
		}
		MembersInfo info = memberMap.get(uid);
		m.put("username", info.getUsername() == null ? "" : info.getUsername());
		m.put("avatar", info.getAvatar() == null ? "" : info.getAvatar());
		try {
			Map<String, Object> fee =
					employeePurchaseActivityDataService.getAggregateFeeForInvitee(
							companyId, enterpriseId, activityId, uid);
			m.put("limit_fee", numberToInt(fee.get("limit_fee"), 0));
			m.put("used_limitfee", numberToLong(fee.get("aggregate_fee"), 0L));
			m.put("left_fee", numberToInt(fee.get("left_fee"), 0));
		} catch (Exception e) {
			m.put("limit_fee", 0);
			m.put("used_limitfee", 0L);
			m.put("left_fee", 0);
		}
		return m;
	}

	private static int booleanDisabledToInt(Boolean disabled) {
		if (disabled == null || !disabled) {
			return 0;
		}
		return 1;
	}

	private static int numberToInt(Object o, int def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static long numberToLong(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
