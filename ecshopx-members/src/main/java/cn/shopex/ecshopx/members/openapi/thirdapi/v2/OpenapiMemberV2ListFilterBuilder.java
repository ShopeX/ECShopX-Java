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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.kaquan.port.OpenapiMemberListKaquanLookupPort;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.common.salesperson.port.OpenapiShopSalespersonOpenapiBriefPort;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.admin.dto.OpenapiMemberV2ListQueryFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiMemberV2ListFilterBuilder {

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final MembersMapper membersMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final OpenapiShopSalespersonOpenapiBriefPort shopSalespersonOpenapiBriefPort;
	private final OpenapiMemberListKaquanLookupPort memberListKaquanLookupPort;

	public OpenapiMemberV2ListFilterBuilder(
			MembersMapper membersMapper,
			MemberTagsMapper memberTagsMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			OpenapiShopSalespersonOpenapiBriefPort shopSalespersonOpenapiBriefPort,
			OpenapiMemberListKaquanLookupPort memberListKaquanLookupPort) {
		this.membersMapper = membersMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.shopSalespersonOpenapiBriefPort = shopSalespersonOpenapiBriefPort;
		this.memberListKaquanLookupPort = memberListKaquanLookupPort;
	}

	public record BuildResult(OpenapiMemberV2ListQueryFilter filter, MemberV2ListContext context) {}

	public BuildResult build(long companyId, Map<String, Object> rawParams) {
		OpenapiMemberV2ListQueryFilter filter = new OpenapiMemberV2ListQueryFilter();
		filter.setCompanyId(companyId);
		MemberV2ListContext context = new MemberV2ListContext();

		if (rawParams.containsKey("have_consume")) {
			filter.setHaveConsumeEq(parseInt(rawParams.get("have_consume")));
		}
		if (rawParams.containsKey("mobile")) {
			String mobile = stringValue(rawParams.get("mobile"));
			if (StringUtils.hasText(mobile)) {
				filter.setMobileEq(sensitiveFieldEncryptor.encrypt(mobile.trim()));
			}
		}
		if (rawParams.containsKey("source_from")) {
			filter.setSourceFromEq(stringValue(rawParams.get("source_from")));
		}
		if (rawParams.containsKey("status")) {
			filter.setDisabledEq(mapStatusToDisabled(parseInt(rawParams.get("status"))));
		}
		if (rawParams.containsKey("card_code")) {
			filter.setCardCodeEq(stringValue(rawParams.get("card_code")));
		}

		if (rawParams.containsKey("start_date")) {
			String start = stringValue(rawParams.get("start_date"));
			if (StringUtils.hasText(start)) {
				filter.setCreatedStart(parseDateTimeToEpoch(start));
			}
		}
		if (rawParams.containsKey("end_date")) {
			String end = stringValue(rawParams.get("end_date"));
			if (StringUtils.hasText(end)) {
				filter.setCreatedEnd(parseDateTimeToEpoch(end));
			}
		}

		if (rawParams.containsKey("inviter_mobile")) {
			resolveInviterFilter(companyId, stringValue(rawParams.get("inviter_mobile")), filter, context);
		}
		if (rawParams.containsKey("salesperson_mobile")) {
			resolveSalespersonFilter(companyId, stringValue(rawParams.get("salesperson_mobile")), filter, context);
		}
		if (rawParams.containsKey("tag_id")) {
			filter.setTagIdGt((long) parseInt(rawParams.get("tag_id")));
		} else if (rawParams.containsKey("tag_name")) {
			filter.setTagIdGt(resolveTagIdByName(companyId, stringValue(rawParams.get("tag_name"))));
		}
		if (rawParams.containsKey("grade_id")) {
			filter.setGradeIdEq((long) parseInt(rawParams.get("grade_id")));
		} else if (rawParams.containsKey("grade_name")) {
			long gradeId = memberListKaquanLookupPort.resolveGradeIdByName(companyId, stringValue(rawParams.get("grade_name")));
			filter.setGradeIdEq(gradeId);
		}
		if (rawParams.containsKey("vip_grade_id")) {
			filter.setVipGradeIdGt((long) parseInt(rawParams.get("vip_grade_id")));
		} else if (rawParams.containsKey("vip_grade_name")) {
			filter.setVipGradeIdGt(memberListKaquanLookupPort.resolveVipGradeIdByName(
					companyId, stringValue(rawParams.get("vip_grade_name"))));
		}

		unsetEmptyStringFields(filter);
		return new BuildResult(filter, context);
	}

	private void resolveInviterFilter(
			long companyId, String mobilePlain, OpenapiMemberV2ListQueryFilter filter, MemberV2ListContext context) {
		if (!StringUtils.hasText(mobilePlain)) {
			return;
		}
		String mobileEnc = sensitiveFieldEncryptor.encrypt(mobilePlain.trim());
		List<Map<String, Object>> rows =
				membersMapper.selectOpenapiMemberBriefRowsByCompanyAndMobileEnc(companyId, mobileEnc);
		if (rows.isEmpty()) {
			return;
		}
		Map<Long, Map<String, Object>> inviterByUserId = new LinkedHashMap<>();
		List<Long> inviterIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Long userId = longValue(row.get("user_id"));
			if (userId == null) {
				continue;
			}
			inviterIds.add(userId);
			inviterByUserId.put(userId, row);
		}
		if (!inviterIds.isEmpty()) {
			context.setInviterByUserId(inviterByUserId);
			filter.setInviterIdIn(inviterIds);
		}
	}

	private void resolveSalespersonFilter(
			long companyId, String mobilePlain, OpenapiMemberV2ListQueryFilter filter, MemberV2ListContext context) {
		Map<String, Object> salespersonInfo =
				shopSalespersonOpenapiBriefPort.findByMobile(companyId, mobilePlain != null ? mobilePlain : "");
		long salespersonId = 0L;
		if (salespersonInfo != null && salespersonInfo.get("salesperson_id") != null) {
			salespersonId = longValue(salespersonInfo.get("salesperson_id"));
		}
		filter.setSalespersonIdGt(salespersonId);
		context.getSalespersonByUserId().put(0L, salespersonInfo != null ? salespersonInfo : Map.of());
		context.setBySalespersonFilter(true);
	}

	private long resolveTagIdByName(long companyId, String tagName) {
		if (!StringUtils.hasText(tagName)) {
			return 0L;
		}
		MemberTags tag =
				memberTagsMapper.selectOne(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.eq(MemberTags::getTagName, tagName.trim())
								.last("LIMIT 1"));
		return tag == null || tag.getTagId() == null ? 0L : tag.getTagId();
	}

	private static void unsetEmptyStringFields(OpenapiMemberV2ListQueryFilter filter) {
		if ("".equals(filter.getMobileEq())) {
			filter.setMobileEq(null);
		}
		if ("".equals(filter.getSourceFromEq())) {
			filter.setSourceFromEq(null);
		}
		if ("".equals(filter.getCardCodeEq())) {
			filter.setCardCodeEq(null);
		}
	}

	private static Integer mapStatusToDisabled(int status) {
		return switch (status) {
			case 0 -> 1;
			case 1 -> 0;
			default -> status;
		};
	}

	private static long parseDateTimeToEpoch(String raw) {
		String t = raw.trim();
		if (t.matches("\\d+")) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.VALIDATION_TIMESTAMP_ERROR, "时间格式有误");
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(t, DATETIME_FMT);
			return ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException e) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.VALIDATION_TIMESTAMP_ERROR, "时间格式有误");
		}
	}

	private static int parseInt(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			if (s.contains(".")) {
				return (int) Double.parseDouble(s);
			}
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringValue(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		return String.valueOf(raw);
	}

	private static Long longValue(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
