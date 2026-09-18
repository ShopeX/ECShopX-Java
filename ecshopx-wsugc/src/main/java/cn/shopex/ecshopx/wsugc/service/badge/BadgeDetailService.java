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

package cn.shopex.ecshopx.wsugc.service.badge;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Badge;
import cn.shopex.ecshopx.wsugc.mapper.BadgeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BadgeDetailService {

	private static final DateTimeFormatter CREATED_TEXT_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final BadgeMapper badgeMapper;
	private final BadgeOutsideLangReadService badgeOutsideLangReadService;
	private final MemberAccountService memberAccountService;

	public BadgeDetailService(
			BadgeMapper badgeMapper,
			BadgeOutsideLangReadService badgeOutsideLangReadService,
			MemberAccountService memberAccountService) {
		this.badgeMapper = badgeMapper;
		this.badgeOutsideLangReadService = badgeOutsideLangReadService;
		this.memberAccountService = memberAccountService;
	}

	public void applyOutsideLangAndFormatForListRow(Map<String, Object> rowMap, String requestLangTag) {
		if (rowMap == null || rowMap.isEmpty()) {
			return;
		}
		long companyId = readLong(rowMap.get("company_id"), 1L);
		badgeOutsideLangReadService.applyToRowMap(companyId, requestLangTag, rowMap);
		if (hasBadgeIdForFormat(rowMap)) {
			formatDetail(rowMap);
		}
	}

	public static LinkedHashMap<String, Object> badgeToListRowMap(Badge e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("badge_id", e.getBadgeId());
		m.put("badge_name", e.getBadgeName());
		m.put("badge_memo", e.getBadgeMemo());
		m.put("user_id", e.getUserId() != null ? e.getUserId().intValue() : 0);
		m.put("p_order", e.getPOrder());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("source", e.getSource());
		m.put("is_top", e.getIsTop());
		m.put("status", e.getStatus());
		m.put("company_id", e.getCompanyId());
		return m;
	}

	public static LinkedHashMap<String, Object> ksortRowMap(Map<String, Object> src) {
		TreeMap<String, Object> sorted = new TreeMap<>(src);
		return new LinkedHashMap<>(sorted);
	}

	public Map<String, Object> buildResponse(
			String badgeIdRaw, Map<String, Object> operatorJwt, String requestLangTag) {
		long companyId = readLong(operatorJwt.get("company_id"), 1L);
		Long badgeId = parseOptionalLong(badgeIdRaw);
		if (badgeId == null) {
			return sortedOuter(null);
		}
		LambdaQueryWrapper<Badge> w = new LambdaQueryWrapper<>();
		w.eq(Badge::getCompanyId, companyId).eq(Badge::getBadgeId, badgeId).last("LIMIT 1");
		Badge row = badgeMapper.selectOne(w);
		if (row == null) {
			return sortedOuter(null);
		}
		LinkedHashMap<String, Object> rowMap = badgeToRowMap(row);
		badgeOutsideLangReadService.applyToRowMap(companyId, requestLangTag, rowMap);
		if (hasBadgeIdForFormat(rowMap)) {
			formatDetail(rowMap);
		}
		return sortedOuter(ksortCopy(rowMap));
	}

	private static Map<String, Object> sortedOuter(Map<String, Object> detailOrNull) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("badge_info", detailOrNull);
		return ksortCopy(out);
	}

	private static LinkedHashMap<String, Object> ksortCopy(Map<String, Object> src) {
		TreeMap<String, Object> sorted = new TreeMap<>(src);
		return new LinkedHashMap<>(sorted);
	}

	private static boolean hasBadgeIdForFormat(Map<String, Object> rowMap) {
		Object v = rowMap.get("badge_id");
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s);
	}

	private void formatDetail(Map<String, Object> rowMap) {
		Object created = rowMap.get("created");
		if (created != null) {
			long sec = toEpochSeconds(created);
			if (sec > 0) {
				rowMap.put("created_text", CREATED_TEXT_FMT.format(Instant.ofEpochSecond(sec)));
			}
		}
		Object st = rowMap.get("status");
		int status = st instanceof Number n ? n.intValue() : parseIntLoose(st);
		rowMap.put("status_text", statusText(status));

		Long uid = longOrNull(rowMap.get("user_id"));
		long rowCompanyId = readLong(rowMap.get("company_id"), 1L);
		if (uid != null && uid != 0L) {
			Map<String, Object> filter = Map.of("user_id", uid, "company_id", rowCompanyId);
			Map<String, Object> userInfo = memberAccountService.getWechatUserInfo(filter);
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(uid, rowCompanyId);
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
			if (memberInfo != null && !memberInfo.isEmpty()) {
				merged.putAll(memberInfo);
			}
			merged.putAll(userInfo);
			Map<String, Object> userInfoOut = new TreeMap<>();
			for (String k : List.of("avatar", "headimgurl", "nickname", "user_id", "username")) {
				if (merged.containsKey(k)) {
					userInfoOut.put(k, merged.get(k));
				}
			}
			rowMap.put("userInfo", new LinkedHashMap<>(userInfoOut));
		}
	}

	private static String statusText(int status) {
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
			long v = n.longValue();
			return v;
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

	private static Long nullIfUnsetEpoch(Long ts) {
		if (ts == null || ts.longValue() == 0L) {
			return null;
		}
		return ts;
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

	private static LinkedHashMap<String, Object> badgeToRowMap(Badge e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("badge_id", e.getBadgeId());
		m.put("badge_name", e.getBadgeName());
		m.put("badge_memo", e.getBadgeMemo());
		m.put("created", e.getCreated());
		m.put("p_order", e.getPOrder());
		m.put("is_top", e.getIsTop());
		m.put("user_id", e.getUserId() != null ? e.getUserId().intValue() : 0);
		m.put("company_id", e.getCompanyId());
		m.put("enabled", e.getEnabled());
		m.put("status", e.getStatus());
		m.put("operator_id", e.getOperatorId());
		m.put("source", e.getSource());
		m.put("updated", e.getUpdated());
		m.put("ai_verify_time", nullIfUnsetEpoch(e.getAiVerifyTime()));
		m.put("manual_verify_time", nullIfUnsetEpoch(e.getManualVerifyTime()));
		m.put("manual_refuse_reason", e.getManualRefuseReason());
		m.put("ai_refuse_reason", e.getAiRefuseReason());
		m.put("mobile", e.getMobile());
		return m;
	}
}
