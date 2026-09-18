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
import cn.shopex.ecshopx.common.salesperson.port.OpenapiShopSalespersonOpenapiBriefPort;
import cn.shopex.ecshopx.members.domain.MemberUserRelTagRow;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberV2ListEnrichmentService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MembersMapper membersMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final OpenapiShopSalespersonOpenapiBriefPort shopSalespersonOpenapiBriefPort;
	private final OpenapiMemberListKaquanLookupPort memberListKaquanLookupPort;
	private final ObjectMapper objectMapper;

	public OpenapiMemberV2ListEnrichmentService(
			MembersMapper membersMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			WorkWechatRelMapper workWechatRelMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			OpenapiShopSalespersonOpenapiBriefPort shopSalespersonOpenapiBriefPort,
			OpenapiMemberListKaquanLookupPort memberListKaquanLookupPort,
			ObjectMapper objectMapper) {
		this.membersMapper = membersMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.workWechatRelMapper = workWechatRelMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.shopSalespersonOpenapiBriefPort = shopSalespersonOpenapiBriefPort;
		this.memberListKaquanLookupPort = memberListKaquanLookupPort;
		this.objectMapper = objectMapper;
	}

	public void enrichList(long companyId, List<Map<String, Object>> list, MemberV2ListContext context) {
		appendInviterToList(companyId, list, context);
		appendSalespersonToList(companyId, list, context);
		appendTagToList(companyId, list);
		appendVipGradeToList(companyId, list);
		handleDataToList(list);
	}

	public void enrichDetailRaw(long companyId, List<Map<String, Object>> list) {
		MemberV2ListContext context = new MemberV2ListContext();
		appendInviterToList(companyId, list, context);
		appendSalespersonToList(companyId, list, context);
		appendTagToList(companyId, list);
		appendVipGradeToList(companyId, list);
	}

	private void appendInviterToList(long companyId, List<Map<String, Object>> list, MemberV2ListContext context) {
		if (context.getInviterByUserId() == null) {
			List<Long> inviterIds = new ArrayList<>();
			for (Map<String, Object> row : list) {
				Long inviterId = longValue(row.get("inviter_id"));
				if (inviterId != null && inviterId > 0) {
					inviterIds.add(inviterId);
				}
			}
			if (!inviterIds.isEmpty()) {
				List<Map<String, Object>> rows =
						membersMapper.selectOpenapiInviterBriefByUserIds(companyId, inviterIds);
				Map<Long, Map<String, Object>> inviterByUserId = new LinkedHashMap<>();
				for (Map<String, Object> row : rows) {
					Long userId = longValue(row.get("user_id"));
					if (userId != null) {
						decryptBriefRow(row);
						inviterByUserId.put(userId, row);
					}
				}
				context.setInviterByUserId(inviterByUserId);
			}
		} else {
			for (Map<String, Object> row : context.getInviterByUserId().values()) {
				decryptBriefRow(row);
			}
		}

		Map<Long, Map<String, Object>> inviterMap =
				context.getInviterByUserId() != null ? context.getInviterByUserId() : Map.of();
		for (Map<String, Object> item : list) {
			long inviterId = longValueOrZero(item.get("inviter_id"));
			Map<String, Object> inviterRow = inviterMap.get(inviterId);
			Map<String, Object> inviter = new LinkedHashMap<>();
			inviter.put("id", inviterId);
			inviter.put("name", inviterRow != null ? stringOrEmpty(inviterRow.get("username")) : "");
			inviter.put("mobile", inviterRow != null ? stringOrEmpty(inviterRow.get("mobile")) : "");
			item.put("inviter", inviter);
			item.remove("inviter_id");
		}
	}

	private void appendSalespersonToList(long companyId, List<Map<String, Object>> list, MemberV2ListContext context) {
		if (!context.isBySalespersonFilter()) {
			List<Long> userIds = collectUserIds(list);
			if (!userIds.isEmpty()) {
				List<WorkWechatRel> relRows =
						workWechatRelMapper.selectList(
								new LambdaQueryWrapper<WorkWechatRel>()
										.eq(WorkWechatRel::getCompanyId, companyId)
										.in(WorkWechatRel::getUserId, userIds)
										.eq(WorkWechatRel::getIsBind, true)
										.select(WorkWechatRel::getUserId, WorkWechatRel::getSalespersonId));
				Map<Long, Long> salespersonIdByUserId = new LinkedHashMap<>();
				List<Long> salespersonIds = new ArrayList<>();
				for (WorkWechatRel rel : relRows) {
					if (rel.getUserId() == null || rel.getSalespersonId() == null || rel.getSalespersonId() <= 0) {
						continue;
					}
					salespersonIdByUserId.put(rel.getUserId(), rel.getSalespersonId());
					salespersonIds.add(rel.getSalespersonId());
				}
				if (!salespersonIds.isEmpty()) {
					Map<Long, Map<String, Object>> briefById =
							shopSalespersonOpenapiBriefPort.listBySalespersonIds(companyId, salespersonIds);
					for (Map.Entry<Long, Long> entry : salespersonIdByUserId.entrySet()) {
						Map<String, Object> brief = briefById.get(entry.getValue());
						if (brief != null) {
							context.getSalespersonByUserId().put(entry.getKey(), brief);
						}
					}
				}
			}
		}

		for (Map<String, Object> item : list) {
			long lookupUserId = context.isBySalespersonFilter() ? 0L : longValueOrZero(item.get("user_id"));
			Map<String, Object> salespersonRow = context.getSalespersonByUserId().get(lookupUserId);
			Map<String, Object> salesperson = new LinkedHashMap<>();
			salesperson.put(
					"id",
					salespersonRow != null ? longValueOrZero(salespersonRow.get("salesperson_id")) : 0L);
			salesperson.put("name", salespersonRow != null ? stringOrEmpty(salespersonRow.get("name")) : "");
			salesperson.put("mobile", salespersonRow != null ? stringOrEmpty(salespersonRow.get("mobile")) : "");
			item.put("salesperson", salesperson);
			item.remove("salesperson_id");
		}
	}

	private void appendTagToList(long companyId, List<Map<String, Object>> list) {
		List<Long> userIds = collectUserIds(list);
		Map<Long, List<Map<String, Object>>> tagsByUserId = new LinkedHashMap<>();
		if (!userIds.isEmpty()) {
			List<MemberUserRelTagRow> relTags =
					memberRelTagsMapper.selectUserRelTagListByUserIds(companyId, userIds);
			for (MemberUserRelTagRow relTag : relTags) {
				Long userId = relTag.getUserId();
				if (userId == null) {
					continue;
				}
				tagsByUserId.computeIfAbsent(userId, k -> new ArrayList<>())
						.add(Map.of("id", relTag.getTagId() != null ? relTag.getTagId() : 0L, "name", stringOrEmpty(relTag.getTagName())));
			}
		}
		for (Map<String, Object> item : list) {
			long userId = longValueOrZero(item.get("user_id"));
			item.put("tags", tagsByUserId.getOrDefault(userId, List.of()));
		}
	}

	private void appendVipGradeToList(long companyId, List<Map<String, Object>> list) {
		List<Long> userIds = collectUserIds(list);
		Map<Long, List<Map<String, Object>>> vipGradesByUserId =
				userIds.isEmpty()
						? Map.of()
						: memberListKaquanLookupPort.loadActiveVipGradesByUserIds(companyId, userIds);
		for (Map<String, Object> item : list) {
			long userId = longValueOrZero(item.get("user_id"));
			item.put("vip_grades", vipGradesByUserId.getOrDefault(userId, List.of()));
		}
	}

	private void handleDataToList(List<Map<String, Object>> list) {
		for (Map<String, Object> data : list) {
			if (data.containsKey("created")) {
				data.put("created", formatEpochSeconds(data.get("created")));
			}
			if (data.containsKey("updated")) {
				data.put("updated", formatEpochSeconds(data.get("updated")));
			}
			if (data.containsKey("habbit")) {
				data.put("habbit", parseHabbit(data.get("habbit")));
			}
			if (data.containsKey("disabled")) {
				int disabled = intValue(data.get("disabled"));
				Integer status =
						switch (disabled) {
							case 0 -> 1;
							case 1 -> 0;
							default -> null;
						};
				data.put("status", status);
				data.remove("disabled");
			}
			OpenapiMemberV2EnumNormalize.normalizeListRow(data);
		}
	}

	private List<Map<String, Object>> parseHabbit(Object raw) {
		if (raw == null) {
			return List.of();
		}
		try {
			List<Map<String, Object>> items;
			if (raw instanceof String s) {
				items = objectMapper.readValue(s, new TypeReference<>() {});
			} else if (raw instanceof List<?> list) {
				items = objectMapper.convertValue(list, new TypeReference<>() {});
			} else {
				return List.of();
			}
			for (Map<String, Object> item : items) {
				Object ischecked = item.get("ischecked");
				if ("true".equals(ischecked)) {
					item.put("ischecked", true);
				} else if ("false".equals(ischecked)) {
					item.put("ischecked", false);
				}
			}
			return items;
		} catch (Exception e) {
			return List.of();
		}
	}

	private void decryptBriefRow(Map<String, Object> row) {
		Object mobileEnc = row.get("mobile");
		if (mobileEnc != null) {
			row.put("mobile", sensitiveFieldEncryptor.decrypt(String.valueOf(mobileEnc)));
		}
		Object usernameEnc = row.get("username");
		if (usernameEnc != null) {
			row.put("username", sensitiveFieldEncryptor.decrypt(String.valueOf(usernameEnc)));
		}
	}

	private static List<Long> collectUserIds(List<Map<String, Object>> list) {
		List<Long> userIds = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Long userId = longValue(row.get("user_id"));
			if (userId != null) {
				userIds.add(userId);
			}
		}
		return userIds;
	}

	private static String formatEpochSeconds(Object epochObj) {
		Long epoch = longValue(epochObj);
		if (epoch == null) {
			return null;
		}
		return Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).format(DATETIME_FMT);
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

	private static long longValueOrZero(Object raw) {
		Long v = longValue(raw);
		return v == null ? 0L : v;
	}

	private static int intValue(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringOrEmpty(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}
}
