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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchCouponGivePort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchGroupSmsPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchVipDelayPort;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.MemberRelTagsBatchCreateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberBatchOperatingChunkExecutor {

	private final AdminMemberBatchOperatingUserIdsPageService adminMemberBatchOperatingUserIdsPageService;
	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;
	private final AdminMemberBatchCouponGivePort adminMemberBatchCouponGivePort;
	private final AdminMemberBatchGroupSmsPort adminMemberBatchGroupSmsPort;
	private final AdminMemberBatchVipDelayPort adminMemberBatchVipDelayPort;
	private final AdminMemberBatchUpdateGradeService adminMemberBatchUpdateGradeService;
	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AdminMemberBatchOperatingChunkExecutor(
			AdminMemberBatchOperatingUserIdsPageService adminMemberBatchOperatingUserIdsPageService,
			MemberRelTagsBatchCreateService memberRelTagsBatchCreateService,
			@Qualifier("adminMemberBatchCouponGivePortImpl") AdminMemberBatchCouponGivePort adminMemberBatchCouponGivePort,
			@Qualifier("adminMemberBatchGroupSmsPortImpl") AdminMemberBatchGroupSmsPort adminMemberBatchGroupSmsPort,
			@Qualifier("adminMemberBatchVipDelayPortImpl") AdminMemberBatchVipDelayPort adminMemberBatchVipDelayPort,
			AdminMemberBatchUpdateGradeService adminMemberBatchUpdateGradeService,
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.adminMemberBatchOperatingUserIdsPageService = adminMemberBatchOperatingUserIdsPageService;
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
		this.adminMemberBatchCouponGivePort = adminMemberBatchCouponGivePort;
		this.adminMemberBatchGroupSmsPort = adminMemberBatchGroupSmsPort;
		this.adminMemberBatchVipDelayPort = adminMemberBatchVipDelayPort;
		this.adminMemberBatchUpdateGradeService = adminMemberBatchUpdateGradeService;
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public void executeChunk(
			long companyId,
			Map<String, Object> operatorParams,
			String actionType,
			Map<String, Object> chunkFilter,
			int page,
			int pageSize) {
		List<Long> list =
				adminMemberBatchOperatingUserIdsPageService.listUserIdsPage(
						companyId, chunkFilter, page, pageSize);
		if (list.isEmpty()) {
			return;
		}
		if (actionType == null) {
			return;
		}
		switch (actionType) {
			case "rel_tag" -> {
				List<Long> tagIds = parseTagIds(operatorParams.get("tag_ids"));
				memberRelTagsBatchCreateService.createRelTags(list, tagIds, companyId);
			}
			case "give_coupon" -> {
				long distributorId = parseLong(operatorParams.get("distributor_id"), 0L);
				List<Long> couponIds = parseLongListFlexible(operatorParams.get("couponsids"));
				String sender = Objects.toString(operatorParams.get("sender"), "");
				adminMemberBatchCouponGivePort.giveCoupons(
						companyId, distributorId, sender, list, couponIds, "商城后台发放");
			}
			case "send_sms" -> {
				long distributorId = parseLong(operatorParams.get("distributor_id"), 0L);
				String sender = Objects.toString(operatorParams.get("sender"), "");
				String smsContent = Objects.toString(operatorParams.get("sms_content"), "");
				adminMemberBatchGroupSmsPort.enqueue(companyId, sender, distributorId, list, smsContent);
			}
			case "vip_delay" -> {
				long vipGradeId = parseLong(operatorParams.get("vip_grade_id"), 0L);
				int addDay = (int) parseLong(operatorParams.get("add_day"), 0L);
				Map<Long, String> mobiles = loadPlainMobilesByUserIds(companyId, list);
				adminMemberBatchVipDelayPort.applyVipDelayForUserChunk(companyId, vipGradeId, addDay, mobiles);
			}
			case "set_grade" -> {
				Map<?, ?> jwtClaims = (Map<?, ?>) operatorParams.get("__batch_operator_jwt__");
				if (jwtClaims == null) {
					throw new ResourceException("未登录");
				}
				long gradeId = parseLong(operatorParams.get("grade_id"), 0L);
				String remarks = Objects.toString(operatorParams.get("remarks"), "");
				Map<String, Object> merged = new LinkedHashMap<>();
				List<Map<String, Object>> userIdsPayload = new ArrayList<>();
				for (Long uid : list) {
					Map<String, Object> one = new LinkedHashMap<>();
					one.put("user_id", uid);
					userIdsPayload.add(one);
				}
				merged.put("user_ids", userIdsPayload);
				merged.put("grade_id", gradeId);
				merged.put("remarks", remarks);
				adminMemberBatchUpdateGradeService.updateGrade(companyId, jwtClaims, merged);
			}
			default -> {
			}
		}
	}

	private Map<Long, String> loadPlainMobilesByUserIds(long companyId, List<Long> userIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (userIds.isEmpty()) {
			return out;
		}
		List<Members> rows =
				membersMapper.selectList(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.in(Members::getUserId, userIds));
		for (Members m : rows) {
			String plain = "";
			if (m.getMobile() != null) {
				String d = sensitiveFieldEncryptor.decrypt(m.getMobile());
				if (StringUtils.hasText(d)) {
					plain = d;
				}
			}
			if (!StringUtils.hasText(plain) && m.getRegionMobile() != null) {
				plain = m.getRegionMobile();
			}
			out.put(m.getUserId(), plain);
		}
		return out;
	}

	private static List<Long> parseTagIds(Object raw) {
		return parseLongListFlexible(raw);
	}

	static List<Long> parseLongListFlexible(Object raw) {
		List<Long> out = new ArrayList<>();
		if (raw == null) {
			return out;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return out;
			}
			for (String p : t.split(",")) {
				String x = p.trim();
				if (x.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(x));
				} catch (NumberFormatException ignored) {
				}
			}
			return out;
		}
		if (raw instanceof Iterable<?> it) {
			for (Object el : it) {
				if (el == null) {
					continue;
				}
				try {
					out.add(Long.parseLong(String.valueOf(el).trim()));
				} catch (NumberFormatException ignored) {
				}
			}
			return out;
		}
		if (raw instanceof Number n) {
			out.add(n.longValue());
		}
		return out;
	}

	private static long parseLong(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
