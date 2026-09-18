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

package cn.shopex.ecshopx.kaquan.integration.members;

import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.integration.kaquan.AdminMemberListVipGradeRowGetPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("adminMemberListVipGradeRowGetPortImpl")
public class AdminMemberListVipGradeRowGetPortImpl implements AdminMemberListVipGradeRowGetPort {

	private static final String DEFAULT_VIP = "svip";

	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;

	public AdminMemberListVipGradeRowGetPortImpl(
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			VipGradeRelUserMapper vipGradeRelUserMapper) {
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
	}

	@Override
	public Map<String, Object> userVipGradeGet(long companyId, long userId, boolean ifAll) {
		return vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, ifAll);
	}

	@Override
	public void mergeVipGradeLabelsIntoRows(long companyId, List<Map<String, Object>> memberListRows) {
		if (memberListRows == null || memberListRows.isEmpty()) {
			return;
		}
		List<Long> userIds = new ArrayList<>();
		for (Map<String, Object> row : memberListRows) {
			Long uid = extractUserId(row.get("user_id"));
			if (uid != null && uid > 0L) {
				userIds.add(uid);
			}
		}
		Map<Long, String> labelByUserId = batchVipGradeLabels(companyId, userIds);
		for (Map<String, Object> row : memberListRows) {
			Long uid = extractUserId(row.get("user_id"));
			if (uid == null || uid <= 0L) {
				row.put("vip_grade", null);
				continue;
			}
			String label = labelByUserId.get(uid);
			if (label != null) {
				row.put("vip_grade", label);
			} else {
				row.remove("vip_grade");
			}
		}
	}

	/**
	 * Same label semantics as {@link VipGradeUserVipGradeGetService#userVipGradeGet(long, long, boolean)}
	 * with {@code ifAll=false} + list-row extraction: prefer {@code svip} when present among valid
	 * relations, otherwise the first relation's {@code vip_type}.
	 */
	private Map<Long, String> batchVipGradeLabels(long companyId, List<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}
		long now = System.currentTimeMillis() / 1000L;
		List<Long> distinctIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
		if (distinctIds.isEmpty()) {
			return Map.of();
		}
		List<VipGradeRelUser> relRows =
				vipGradeRelUserMapper.selectList(
						new LambdaQueryWrapper<VipGradeRelUser>()
								.eq(VipGradeRelUser::getCompanyId, (int) companyId)
								.in(VipGradeRelUser::getUserId, distinctIds)
								.apply("CAST(end_date AS UNSIGNED) > {0}", now));
		Map<Long, Map<String, String>> vipTypeByUser = new LinkedHashMap<>();
		for (VipGradeRelUser rel : relRows) {
			if (rel.getUserId() == null || !StringUtils.hasText(rel.getVipType())) {
				continue;
			}
			vipTypeByUser
					.computeIfAbsent(rel.getUserId(), k -> new LinkedHashMap<>())
					.putIfAbsent(rel.getVipType().trim(), rel.getVipType().trim());
		}
		Map<Long, String> out = new LinkedHashMap<>();
		for (Map.Entry<Long, Map<String, String>> e : vipTypeByUser.entrySet()) {
			Map<String, String> byType = e.getValue();
			if (byType.containsKey(DEFAULT_VIP)) {
				out.put(e.getKey(), DEFAULT_VIP);
			} else if (!byType.isEmpty()) {
				out.put(e.getKey(), byType.values().iterator().next());
			}
		}
		return out;
	}

	private static Long extractUserId(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
