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

package cn.shopex.ecshopx.wsugc.service.topic;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TopicDetailService {

	private static final DateTimeFormatter CREATED_TEXT_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final List<String> USER_INFO_WHITELIST_ADMIN =
			List.of("username", "avatar", "headimgurl", "nickname", "user_id", "mobile");

	private static final List<String> USER_INFO_WHITELIST_H5 =
			List.of("username", "avatar", "headimgurl", "nickname", "user_id");

	private final TopicMapper topicMapper;
	private final TopicOutsideLangReadService topicOutsideLangReadService;
	private final MemberAccountService memberAccountService;

	public TopicDetailService(
			TopicMapper topicMapper,
			TopicOutsideLangReadService topicOutsideLangReadService,
			MemberAccountService memberAccountService) {
		this.topicMapper = topicMapper;
		this.topicOutsideLangReadService = topicOutsideLangReadService;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> buildResponse(
			String topicIdRaw, Map<String, Object> operatorJwt, String requestLangTag) {
		long companyId = readLong(operatorJwt.get("company_id"), 1L);
		Long topicId = parseOptionalLong(topicIdRaw);
		if (topicId == null) {
			return sortedOuter(null);
		}
		LambdaQueryWrapper<Topic> w = new LambdaQueryWrapper<>();
		w.eq(Topic::getCompanyId, companyId).eq(Topic::getTopicId, topicId).last("LIMIT 1");
		Topic row = topicMapper.selectOne(w);
		if (row == null) {
			return sortedOuter(null);
		}
		LinkedHashMap<String, Object> rowMap = topicToRowMap(row);
		topicOutsideLangReadService.applyToRowMap(companyId, requestLangTag, rowMap);
		if (hasTopicIdForFormat(rowMap)) {
			formatDetail(rowMap, true, USER_INFO_WHITELIST_ADMIN, false);
		}
		return sortedOuter(ksortCopy(rowMap));
	}

	public LinkedHashMap<String, Object> buildH5TopicDetailRowMap(long companyId, String topicIdRaw, String langTag) {
		Long topicId = parseOptionalLong(topicIdRaw);
		if (topicId == null) {
			return null;
		}
		LambdaQueryWrapper<Topic> w = new LambdaQueryWrapper<>();
		w.eq(Topic::getCompanyId, companyId).eq(Topic::getTopicId, topicId).last("LIMIT 1");
		Topic row = topicMapper.selectOne(w);
		if (row == null) {
			return null;
		}
		LinkedHashMap<String, Object> rowMap = topicToRowMap(row);
		topicOutsideLangReadService.applyToRowMap(companyId, langTag, rowMap);
		if (hasTopicIdForFormat(rowMap)) {
			formatDetail(rowMap, true, USER_INFO_WHITELIST_H5, true);
		}
		return ksortCopy(rowMap);
	}

	public LinkedHashMap<String, Object> toAdminListRowMap(Topic entity, String langTag) {
		LinkedHashMap<String, Object> rowMap = topicToRowMap(entity);
		long companyId = readLong(entity.getCompanyId(), 1L);
		topicOutsideLangReadService.applyToRowMap(companyId, langTag, rowMap);
		if (hasTopicIdForFormat(rowMap)) {
			formatDetail(rowMap, false, USER_INFO_WHITELIST_ADMIN, false);
		}
		return ksortCopy(rowMap);
	}

	public LinkedHashMap<String, Object> toH5TopicListRowMap(Topic entity, long tenantCompanyId, String langTag) {
		LinkedHashMap<String, Object> rowMap = new LinkedHashMap<>();
		rowMap.put("topic_id", entity.getTopicId());
		rowMap.put("topic_name", entity.getTopicName());
		rowMap.put("user_id", entity.getUserId() != null ? entity.getUserId().intValue() : 0);
		rowMap.put("p_order", entity.getPOrder());
		rowMap.put("created", entity.getCreated());
		rowMap.put("source", entity.getSource());
		rowMap.put("status", entity.getStatus());
		rowMap.put("manual_verify_time", entity.getManualVerifyTime());
		rowMap.put("company_id", tenantCompanyId);
		topicOutsideLangReadService.applyToRowMap(tenantCompanyId, langTag, rowMap);
		if (hasTopicIdForFormat(rowMap)) {
			formatDetail(rowMap, false, USER_INFO_WHITELIST_H5, true);
		}
		rowMap.remove("company_id");
		return ksortCopy(rowMap);
	}

	private static Map<String, Object> sortedOuter(Map<String, Object> detailOrNull) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("topic_info", detailOrNull);
		return ksortCopy(out);
	}

	private static LinkedHashMap<String, Object> ksortCopy(Map<String, Object> src) {
		TreeMap<String, Object> sorted = new TreeMap<>(src);
		return new LinkedHashMap<>(sorted);
	}

	private static boolean hasTopicIdForFormat(Map<String, Object> rowMap) {
		Object v = rowMap.get("topic_id");
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s);
	}

	private void formatDetail(
			Map<String, Object> rowMap,
			boolean stripMobileWhenNoUser,
			List<String> userInfoWhitelist,
			boolean h5TopicDetail) {
		Object created = rowMap.get("created");
		if (created != null) {
			long createdSec = toEpochSeconds(created);
			if (createdSec > 0) {
				String formatted = CREATED_TEXT_FMT.format(Instant.ofEpochSecond(createdSec));
				rowMap.put("created_text", formatted);
			}
		}

		Object manual = rowMap.get("manual_verify_time");
		long mv = 0L;
		if (manual instanceof Number n) {
			mv = n.longValue();
		} else if (manual != null) {
			mv = toEpochSeconds(manual);
		}
		if (mv > 0) {
			if (h5TopicDetail) {
				rowMap.put("manual_verify_time", String.valueOf(mv));
			} else {
				rowMap.put("manual_verify_time", CREATED_TEXT_FMT.format(Instant.ofEpochSecond(mv)));
			}
		}

		Object st = rowMap.get("status");
		int status = st instanceof Number n ? n.intValue() : parseIntLoose(st);
		rowMap.put("status_text", tagStatusText(status));

		Long uid = longOrNull(rowMap.get("user_id"));
		if (uid != null && uid != 0L) {
			long rowCompanyId = readLong(rowMap.get("company_id"), 1L);
			Map<String, Object> filter = Map.of("user_id", uid, "company_id", rowCompanyId);
			Map<String, Object> userInfo = memberAccountService.getWechatUserInfo(filter);
			if (userInfo == null) {
				userInfo = Collections.emptyMap();
			}
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(uid, rowCompanyId);
			if (memberInfo != null && !memberInfo.isEmpty()) {
				LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
				merged.putAll(memberInfo);
				merged.putAll(userInfo);
				TreeMap<String, Object> userInfoOut = new TreeMap<>();
				for (String k : userInfoWhitelist) {
					if (merged.containsKey(k)) {
						userInfoOut.put(k, merged.get(k));
					}
				}
				rowMap.put("userInfo", new LinkedHashMap<>(userInfoOut));
			} else {
				rowMap.put("userInfo", new LinkedHashMap<>(new TreeMap<>(userInfo)));
			}
		}
		if (stripMobileWhenNoUser && (uid == null || uid == 0L)) {
			rowMap.remove("mobile");
		}
	}

	private static String tagStatusText(int status) {
		return switch (status) {
			case 0 -> "待审核";
			case 1 -> "审核通过";
			case 2 -> "机器拒绝";
			case 3 -> "待人工审核";
			case 4 -> "人工拒绝";
			default -> "";
		};
	}

	private static int parseIntLoose(Object st) {
		if (st == null) {
			return -1;
		}
		if (st instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(st.toString().trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static long toEpochSeconds(Object created) {
		if (created instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(created.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long parseOptionalLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long readLong(Object v, long defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static LinkedHashMap<String, Object> topicToRowMap(Topic e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("topic_id", e.getTopicId());
		m.put("topic_name", e.getTopicName());
		m.put("mobile", e.getMobile());
		m.put("p_order", e.getPOrder());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("ai_verify_time", e.getAiVerifyTime());
		m.put("manual_verify_time", e.getManualVerifyTime());
		m.put("company_id", e.getCompanyId());
		m.put("enabled", e.getEnabled());
		m.put("status", e.getStatus());
		m.put("ai_refuse_reason", e.getAiRefuseReason());
		m.put("manual_refuse_reason", e.getManualRefuseReason());
		m.put("is_top", e.getIsTop());
		m.put("user_id", e.getUserId() != null ? e.getUserId().intValue() : 0);
		m.put("source", e.getSource());
		m.put("operator_id", e.getOperatorId());
		return m;
	}
}
